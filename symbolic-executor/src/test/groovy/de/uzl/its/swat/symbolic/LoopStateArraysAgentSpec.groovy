package de.uzl.its.swat.symbolic

import com.fasterxml.jackson.databind.ObjectMapper
import de.uzl.its.swat.testsupport.NondetInputs
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * L2 (forked agent): loop-state matching with arrays on the heap. An allocated array's base symbol
 * is no input (Value.setAllocationBasesConcrete): an array that only tracked code wrote with
 * concrete values, such as a constant table, must not keep the state at a loop head from being
 * hashed, while an array that holds an input, was written at an input index, or was written by
 * untracked code must. The target (targets/loopstate-arrays) runs the same loop in each mode; the
 * spec counts the loop-head states in the trace. Runs via the {@code agentTest} Gradle task.
 */
class LoopStateArraysAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGET_SOURCES = Path.of("src", "test", "resources", "targets", "loopstate-arrays").toAbsolutePath()

    @Shared Path classes

    def setupSpec() {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        classes = Files.createTempDirectory("swat-loopstate-arrays-agent-spec")
        compile(classes)
    }

    def cleanupSpec() {
        classes?.toFile()?.deleteDir()
    }

    @Unroll
    def "mode #mode (#what): loop-head states #expect"() {
        when:
        def out = run(classes, mode)
        def trace = parseTrace(out)
        def states = trace.trace.collectMany { it.loopStates ?: [] }

        then:
        out.contains("phase ")
        !out.contains("[SWAT Exception]")
        hashed ? !states.isEmpty() : states.isEmpty()

        where:
        mode | what                          | hashed
        0    | "tracked concrete writes only" | true
        1    | "an input stored"              | false
        2    | "a store at an input index"    | false
        3    | "written by untracked code"    | false
        expect = hashed ? "are reported" : "are not reported"
    }

    private static void compile(Path outDir) {
        def sources = Files.walk(TARGET_SOURCES).filter { it.toString().endsWith(".java") }.collect { it.toString() }
        def args = ["-source", "1.8", "-target", "1.8", "-g", "-d", outDir.toString()] + sources
        def err = new ByteArrayOutputStream()
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err, args as String[])
        assert rc == 0: "javac failed: ${err}"
    }

    private static String run(Path classes, int mode) {
        def ids = NondetInputs.ids(classes, "Main")
        def java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        def cmd = [java,
                   "-javaagent:${AGENT_JAR}".toString(),
                   "-Djava.library.path=${NATIVE_LIBS}".toString(),
                   "-Dconfig.path=/nonexistent/swat.cfg",
                   "-Dinstrumentation.transformer=SV_COMP",
                   "-Dsolver.mode=PRINT",
                   "-DexitOnError=true",
                   "-DloopStateMatching=true",
                   "-DlogShadowStateToConsole=false",
                   "-Dswat.input.I_${ids[0]}=${mode}".toString(),
                   // two iterations, so that the head is reached three times
                   "-Dswat.input.Z_${ids[3]}=true".toString(),
                   "-Dswat.input.Z_${ids[3]}01=true".toString(),
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
