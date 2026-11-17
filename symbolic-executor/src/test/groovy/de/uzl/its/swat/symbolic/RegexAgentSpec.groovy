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
 * L2 (forked agent): Pattern.compile(c).matcher(s).matches() and Pattern.matches(c, s) on a
 * symbolic string are modelled as String.matches (a regex membership, no context loss); find,
 * reset, flags and regular expressions JavaRegex does not translate still record context loss.
 * Runs via the {@code agentTest} Gradle task (needs the agent jar from {@code copyJar}).
 */
class RegexAgentSpec extends Specification {

    static final Path AGENT_JAR = Path.of("lib", "symbolic-executor.jar").toAbsolutePath()
    static final Path NATIVE_LIBS = Path.of("..", "libs", "java-library-path").toAbsolutePath()
    static final Path TARGET = Path.of("src", "test", "resources", "targets", "regex").toAbsolutePath()
    static final Path VERIFIER = Path.of("src", "test", "resources", "targets", "svcomp", "org").toAbsolutePath()

    @Shared Path classes
    @Shared String input

    def setupSpec() {
        assert Files.isRegularFile(AGENT_JAR): "agent jar missing, run copyJar first: ${AGENT_JAR}"
        classes = Files.createTempDirectory("swat-regex-agent-spec")
        def sources = ([TARGET, VERIFIER].collectMany { root ->
            Files.walk(root).filter { it.toString().endsWith(".java") }.collect { it.toString() }
        })
        def err = new ByteArrayOutputStream()
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err,
                (["-source", "1.8", "-target", "1.8", "-g", "-d", classes.toString()] + sources) as String[])
        assert rc == 0: "javac failed: ${err}"
        input = "java/lang/String_${NondetInputs.ids(classes, "Main")[0]}".toString()
    }

    def cleanupSpec() {
        classes?.toFile()?.deleteDir()
    }

    @Unroll
    def "a modelled match is a regex membership of the symbolic string (#scenario, input '#value')"() {
        when:
        String out = runOutput(scenario, value)
        Map trace = parse(out)

        then:
        out.readLines().contains(expected)
        !trace.symbolicContextLoss
        trace.trace.any { !it.concreteOnly && it.constraint?.contains("str.in_re ${input}".toString()) }

        where:
        scenario         | value          || expected
        "matches"        | ""             || "no match"
        "matches"        | "0123456789#"  || "match"
        "patternMatches" | ""             || "no match"
        "patternMatches" | "42"           || "match"
        "twice"          | "7"            || "match"
    }

    @Unroll
    def "an unmodelled regex call on a symbolic string records context loss (#scenario)"() {
        expect:
        parse(runOutput(scenario, "")).symbolicContextLoss

        where:
        scenario << ["find", "reset", "untranslatable", "flags"]
    }

    private static Map parse(String out) {
        int start = out.indexOf("\n{\n")
        int end = out.indexOf("\n}\n", start)
        assert start >= 0 && end > start: "no trace JSON in output:\n${out}"
        return new ObjectMapper().readValue(out.substring(start + 1, end + 2), Map)
    }

    private String runOutput(String scenario, String value) {
        def java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        def cmd = [java,
                   "-javaagent:${AGENT_JAR}".toString(),
                   "-Djava.library.path=${NATIVE_LIBS}".toString(),
                   "-Dconfig.path=/nonexistent/swat.cfg",
                   "-Dinstrumentation.transformer=SV_COMP",
                   "-Dsolver.mode=PRINT",
                   "-DexitOnError=false",
                   "-DlogShadowStateToConsole=false"] +
                  (value.isEmpty() ? [] : ["-Dswat.input.${input}=${value}".toString()]) +
                  ["-cp", classes.toString(), "-ea", "Main", scenario]
        def proc = new ProcessBuilder(cmd).directory(classes.toFile()).redirectErrorStream(true).start()
        def out = proc.inputStream.text
        assert proc.waitFor(5, TimeUnit.MINUTES): "target did not finish:\n${out}"
        return out
    }
}
