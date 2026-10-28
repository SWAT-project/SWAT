package de.uzl.its.swat.symbolic

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * L2 (forked agent): a StackOverflowError in the target behaves as without SWAT. The target
 * recurses as deep as its first input, on a small stack so that the overflow comes quickly; the
 * second input selects whether main catches the overflow. Run in {@code solver.mode=PRINT}, the
 * target must produce its own output, and the trace must be sent and marked as context loss and as
 * stack overflow. Runs via the {@code agentTest} Gradle task (needs the agent jar from
 * {@code copyJar}).
 */
class StackOverflowAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGET_SOURCES = Path.of("src", "test", "resources", "targets", "stackoverflow").toAbsolutePath()
    static final String DEEP = "1000000000"

    @Shared Path classes

    def setupSpec() {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        classes = Files.createTempDirectory("swat-stackoverflow-agent-spec")
        compile(classes)
    }

    def cleanupSpec() {
        classes?.toFile()?.deleteDir()
    }

    def "a shallow recursion is tracked as usual"() {
        when:
        def out = run(classes, "100", "0")
        def trace = parseTrace(out)

        then:
        out.contains("result 100")
        !trace.stackOverflow
        !trace.symbolicContextLoss
        trace.violation == null
    }

    def "an overflow caught by the target is handled by the target and recorded in the trace"() {
        when:
        def out = run(classes, DEEP, "0")
        def trace = parseTrace(out)

        then: "the target's handler ran, and SWAT neither crashed nor halted"
        out.contains("result -1")
        !out.contains("[SWAT Exception]")
        !out.contains("NoClassDefFoundError")

        and: "the trace up to the overflow is sent, and cannot lead to SAFE or to a violation"
        trace.stackOverflow
        trace.symbolicContextLoss
        trace.trace.size() > 0
    }

    def "an uncaught overflow ends the target as without SWAT"() {
        when:
        def out = run(classes, DEEP, "1")
        def trace = parseTrace(out)

        then:
        !out.contains("result")
        !out.contains("[SWAT Exception]")
        !out.contains("[SWAT] Uncaught top-level RuntimeException")
        trace.violation.throwable == "java.lang.StackOverflowError"
        trace.stackOverflow
        trace.symbolicContextLoss
    }

    private static void compile(Path outDir) {
        def sources = Files.walk(TARGET_SOURCES).filter { it.toString().endsWith(".java") }.collect { it.toString() }
        def args = ["-source", "1.8", "-target", "1.8", "-g", "-d", outDir.toString()] + sources
        def err = new ByteArrayOutputStream()
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err, args as String[])
        assert rc == 0: "javac failed: ${err}"
    }

    private static String run(Path classes, String n, String uncaught) {
        def java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        def cmd = [java,
                   "-Xss256k",
                   "-javaagent:${AGENT_JAR}".toString(),
                   "-Djava.library.path=${NATIVE_LIBS}".toString(),
                   "-Dconfig.path=/nonexistent/swat.cfg",
                   "-Dinstrumentation.transformer=SV_COMP",
                   "-Dsolver.mode=PRINT",
                   "-DexitOnError=true",
                   "-DlogShadowStateToConsole=false",
                   "-Dswat.input.I_0=${n}".toString(),
                   "-Dswat.input.I_1=${uncaught}".toString(),
                   "-cp", classes.toString(), "-ea", "Main"]
        def proc = new ProcessBuilder(cmd).directory(classes.toFile()).redirectErrorStream(true).start()
        def out = proc.inputStream.text
        assert proc.waitFor(5, TimeUnit.MINUTES): "target did not finish:\n${out}"
        return out
    }

    /** The TraceDTO is the single pretty-printed JSON object whose braces start at column 0. */
    private static Map parseTrace(String out) {
        int start = out.indexOf("\n{\n")
        int end = out.indexOf("\n}\n", start)
        assert start >= 0 && end > start: "no trace JSON in output:\n${out}"
        return new ObjectMapper().readValue(out.substring(start + 1, end + 2), Map)
    }
}
