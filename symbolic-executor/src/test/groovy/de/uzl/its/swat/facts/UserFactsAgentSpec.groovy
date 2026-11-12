package de.uzl.its.swat.facts

import com.fasterxml.jackson.databind.ObjectMapper
import de.uzl.its.swat.testsupport.NondetInputs
import spock.lang.Shared
import spock.lang.Specification

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * L2 (forked agent): hand-written facts from the source (de.uzl.its.swat.annotations) are applied
 * only with -DuserFacts=true: @Irrelevant and Swat.beginIrrelevant/endIrrelevant flag branches as
 * merged with their fact, Swat.summary() returns true, and reaching a @Target is a violation.
 */
class UserFactsAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGETS = Path.of("src", "test", "resources", "targets").toAbsolutePath()
    static final Path ANNOTATIONS = Path.of("..", "annotations", "src", "main", "java").toAbsolutePath()

    @Shared Path classes
    @Shared String input

    def setupSpec() {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        classes = Files.createTempDirectory("swat-user-facts-spec")
        def sources = [TARGETS.resolve("userfacts/Main.java"),
                       TARGETS.resolve("svcomp/org/sosy_lab/sv_benchmarks/Verifier.java")]*.toString() +
                Files.walk(ANNOTATIONS).filter { it.toString().endsWith(".java") }.collect { it.toString() }
        def err = new ByteArrayOutputStream()
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err,
                (["-source", "1.8", "-target", "1.8", "-g", "-d", classes.toString()] + sources) as String[])
        assert rc == 0: "javac failed: ${err}"
        input = "I_${NondetInputs.ids(classes, "Main")[0]}"
    }

    def cleanupSpec() {
        classes?.toFile()?.deleteDir()
    }

    def "with user facts, irrelevant branches are merged and named, and the summary runs"() {
        when:
        def out = run(true, 5)
        def trace = parseTrace(out)
        def branches = trace.trace.findAll { it.type == "Branch" && it.constraint?.contains(input) }

        then:
        out.contains("summary 1")
        // noise's branch and the region's branch: merged, with their facts
        branches.findAll { it.userFact != null }*.userFact as Set ==
                ["@Irrelevant Main.noise(I)I", "Swat.beginIrrelevant Main.main([Ljava/lang/String;)V line 19"] as Set
        branches.findAll { it.userFact != null }.every { it.merged }
        // a == 42 is a decision
        branches.any { it.userFact == null && !it.merged }
        trace.userFacts as Set == ["@Irrelevant Main.noise(I)I",
                                   "Swat.beginIrrelevant Main.main([Ljava/lang/String;)V line 19",
                                   "Swat.summary Main.main([Ljava/lang/String;)V line 25 #0"] as Set
        trace.violation == null
    }

    def "with user facts, reaching a @Target is a violation"() {
        when:
        def trace = parseTrace(run(true, 42))

        then:
        trace.violation?.throwable == "java.lang.AssertionError"
    }

    def "without user facts, nothing changes"() {
        when:
        def out = run(false, 42)
        def trace = parseTrace(out)

        then:
        out.contains("summary 0")
        trace.violation == null
        trace.trace.every { it.userFact == null && !it.merged }
        !trace.userFacts
    }

    private String run(boolean facts, int a) {
        def java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        def cmd = [java,
                   "-javaagent:${AGENT_JAR}".toString(),
                   "-Djava.library.path=${NATIVE_LIBS}".toString(),
                   "-Dconfig.path=/nonexistent/swat.cfg",
                   "-Dinstrumentation.transformer=SV_COMP",
                   "-DloopSummaries=false",
                   "-DuserFacts=${facts}".toString(),
                   "-Dsolver.mode=PRINT",
                   "-DexitOnError=false",
                   "-DlogShadowStateToConsole=false",
                   "-Dswat.input.${input}=${a}".toString(),
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
