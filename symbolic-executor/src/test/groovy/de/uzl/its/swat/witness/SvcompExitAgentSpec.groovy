package de.uzl.its.swat.witness

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * L2 (forked agent): a target that ends the JVM (System.exit, Runtime.halt) still sends its trace,
 * and an assertion that fails before the exit is reported where its AssertionError is thrown, even
 * when a {@code finally} exits before the error escapes main. Runs via the {@code agentTest} Gradle
 * task (needs the agent jar from {@code copyJar}).
 */
class SvcompExitAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGETS = Path.of("src", "test", "resources", "targets").toAbsolutePath()

    @Shared Path classes

    def setupSpec() {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        classes = Files.createTempDirectory("swat-exit-agent-spec")
        compile(classes)
    }

    def cleanupSpec() {
        classes?.toFile()?.deleteDir()
    }

    def "an assertion that fails before #exitCall in a finally is reported with its location"() {
        when:
        def (int rc, String out) = run(mainClass, 7)
        Map trace = parseTrace(out)

        then:
        rc == 3
        out.contains("java.lang.AssertionError")
        trace.violation != null
        trace.violation.throwable == "java.lang.AssertionError"
        trace.violation.frames*.className == [mainClass]
        trace.violation.frames*.line == [8]

        where:
        mainClass       | exitCall
        "ExitInFinally" | "System.exit"
        "HaltInFinally" | "Runtime.halt"
    }

    def "a run of #mainClass whose assertion holds reports no violation"() {
        when:
        def (int rc, String out) = run(mainClass, 3)
        Map trace = parseTrace(out)

        then:
        rc == 0
        !out.contains("java.lang.AssertionError")
        trace.violation == null

        where:
        mainClass << ["ExitInFinally", "HaltInFinally"]
    }

    def "System.exit before a failing assertion sends the trace and reports no violation"() {
        when:
        def (int rc, String out) = run("CleanExit", 7)
        Map trace = parseTrace(out)

        then:
        rc == 0
        !out.contains("java.lang.AssertionError")
        trace.violation == null
        trace.nondetReturns*.value == ["7"]
    }

    private static void compile(Path outDir) {
        def sources = [TARGETS.resolve("svcomp/org"), TARGETS.resolve("svcomp-exit")].collectMany { dir ->
            Files.walk(dir).filter { it.toString().endsWith(".java") }.collect { it.toString() }
        }
        def args = ["-source", "1.8", "-target", "1.8", "-g", "-d", outDir.toString()] + sources
        def err = new ByteArrayOutputStream()
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err, args as String[])
        assert rc == 0: "javac failed: ${err}"
    }

    private List run(String mainClass, int input) {
        def java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        def cmd = [java,
                   "-javaagent:${AGENT_JAR}".toString(),
                   "-Djava.library.path=${NATIVE_LIBS}".toString(),
                   "-Dconfig.path=/nonexistent/swat.cfg",
                   "-Dinstrumentation.transformer=SV_COMP",
                   "-Dsolver.mode=PRINT",
                   "-DexitOnError=false",
                   "-DlogShadowStateToConsole=false",
                   "-Dswat.input.I_0=${input}".toString(),
                   "-cp", classes.toString(), "-ea", mainClass]
        def proc = new ProcessBuilder(cmd).directory(classes.toFile()).redirectErrorStream(true).start()
        def out = proc.inputStream.text
        assert proc.waitFor(5, TimeUnit.MINUTES): "target did not finish:\n${out}"
        return [proc.exitValue(), out]
    }

    /** The TraceDTO is the single pretty-printed JSON object whose braces start at column 0. */
    private static Map parseTrace(String out) {
        int start = out.indexOf("\n{\n")
        int end = out.indexOf("\n}\n", start)
        assert start >= 0 && end > start: "no trace JSON in output:\n${out}"
        return new ObjectMapper().readValue(out.substring(start + 1, end + 2), Map)
    }
}
