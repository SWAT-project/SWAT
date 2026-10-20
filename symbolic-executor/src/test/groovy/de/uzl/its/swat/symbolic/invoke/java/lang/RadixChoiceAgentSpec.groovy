package de.uzl.its.swat.symbolic.invoke.java.lang

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * L2 (forked agent): a conversion with a symbolic radix branches on the radix (a binary search over
 * 2 to 36 after the JDK's fallback to 10), so that the conversion is exact with a constant radix
 * and the explorer reaches the other radixes by flipping the branches. Runs via the {@code
 * agentTest} Gradle task (needs the agent jar from {@code copyJar}).
 */
class RadixChoiceAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGETS = Path.of("src", "test", "resources", "targets").toAbsolutePath()

    def "radix #radix is searched in #steps.size() steps, as its concrete value says"() {
        given:
        def classes = Files.createTempDirectory("swat-radix-spec")
        compile(classes)

        when:
        def trace = parseTrace(run(classes, ["-Dswat.input.I_0=-1295", "-Dswat.input.I_1=" + radix]))
        def choices = trace.trace.findAll { it.choice }

        then: "binary search over [2, 37): the branch radix < mid of each step"
        choices*.branched == steps
        choices*.key.collect { it.substring(it.indexOf('#')) } == (0..<steps.size()).collect { "#radix" + it }
        choices.every { it.constraint.contains("I_1") }

        and: "the conversion has no context loss, and the equality depends on the number"
        trace.symbolicContextLoss == false
        trace.trace.any { !it.choice && it.constraint?.contains("I_0") }

        cleanup:
        classes?.toFile()?.deleteDir()

        where:
        // [2,37): mid 19, then 10 or 28, ...; radix 0 is 10 for the JDK
        radix | steps
        36    | [false, false, false, false, false, false]
        0     | [true, false, true, true, true]
        2     | [true, true, true, true, true]
    }

    private static void compile(Path outDir) {
        def sources = [TARGETS.resolve("radix/Main.java"),
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
