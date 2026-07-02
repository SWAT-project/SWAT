package de.uzl.its.swat.testsupport.agent

import com.fasterxml.jackson.databind.ObjectMapper

import javax.tools.ToolProvider
import java.nio.file.Files

/**
 * Level-2 harness. Compiles a tiny {@code @Symbolic}-annotated target against the agent jar, runs it
 * under the REAL SWAT agent in a forked JVM with {@code solver.mode=PRINT}, captures stdout, and
 * parses the emitted {@code TraceDTO} JSON into a {@link TraceObservation}. No Python explorer.
 *
 * Every failure mode throws (missing jar, compile error, non-zero exit, no JSON), so an infra break
 * surfaces as a real failure — never masked. See docs/test-architecture.md (Level L2).
 */
class AgentRun {

    /**
     * @param targetResource classpath resource of the target source, e.g. "targets/ToLowerCaseTarget.java".
     * @param mainClass the target's (default-package) main class name, e.g. "ToLowerCaseTarget".
     */
    static TraceObservation run(String targetResource, String mainClass) {
        // Gradle runs tests with the module dir as the working dir.
        File moduleDir = new File(System.getProperty("user.dir"))
        File agentJar = new File(moduleDir, "lib/symbolic-executor.jar")
        File libs = new File(moduleDir, "../libs/java-library-path")
        assert agentJar.exists():
                "Agent jar missing at ${agentJar} — run `./gradlew :symbolic-executor:copyJar` first " +
                "(the agentTest task does this automatically)."

        // 1. Materialize + compile the target against the agent jar (which provides @Symbolic).
        File work = Files.createTempDirectory("swat-l2-").toFile()
        File outDir = new File(work, "classes")
        outDir.mkdirs()
        File src = new File(work, mainClass + ".java")
        def res = AgentRun.class.classLoader.getResourceAsStream(targetResource)
        assert res != null: "Target resource not found on test classpath: ${targetResource}"
        src.text = res.text

        def compiler = ToolProvider.getSystemJavaCompiler()
        assert compiler != null: "No system Java compiler — tests must run on a JDK, not a JRE."
        int rc = compiler.run(null, System.out, System.err,
                "-g", "-cp", agentJar.absolutePath, "-d", outDir.absolutePath, src.absolutePath)
        assert rc == 0: "Failed to compile target ${targetResource}"

        // 2. Fork a JVM under the agent in PRINT mode (the TraceDTO JSON goes to stdout).
        List<String> cmd = [
                javaBin(),
                "-Djava.library.path=${libs.absolutePath}".toString(),
                "-Dsolver.mode=PRINT",
                "-Dlogging.debug=false",
                "-javaagent:${agentJar.absolutePath}".toString(),
                "-cp", outDir.absolutePath,
                mainClass
        ]
        Process proc = new ProcessBuilder(cmd).start()
        String stdout = proc.inputStream.text
        String stderr = proc.errorStream.text
        int exit = proc.waitFor()
        assert exit == 0:
                "Agent run exited ${exit}.\n--- STDOUT tail ---\n${tail(stdout)}\n--- STDERR tail ---\n${tail(stderr)}"

        // 3. Extract the single top-level pretty-printed JSON object and parse it.
        String json = extractTraceJson(stdout)
        assert json != null: "No TraceDTO JSON found on stdout.\n--- STDOUT tail ---\n${tail(stdout)}"
        return TraceObservation.parse(new ObjectMapper().readTree(json))
    }

    private static String javaBin() {
        return new File(System.getProperty("java.home"), "bin/java").absolutePath
    }

    /**
     * The PRINT-mode TraceDTO is the only top-level (column-0) pretty-printed JSON object on stdout,
     * so it spans from the single line that is "{" to the single line that is "}".
     */
    private static String extractTraceJson(String stdout) {
        List<String> lines = stdout.readLines()
        int start = lines.findIndexOf { it.trim() == "{" }
        int end = lines.findLastIndexOf { it.trim() == "}" }
        if (start < 0 || end < start) {
            return null
        }
        return lines[start..end].join("\n")
    }

    private static String tail(String s, int n = 40) {
        List<String> l = s.readLines()
        return l.subList(Math.max(0, l.size() - n), l.size()).join("\n")
    }
}
