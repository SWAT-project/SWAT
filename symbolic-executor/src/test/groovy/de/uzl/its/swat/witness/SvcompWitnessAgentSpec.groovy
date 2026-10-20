package de.uzl.its.swat.witness

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * L2 (forked agent): the SV-COMP instrumentation records the witness data in the trace. A tiny
 * SV-COMP style target is compiled and run with the agent in {@code solver.mode=PRINT}; the
 * {@code TraceDTO} JSON printed to stdout must carry the nondet returns with their call locations
 * and the violation with its frames. Runs via the {@code agentTest} Gradle task (needs the agent
 * jar from {@code copyJar}).
 */
class SvcompWitnessAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGET_SOURCES = Path.of("src", "test", "resources", "targets", "svcomp").toAbsolutePath()

    @Shared Path classes
    @Shared Map trace
    @Shared String output

    def setupSpec() {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        classes = Files.createTempDirectory("swat-witness-agent-spec")
        compile(classes)
        output = run(classes, ["-Dswat.input.I_0=3", "-Dswat.input.I_1=4"])
        trace = parseTrace(output)
    }

    def cleanupSpec() {
        classes?.toFile()?.deleteDir()
    }

    def "the trace carries every nondet return with its call location"() {
        expect:
        trace.nondetReturns.size() == 4
        trace.nondetReturns*.function == ["nondetInt", "nondetInt", "nondetDouble", "nondetString"]
        trace.nondetReturns*.type == ["int", "int", "double", "String"]
        trace.nondetReturns*.value == ["3", "4", "0.0", "fixed"]
        trace.nondetReturns*.className == ["Main"] * 4
        trace.nondetReturns*.method == ["main([Ljava/lang/String;)V"] * 4
        trace.nondetReturns*.sourceFile == ["Main.java"] * 4
        trace.nondetReturns*.line == [14, 14, 15, 16]
        trace.nondetReturns*.callIndex == [0, 1, 0, 0]
        trace.nondetReturns[2].bits == "0x0"
        trace.nondetReturns[0].bits == null
    }

    def "the trace carries the violation with the frames of the task innermost first"() {
        expect:
        trace.violation != null
        trace.violation.throwable == "java.lang.AssertionError"
        trace.violation.frames*.className == ["Main", "Main"]
        trace.violation.frames*.sourceFile == ["Main.java", "Main.java"]
        trace.violation.frames*.line == [9, 18]
    }

    def "the inputs of the trace still correspond to the nondet returns"() {
        expect:
        trace.inputs.size() == 4
        trace.inputs*.name.findAll { it.startsWith("I_") }.size() == 2
    }

    def "nothing is printed for the witness besides the trace"() {
        expect:
        !output.contains("[WITNESS]")
        !output.contains("[WITNESS_TARGET]")
    }

    private static void compile(Path outDir) {
        def sources = Files.walk(TARGET_SOURCES).filter { it.toString().endsWith(".java") }.collect { it.toString() }
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
