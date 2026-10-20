package de.uzl.its.swat.symbolic.array

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * L2 (forked agent): an access to an object array at a symbolic index branches on which element
 * the index selects (a binary search over the array's length), so the explorer can reach every
 * element; only an array too large to search records precision loss. Runs via the
 * {@code agentTest} Gradle task (needs the agent jar from {@code copyJar}).
 */
class ObjectArrayIndexAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGETS = Path.of("src", "test", "resources", "targets").toAbsolutePath()

    @Shared Path classes
    @Shared List<Map> choices
    @Shared Map trace
    @Shared Map largeTrace

    def setupSpec() {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        classes = Files.createTempDirectory("swat-object-array-spec")
        compile(classes)
        trace = parseTrace(run(classes, ["-Dswat.input.I_0=7"], []))
        largeTrace = parseTrace(run(classes, ["-Dswat.input.I_0=7"], ["large"]))
        choices = trace.trace.findAll { it.choice }
    }

    def cleanupSpec() {
        classes?.toFile()?.deleteDir()
    }

    def "a load and a store at a symbolic index each search the ten elements in three steps"() {
        // [0,10): 7 < 5 no; [5,10): 7 < 7 no; [7,10): 7 < 8 yes. The array of length one needs none.
        expect:
        choices.size() == 6
        choices*.branched == [false, false, true] * 2
        choices*.key.collect { it.substring(it.indexOf('#')) } == ["#idx0", "#idx1", "#idx2"] * 2
        choices.every { it.constraint.contains("I_0") }
    }

    def "the searched accesses record no precision loss"() {
        expect:
        !trace.symbolicPrecisionLoss
    }

    def "an array too large to search records precision loss instead"() {
        expect:
        largeTrace.symbolicPrecisionLoss
        largeTrace.trace.count { it.choice } == 6
    }

    private static void compile(Path outDir) {
        def sources = [TARGETS.resolve("objectarray/Main.java"),
                       TARGETS.resolve("svcomp/org/sosy_lab/sv_benchmarks/Verifier.java")]*.toString()
        def args = ["-source", "1.8", "-target", "1.8", "-g", "-d", outDir.toString()] + sources
        def err = new ByteArrayOutputStream()
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err, args as String[])
        assert rc == 0: "javac failed: ${err}"
    }

    private static String run(Path classes, List<String> inputs, List<String> args) {
        def java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        def cmd = [java,
                   "-javaagent:${AGENT_JAR}".toString(),
                   "-Djava.library.path=${NATIVE_LIBS}".toString(),
                   "-Dconfig.path=/nonexistent/swat.cfg",
                   "-Dinstrumentation.transformer=SV_COMP",
                   "-Dsolver.mode=PRINT",
                   "-DexitOnError=false",
                   "-DlogShadowStateToConsole=false"] + inputs +
                  ["-cp", classes.toString(), "-ea", "Main"] + args
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
