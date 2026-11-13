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
 * L2 (forked agent): symbolic values that go through modelled JDK container calls stay symbolic
 * without context loss; calls that inspect a container holding a symbolic value still record it.
 * Runs via the {@code agentTest} Gradle task (needs the agent jar from {@code copyJar}).
 */
class JdkContainerAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGET = Path.of("src", "test", "resources", "targets", "collections").toAbsolutePath()
    static final Path VERIFIER = Path.of("src", "test", "resources", "targets", "svcomp", "org").toAbsolutePath()

    @Shared Path classes

    def setupSpec() {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        classes = Files.createTempDirectory("swat-container-agent-spec")
        def sources = ([TARGET, VERIFIER].collectMany { root ->
            Files.walk(root).filter { it.toString().endsWith(".java") }.collect { it.toString() }
        })
        def err = new ByteArrayOutputStream()
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err,
                (["-source", "1.8", "-target", "1.8", "-g", "-d", classes.toString()] + sources) as String[])
        assert rc == 0: "javac failed: ${err}"
    }

    def cleanupSpec() {
        classes?.toFile()?.deleteDir()
    }

    @Unroll
    def "a symbolic string kept in a container stays symbolic (#scenario)"() {
        when:
        Map trace = run(scenario)

        then:
        !trace.symbolicContextLoss
        trace.trace.any { !it.concreteOnly && it.constraint?.contains("String_${NondetInputs.ids(classes, "Main")[0]}".toString()) }

        where:
        scenario << ["list", "iterator", "map", "array", "arraycopy", "typedArray", "sharedEmpty"]
    }

    @Unroll
    def "a call that inspects a container holding a symbolic value records context loss (#scenario)"() {
        expect:
        run(scenario).symbolicContextLoss

        where:
        scenario << ["contains", "symbolicKey", "heldByUnmodelled", "lambdaCallback", "appendCallback"]
    }

    def "a lambda that library code calls back runs without an executor error"() {
        when:
        String out = runOutput("lambdaCallback")

        then:
        !out.contains("Addresses must match")
        !out.contains("SWAT Exception")
        out.contains("long")
    }

    def "a StringBuilder whose append calls application code back is read concretely afterwards"() {
        when:
        String out = runOutput("appendCallback")

        then:
        !out.contains("does not match the value in the stack")
        !out.contains("SWAT Exception")
        out.contains("appended")
    }

    private Map run(String scenario) {
        String out = runOutput(scenario)
        int start = out.indexOf("\n{\n")
        int end = out.indexOf("\n}\n", start)
        assert start >= 0 && end > start: "no trace JSON in output:\n${out}"
        return new ObjectMapper().readValue(out.substring(start + 1, end + 2), Map)
    }

    private String runOutput(String scenario) {
        def java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        def cmd = [java,
                   "-javaagent:${AGENT_JAR}".toString(),
                   "-Djava.library.path=${NATIVE_LIBS}".toString(),
                   "-Dconfig.path=/nonexistent/swat.cfg",
                   "-Dinstrumentation.transformer=SV_COMP",
                   "-Dsolver.mode=PRINT",
                   "-DexitOnError=false",
                   "-DlogShadowStateToConsole=false",
                   "-cp", classes.toString(), "-ea", "Main", scenario]
        def proc = new ProcessBuilder(cmd).directory(classes.toFile()).redirectErrorStream(true).start()
        def out = proc.inputStream.text
        assert proc.waitFor(5, TimeUnit.MINUTES): "target did not finish:\n${out}"
        return out
    }
}
