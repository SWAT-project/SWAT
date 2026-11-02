package de.uzl.its.swat.symbolic

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * L2 (forked agent): the branches of SWAT's helper for reference comparisons
 * (UtilInstrumented.refEquals) are flagged like callbacks, so the static pre-analysis walk holds
 * its place for them; the comparison's own branch is not, and keeps its key. How many branches the
 * helper takes depends on the operands' runtime types, so the graph cannot have them. Runs via the
 * {@code agentTest} Gradle task (needs the agent jar from {@code copyJar}).
 */
class RefEqualsHelperAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGETS = Path.of("src", "test", "resources", "targets").toAbsolutePath()
    static final Path SOURCE = TARGETS.resolve("svcomp-refequals/Main.java")

    @Shared Path classes
    @Shared Map trace
    @Shared String comparisonKey

    def setupSpec() {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        classes = Files.createTempDirectory("swat-refequals-agent-spec")
        compile(classes)
        trace = parseTrace(run(classes))
        int line = Files.readAllLines(SOURCE).findIndexOf { it.contains("if (a == b)") } + 1
        comparisonKey = "Main.main([Ljava/lang/String;)V|${line}|if|0".toString()
    }

    def cleanupSpec() {
        classes?.toFile()?.deleteDir()
    }

    private List<Map> branches() {
        return trace.trace.findAll { it.type != "Special" }
    }

    def "the helper's branches are flagged, the comparison's own branch is not"() {
        when:
        def all = branches()
        int comparison = all.findIndexOf { it.key == comparisonKey }
        def helper = all.subList(0, comparison).findAll { it.key == null }

        then:
        comparison >= 0
        !all[comparison].callback
        !helper.isEmpty()
        helper.every { it.callback }
    }

    def "the helper's frames are not reported as stack changes"() {
        expect:
        branches().every { b -> (b.pushes ?: []).every { it == null || !it.contains("refEquals") } }
    }

    private static void compile(Path outDir) {
        def sources = [SOURCE, TARGETS.resolve("svcomp/org/sosy_lab/sv_benchmarks/Verifier.java")]*.toString()
        def args = ["-source", "1.8", "-target", "1.8", "-g", "-d", outDir.toString()] + sources
        def err = new ByteArrayOutputStream()
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err, args as String[])
        assert rc == 0: "javac failed: ${err}"
    }

    private static String run(Path classes) {
        def java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        def cmd = [java,
                   "-javaagent:${AGENT_JAR}".toString(),
                   "-Djava.library.path=${NATIVE_LIBS}".toString(),
                   "-Dconfig.path=/nonexistent/swat.cfg",
                   "-Dinstrumentation.transformer=SV_COMP",
                   "-Dsolver.mode=PRINT",
                   "-DexitOnError=false",
                   "-DlogShadowStateToConsole=false",
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
