package de.uzl.its.swat.instrument

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * L2 (forked agent): how the SV-COMP instrumentation starts and ends the symbolic execution of an
 * entry method. Each target under {@code targets/entry/<name>} is compiled with the SV-COMP
 * Verifier stub and run with the agent in {@code solver.mode=PRINT}; the TraceDTO printed to
 * stdout is checked. Runs via the {@code agentTest} Gradle task (needs the agent jar from
 * {@code copyJar}).
 */
class EntryAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGETS = Path.of("src", "test", "resources", "targets").toAbsolutePath()

    def "an input read by the entry class's static initializer is tracked"() {
        when:
        def output = run("clinit", ["-Dswat.input.I_0=7"])
        def trace = parseTrace(output)

        then:
        !output.contains("NoThreadContextException")
        trace.inputs*.name == ["I_0"]
        !trace.trace.isEmpty()
        trace.violation?.throwable == "java.lang.AssertionError"
    }

    def "a main called from the entry method continues the running execution"() {
        when:
        def output = run("nested", ["-Dswat.input.I_0=2", "-Dswat.input.I_1=2"])
        def trace = parseTrace(output)

        then:
        !output.contains("DuplicateThreadContextException")
        traceCount(output) == 1
        trace.inputs*.name.toSet() == ["I_0", "I_1"].toSet()
        trace.violation?.throwable == "java.lang.AssertionError"
    }

    def "an exception leaving a super constructor does not break the invocation stack"() {
        when:
        def output = run("initexception", ["-Dswat.input.I_0=3"])
        def trace = parseTrace(output)

        then:
        !output.contains("Invocation stack is broken")
        trace.inputs*.name == ["I_0"]
        trace.violation?.throwable == "java.lang.AssertionError"
    }

    private static String run(String target, List<String> inputs) {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        Path classes = Files.createTempDirectory("swat-entry-agent-spec")
        try {
            compile([TARGETS.resolve("entry").resolve(target), TARGETS.resolve("svcomp").resolve("org")], classes)
            def java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
            def cmd = [java,
                       "-javaagent:${AGENT_JAR}".toString(),
                       "-Djava.library.path=${NATIVE_LIBS}".toString(),
                       "-Dconfig.path=/nonexistent/swat.cfg",
                       "-Dinstrumentation.transformer=SV_COMP",
                       "-Dsolver.mode=PRINT",
                       "-DexitOnError=false",
                       "-DlogShadowStateToConsole=false"] + inputs +
                      ["-cp", classes.toString(), "-ea", "Main"]
            def proc = new ProcessBuilder(cmd).directory(classes.toFile()).redirectErrorStream(true).start()
            def out = proc.inputStream.text
            assert proc.waitFor(5, TimeUnit.MINUTES): "target did not finish:\n${out}"
            return out
        } finally {
            classes.toFile().deleteDir()
        }
    }

    private static void compile(List<Path> sourceDirs, Path outDir) {
        def sources = sourceDirs.collectMany { dir ->
            Files.walk(dir).filter { it.toString().endsWith(".java") }.collect { it.toString() }
        }
        def args = ["-source", "1.8", "-target", "1.8", "-g", "-d", outDir.toString()] + sources
        def err = new ByteArrayOutputStream()
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err, args as String[])
        assert rc == 0: "javac failed: ${err}"
    }

    private static int traceCount(String out) {
        return out.split("\n").count { it == "{" }
    }

    /** The TraceDTO is the single pretty-printed JSON object whose braces start at column 0. */
    private static Map parseTrace(String out) {
        int start = out.indexOf("\n{\n")
        int end = out.indexOf("\n}\n", start)
        assert start >= 0 && end > start: "no trace JSON in output:\n${out}"
        return new ObjectMapper().readValue(out.substring(start + 1, end + 2), Map)
    }
}
