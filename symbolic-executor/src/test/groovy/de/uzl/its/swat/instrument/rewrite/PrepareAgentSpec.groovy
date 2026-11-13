package de.uzl.its.swat.instrument.rewrite

import com.fasterxml.jackson.databind.ObjectMapper
import de.uzl.its.swat.testsupport.NondetInputs
import spock.lang.Shared
import spock.lang.Specification

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * L2 (forked agent): classes prepared ahead of time ({@code de.uzl.its.swat.prepare.Prepare}) run
 * exactly like the original classes rewritten at load time, and the agent refuses classes prepared
 * with another configuration. Runs via the {@code agentTest} Gradle task.
 */
class PrepareAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGETS = Path.of("src", "test", "resources", "targets").toAbsolutePath()
    static final List<String> CONFIG = ["-Dconfig.path=/nonexistent/swat.cfg", "-Dinstrumentation.transformer=SV_COMP"]

    @Shared Path classes
    @Shared Path work
    @Shared List<String> inputs

    def setupSpec() {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        work = Files.createTempDirectory("swat-prepare-agent-spec")
        classes = work.resolve("classes")
        compile(classes)
        Files.writeString(classes.resolve("marker.txt"), "a resource")
        def ids = NondetInputs.ids(classes, "Main")
        inputs = ["-Dswat.input.I_${ids[0]}=5".toString(), "-Dswat.input.Z_${ids[1]}=true".toString()]
    }

    def cleanupSpec() {
        work?.toFile()?.deleteDir()
    }

    def "a prepared class is marked and an unchanged one is copied"() {
        when:
        Map result = prepare(work.resolve("p1"), [])

        then:
        result.classpath == [work.resolve("p1").resolve("0").toString()]
        // Main, and the test's Verifier (excluded only by sv-comp.cfg, not by the defaults)
        result.prepared == 2
        result.failed == []
        def bytes = Files.readAllBytes(work.resolve("p1/0/Main.class"))
        PreparedMarker.read(bytes) == new PreparedMarker.Marker(Rewrites.FORMAT_VERSION, result.config)
        PreparedMarker.read(Files.readAllBytes(classes.resolve("Main.class"))) == null
        // Not a class: copied
        Files.readAllBytes(work.resolve("p1/0/marker.txt")) == Files.readAllBytes(classes.resolve("marker.txt"))
    }

    def "a prepared classpath gives the same trace as the original"() {
        given:
        Map result = prepare(work.resolve("p2"), [])

        when:
        Map original = parseTrace(run(classes.toString(), inputs))
        Map prepared = parseTrace(run(result.classpath[0], inputs))

        then:
        original.violation?.throwable == "java.lang.AssertionError"
        prepared.violation == original.violation
        prepared.inputs*.name == original.inputs*.name
        keys(prepared) == keys(original)
        prepared.trace*.constraint == original.trace*.constraint
        prepared.nondetReturns == original.nondetReturns
    }

    def "a class prepared with another configuration is refused"() {
        given:
        Map result = prepare(work.resolve("p3"), ["-DuseStringInterning=false"])

        when:
        String out = run(result.classpath[0], inputs)

        then:
        out.contains("was prepared with format")
        out.contains("refusing to run it")
    }

    private static List keys(Map trace) {
        return trace.trace.findAll { it.key != null }*.key
    }

    private Map prepare(Path out, List<String> extra) {
        def java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        def cmd = [java] + CONFIG + extra + ["-cp", AGENT_JAR.toString(), "de.uzl.its.swat.prepare.Prepare",
                                             out.toString(), classes.toString()]
        def proc = new ProcessBuilder(cmd).redirectErrorStream(false).start()
        def stdout = proc.inputStream.text
        assert proc.waitFor(2, TimeUnit.MINUTES) && proc.exitValue() == 0: "Prepare failed:\n${proc.errorStream.text}"
        return new ObjectMapper().readValue(stdout.readLines().last(), Map)
    }

    private static void compile(Path outDir) {
        def sources = [TARGETS.resolve("prepare/Main.java"),
                       TARGETS.resolve("svcomp/org/sosy_lab/sv_benchmarks/Verifier.java")]*.toString()
        def args = ["-source", "1.8", "-target", "1.8", "-g", "-d", outDir.toString()] + sources
        def err = new ByteArrayOutputStream()
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err, args as String[])
        assert rc == 0: "javac failed: ${err}"
    }

    private static String run(String cp, List<String> inputs) {
        def java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        def cmd = [java,
                   "-javaagent:${AGENT_JAR}".toString(),
                   "-Djava.library.path=${NATIVE_LIBS}".toString()] + CONFIG +
                  ["-Dsolver.mode=PRINT",
                   "-DexitOnError=false",
                   "-DlogShadowStateToConsole=false"] + inputs +
                  ["-cp", cp, "-ea", "Main"]
        def proc = new ProcessBuilder(cmd).directory(Path.of(cp).toFile()).redirectErrorStream(true).start()
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
