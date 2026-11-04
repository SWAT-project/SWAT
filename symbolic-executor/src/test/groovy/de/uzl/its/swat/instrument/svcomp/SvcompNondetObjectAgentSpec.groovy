package de.uzl.its.swat.instrument.svcomp

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * L2 (forked agent): {@code Verifier.nondetObject(type, factory)} is rewritten to a call of the
 * factory, so the factory's nondet choices are symbolic and the object it returns is tracked. A
 * factory that is not instrumented makes concrete choices, which must show up as a context loss.
 * Runs via the {@code agentTest} Gradle task (needs the agent jar from {@code copyJar}).
 */
class SvcompNondetObjectAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGET_SOURCES = Path.of("src", "test", "resources", "targets", "svcomp-objects").toAbsolutePath()
    static final Path SV_COMP_CONFIG = Path.of("..", "targets", "sv-comp", "sv-comp.cfg").toAbsolutePath()

    @Shared Path classes
    /** The exclusions of the competition config: the benchmark's org/sosy_lab classes stay uninstrumented. */
    @Shared String excludePackages

    def setupSpec() {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        classes = Files.createTempDirectory("swat-nondet-object-agent-spec")
        compile(classes)
        excludePackages = Files.readAllLines(SV_COMP_CONFIG)
                .find { it.startsWith("instrumentation.excludePackages=") }
                .substring("instrumentation.excludePackages=".length())
    }

    def cleanupSpec() {
        classes?.toFile()?.deleteDir()
    }

    def "the choices of an instrumented factory are symbolic and reach the violation"() {
        when:
        def output = run("Main", ["-Dswat.input.I_0=1", "-Dswat.input.I_1=42"])
        def trace = parseTrace(output)

        then:
        !output.contains("Error while instrumenting")
        trace.inputs*.name.findAll { it.startsWith("I_") }.size() == 2
        trace.trace.findAll { it.constraint?.contains("I_0") }.size() > 0
        trace.trace.findAll { it.constraint?.contains("I_1") }.size() > 0
        !trace.symbolicContextLoss
        trace.violation?.throwable == "java.lang.AssertionError"
        trace.nondetReturns*.function == ["nondetInt", "nondetInt"]
        trace.nondetReturns*.className == ["objects.BoxFactory"] * 2
        trace.nondetReturns*.value == ["1", "42"]
    }

    def "the null object is a path of its own"() {
        when:
        def trace = parseTrace(run("Main", ["-Dswat.input.I_0=0"]))

        then:
        trace.violation == null
        !trace.symbolicContextLoss
        trace.trace.findAll { it.constraint?.contains("I_0") }.size() > 0
    }

    def "a factory that is not instrumented causes context loss"() {
        when:
        def trace = parseTrace(run("PlainMain", []))

        then:
        trace.symbolicContextLoss
        trace.missingInvocations.any { it.name == "createObject" && it.contextLoss }
    }

    private static void compile(Path outDir) {
        def sources = Files.walk(TARGET_SOURCES).filter { it.toString().endsWith(".java") }.collect { it.toString() }
        def args = ["-source", "1.8", "-target", "1.8", "-g", "-d", outDir.toString()] + sources
        def err = new ByteArrayOutputStream()
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err, args as String[])
        assert rc == 0: "javac failed: ${err}"
    }

    private String run(String mainClass, List<String> inputs) {
        def java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        def cmd = [java,
                   "-javaagent:${AGENT_JAR}".toString(),
                   "-Djava.library.path=${NATIVE_LIBS}".toString(),
                   "-Dconfig.path=/nonexistent/swat.cfg",
                   "-Dinstrumentation.excludePackages=${excludePackages}".toString(),
                   "-Dinstrumentation.transformer=SV_COMP",
                   "-Dsolver.mode=PRINT",
                   "-DexitOnError=false",
                   "-DlogShadowStateToConsole=false"] + inputs +
                  ["-cp", classes.toString(), "-ea", mainClass]
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
