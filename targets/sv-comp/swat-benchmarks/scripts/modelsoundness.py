"""model-soundness: SAFE/VIOLATION pairs for SWAT's models of library calls (one group per model).
A model must agree with the JVM, or not be used (context loss): a VIOLATION task that comes out
SAFE means a model hid a reachable path. In each pair the initial run (all inputs 0, the empty
string) does not reach the assert.

Each entry: name -> (source, witness, safe_reason). The source holds @SAFE|VIOLATION@ for the
one spot where the two variants differ. witness: the violating inputs in nondet call order.

Expected SWAT results: the pairs of a modelled call give SAFE and VIOLATION; for the calls that
are deliberately not modelled (marked "context loss") the SAFE variant must stay UNKNOWN and the
VIOLATION variant must not be SAFE.
"""

import re

from task import Task

IMPORT = 'import org.sosy_lab.sv_benchmarks.Verifier;\n'

TASKS = {}


def task(name, witness, safe_reason):
    def register(fn):
        TASKS[name] = (fn.__doc__, witness, safe_reason)
        return fn
    return register


# --- java.util.regex: Pattern.compile(c).matcher(s).matches() (modelled) -----------------------

@task('regex-matcher-match', [('string', '12')],
      'a string of digits only does not start with "x".')
def _():
    """import java.util.regex.Pattern;
IMPORT
// The matching side of Matcher.matches() on a symbolic string, the pattern in a static field.
public class Main {
    private static final Pattern DIGITS = Pattern.compile("[0-9]+");

    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (DIGITS.matcher(s).matches()) {
            if (s.startsWith(@"x"|"1"@)) assert false;
        }
    }
}
"""


@task('regex-matcher-nomatch', [('string', '1a')],
      'a string of two chars that are both digits matches [0-9]{2}.')
def _():
    """import java.util.regex.Matcher;
import java.util.regex.Pattern;
IMPORT
// The non-matching side of Matcher.matches() on a symbolic string.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Pattern p = Pattern.compile("[0-9]{2}");
        Matcher m = p.matcher(s);
        if (!m.matches() && s.length() == 2 && s.charAt(0) >= '0' && s.charAt(0) <= '9') {
            if (@s.charAt(1) >= '0' && s.charAt(1) <= '9'|s.charAt(1) == 'a'@) assert false;
        }
    }
}
"""


@task('regex-matcher-aid', [('string', '0123456789#')],
      'a match of the pattern starts with at least 10 hex digits.')
def _():
    """import java.util.regex.Pattern;
IMPORT
// The pattern of argv-mhe's AidGroup (escaped '*' and '#', a bounded repetition).
public class Main {
    private static final Pattern AID = Pattern.compile("[0-9A-Fa-f]{10,32}\\\\*?\\\\#?");

    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (AID.matcher(s).matches()) {
            if (@s.length() < 10|s.endsWith("#")@) assert false;
        }
    }
}
"""


@task('regex-pattern-matches', [('string', 'ab')],
      'a string of digits only does not start with "a".')
def _():
    """import java.util.regex.Pattern;
IMPORT
// The static Pattern.matches(regex, s), which is s.matches(regex).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (Pattern.matches(@"[0-9]+"|"[a-z]+"@, s)) {
            if (s.startsWith("a")) assert false;
        }
    }
}
"""


@task('regex-matcher-twice', [('string', '7')],
      'matches() gives the same answer twice on an unchanged matcher.')
def _():
    """import java.util.regex.Matcher;
import java.util.regex.Pattern;
IMPORT
// matches() called twice on one matcher (it does not depend on the earlier match).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Matcher m = Pattern.compile("[0-9]").matcher(s);
        boolean first = m.matches();
        boolean second = m.matches();
        if (@first != second|first && second@) assert false;
    }
}
"""


# --- java.util.regex: deliberately not modelled (context loss) ----------------------------------

@task('regex-untranslatable', [('string', 'abc')],
      'a match of a.c has three chars. "." is not translated: SWAT must say UNKNOWN.')
def _():
    """import java.util.regex.Pattern;
IMPORT
// A regular expression SWAT does not translate ("."): context loss, SAFE must not be claimed.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (Pattern.compile("a.c").matcher(s).matches()) {
            if (@s.length() != 3|s.length() == 3@) assert false;
        }
    }
}
"""


@task('regex-flags', [('string', 'AB')],
      'with CASE_INSENSITIVE a match is letters only, so it has no digit "1". Flags are not modelled: UNKNOWN.')
def _():
    """import java.util.regex.Pattern;
IMPORT
// A pattern with flags (CASE_INSENSITIVE): not modelled, context loss. A model that ignored the
// flags would hide the violation (an upper-case match).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Pattern p = Pattern.compile("[a-z]+", Pattern.CASE_INSENSITIVE);
        if (p.matcher(s).matches()) {
            if (s.startsWith(@"1"|"A"@)) assert false;
        }
    }
}
"""


@task('regex-find', [('string', 'x12')],
      'find() of [0-9]{2} in a string of one char fails. find() is not modelled: UNKNOWN.')
def _():
    """import java.util.regex.Matcher;
import java.util.regex.Pattern;
IMPORT
// Matcher.find() is not modelled: context loss. A model that took it for matches() would hide
// the violation (a match inside the string).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Matcher m = Pattern.compile("[0-9]{2}").matcher(s);
        if (m.find()) {
            if (@s.length() == 1|s.startsWith("x")@) assert false;
        }
    }
}
"""


@task('regex-reset', [('string', 'a'), ('string', '1')],
      'after reset(t), matches() of [0-9]+ is about t, which is a digit string and does not start with "x".'
      ' reset is not modelled: UNKNOWN.')
def _():
    """import java.util.regex.Matcher;
import java.util.regex.Pattern;
IMPORT
// A matcher reused on another input with reset(t): not modelled, context loss. A model that kept
// matching s would hide the violation (s does not match, t does).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        String t = Verifier.nondetString();
        Matcher m = Pattern.compile("[0-9]+").matcher(s);
        boolean first = m.matches();
        m.reset(t);
        boolean second = m.matches();
        if (!first && second) {
            if (@t.startsWith("x")|t.length() == 1@) assert false;
        }
    }
}
"""


@task('regex-reset-concrete', [('string', 'a')],
      'after reset("123"), matches() is true, so the assert needs !true. reset is not modelled: UNKNOWN.')
def _():
    """import java.util.regex.Matcher;
import java.util.regex.Pattern;
IMPORT
// A matcher of a symbolic string reset to a concrete one: not modelled, context loss.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        Matcher m = Pattern.compile("[0-9]+").matcher(s);
        boolean first = m.matches();
        m.reset("123");
        boolean second = m.matches();
        if (!first && s.length() == 1) {
            if (@!second|second@) assert false;
        }
    }
}
"""


def render(src: str, safe: bool) -> str:
    src = src.replace('IMPORT\n', IMPORT)
    return re.sub(r'@(.*?)\|(.*?)@', lambda m: m.group(1) if safe else m.group(2), src)


def all_model_soundness_tasks():
    for name, (src, witness, safe_reason) in TASKS.items():
        for safe in (True, False):
            yield Task('model-soundness', name, safe, render(src, safe), witness, safe_reason)
