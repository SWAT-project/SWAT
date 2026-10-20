package de.uzl.its.swat.symbolic

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * L2 (forked agent): the index check of String.charAt is a branch of the trace, on both sides. A
 * tiny SV-COMP style target calls charAt(5) on a nondet string and catches the exception; it is run
 * with the agent in {@code solver.mode=PRINT}, once with the default input "fixed" (too short: the
 * call throws) and once with a longer one. Runs via the {@code agentTest} Gradle task (needs the
 * agent jar from {@code copyJar}).
 */
class StringIndexCheckAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGETS = Path.of("src", "test", "resources", "targets").toAbsolutePath()

    @Shared Path classes
    @Shared Map shortTrace
    @Shared Map longTrace

    def setupSpec() {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        classes = Files.createTempDirectory("swat-string-index-agent-spec")
        compile(classes)
        shortTrace = parseTrace(run(classes, []))
        longTrace = parseTrace(run(classes, ["-Dswat.input.java/lang/String_0=abcdefgh"]))
    }

    def cleanupSpec() {
        classes?.toFile()?.deleteDir()
    }

    private static List<Map> indexChecks(Map trace) {
        return trace.trace.findAll { it.callback && it.constraint?.contains("str.len") }
    }

    def "a call that throws at the index check records the failed check, not a context loss"() {
        expect:
        indexChecks(shortTrace).size() == 1
        !indexChecks(shortTrace)[0].branched
        !shortTrace.symbolicContextLoss
    }

    def "a call that returns records the check as held"() {
        expect:
        indexChecks(longTrace).size() == 1
        indexChecks(longTrace)[0].branched
        !longTrace.symbolicContextLoss
    }

    private static void compile(Path outDir) {
        def sources = [TARGETS.resolve("svcomp-strings/Main.java"),
                       TARGETS.resolve("svcomp/org/sosy_lab/sv_benchmarks/Verifier.java")]*.toString()
        def args = ["-source", "1.8", "-target", "1.8", "-g", "-d", outDir.toString()] + sources
        def err = new ByteArrayOutputStream()
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err, args as String[])
        assert rc == 0: "javac failed: ${err}"
    }

    private static String run(Path classes, List<String> inputs) {
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
    }

    /** The TraceDTO is the single pretty-printed JSON object whose braces start at column 0. */
    private static Map parseTrace(String out) {
        int start = out.indexOf("\n{\n")
        int end = out.indexOf("\n}\n", start)
        assert start >= 0 && end > start: "no trace JSON in output:\n${out}"
        return new ObjectMapper().readValue(out.substring(start + 1, end + 2), Map)
    }
}
