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


# --- String.toLowerCase / toUpperCase on a symbolic string ----------------------------------
# Exact for ASCII strings (length pinned, ASCII split, char by char), approximate otherwise; the
# pins matter only where a branch reads the result. A SAFE variant whose search needs every
# length (the result is branched on) may stay UNKNOWN; `case-unused` must come out SAFE.

@task('case-lower-equals', [('string', 'Abc')],
      'only "abc", "ABC" and the like lower-case to "abc" (no char beyond ASCII has a, b or c as its lower case).')
def _():
    """IMPORT
// A symbolic string whose lower case equals a constant.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.toLowerCase().equals("abc")) {
            if (@s.length() != 3|!s.equals("abc")@) assert false;
        }
    }
}
"""


@task('case-upper-letter', [('string', 'Ok')],
      '"xy" lower-cases to "xy", not "ok".')
def _():
    """IMPORT
// A violation only an upper-case letter reaches.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        String l = s.toLowerCase();
        if (s.length() == 2 && l.equals("ok")) {
            if (@s.equals("xy")|!s.equals("ok")@) assert false;
        }
    }
}
"""


@task('case-kelvin', [('string', 'K')],
      'only K, k and U+212A (KELVIN SIGN) lower-case to "k".')
def _():
    """IMPORT
// A violation only a char beyond ASCII reaches: the Kelvin sign lower-cases to the ASCII "k".
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1 && s.toLowerCase().equals("k")) {
            char c = s.charAt(0);
            if (c != 'K' && c != 'k'@ && c != '\\u212a'|@) assert false;
        }
    }
}
"""


@task('case-length-lower', [('string', 'İ')],
      'the lower case of one char has at most two chars (U+0130 gives "i\\u0307").')
def _():
    """IMPORT
// toLowerCase may change the length: "\\u0130" (I with dot above) lower-cases to two chars.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1) {
            String l = s.toLowerCase();
            if (@l.length() > 2|l.length() != 1@) assert false;
        }
    }
}
"""


@task('case-length-upper', [('string', 'ß')],
      'the upper case of one char has at most three chars (e.g. U+0390).')
def _():
    """IMPORT
// toUpperCase may change the length: "\\u00df" (sharp s) upper-cases to "SS".
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1) {
            String u = s.toUpperCase();
            if (@u.length() > 3|u.length() != 1@) assert false;
        }
    }
}
"""


@task('case-mixed', [('string', 'a1-B!')],
      'only a or A upper-cases to A (and 1, -, ! to themselves), so the lower case is "a1-b!".')
def _():
    """IMPORT
// Digits and punctuation are kept, letters mapped: upper and lower case of the same string.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.toUpperCase().equals("A1-B!")) {
            if (@!s.toLowerCase().equals("a1-b!")|s.charAt(0) == 'a'@) assert false;
        }
    }
}
"""


@task('case-unused', [('string', 'xxxx')],
      'a string longer than 3 chars is not shorter than 3; the lower case is never branched on.')
def _():
    """IMPORT
// The lower case is computed but never branched on (as in securibench, where it is only printed):
// the model's pins are irrelevant, the search need not try every length, and SAFE is expected.
public class Main {
    static String sink;

    public static void main(String[] args) {
        String s = Verifier.nondetString();
        sink = s.toLowerCase(java.util.Locale.ROOT);
        if (s.length() > 3) {
            if (@s.length() < 3|s.charAt(0) == 'x'@) assert false;
        }
    }
}
"""


@task('case-later-use', [('string', 'Abcd')],
      '"xyzw" lower-cases to "xyzw", not "abcd".')
def _():
    """IMPORT
// The lower case is branched on only where the string has another length than in the first run
// (the pins of the first run are irrelevant there, those of the run of length 4 are not).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        String l = s.toLowerCase();
        if (s.length() == 4) {
            if (l.equals("abcd") && @s.equals("xyzw")|s.charAt(0) == 'A'@) assert false;
        }
    }
}
"""


@task('case-turkish', [('string', 'İ')],
      '"x" lower-cases to "x", not "i".')
def _():
    """IMPORT
// A Turkish locale maps I with dot above to the ASCII "i": not modelled (approximate).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1 && s.toLowerCase(new java.util.Locale("tr")).equals("i")) {
            if (@s.equals("x")|!s.equals("i")@) assert false;
        }
    }
}
"""


@task('case-dotted-i', [('string', '\u0130')],
      '"x" lower-cases to "x", which has no "i".')
def _():
    """IMPORT
// The lower case of "\\u0130" is "i\\u0307", which contains "i": a comparison with a string
// that has a char of a multi-char mapping is not modelled over the original string.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1 && s.toLowerCase().contains("i")) {
            if (@s.equals("x")|s.charAt(0) != 'i' && s.charAt(0) != 'I'@) assert false;
        }
    }
}
"""


@task('case-sharp-s', [('string', '\u00df')],
      '"x" upper-cases to "X", not "SS".')
def _():
    """IMPORT
// The upper case of "\\u00df" (sharp s) is "SS": a string of one char upper-cases to two.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        if (s.length() == 1 && s.toUpperCase().equals("SS")) {
            if (@s.equals("x")|true@) assert false;
        }
    }
}
"""


@task('case-sink', [('string', '<BAD/>')],
      'only x and X upper-case to "X", and their lower case "x" does not contain "<bad/>".')
def _():
    """IMPORT
// As the securibench sinks: the case mapping result is only searched for a concrete string.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        String l = s.toLowerCase();
        if (l.contains("<bad/>")) {
            if (@s.toUpperCase().equals("X")|!s.contains("<bad/>")@) assert false;
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
