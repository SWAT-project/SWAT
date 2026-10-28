"""Tasks for loop summarization (see the executor's loop summaries and RESULTS.md).

`loop-summary`: families over the loop length n in which the assert depends on what a loop
computes from an input, so the loop's branches are relevant and slicing cannot help:
- diamonds: an input-dependent branch per iteration that only updates a counter or an accumulator
  (`count-below`, `ternary-sum`, `found-index`); merged, the counter is a formula of the input and
  the assert's branch can be flipped directly, instead of one more iteration per round;
- counting loops whose bound is an input (`count-up`, `count-down`); summarized, the loop's
  variables are closed forms of the input and its trip count needs no enumeration.
SAFE variants ask for a value the loop can never produce.

`loop-summary-soundness`: SAFE/VIOLATION pairs that must not be summarized (a call, a branch or a
library object in the body, a non-strict bound) or where summarizing must keep Java's overflow,
plus nested loops. As in sa-soundness, @HIT(x)@ is `x > 1000 && x < HI`.
"""

from soundness import render
from task import Task, ints

IMPORT = 'import org.sosy_lab.sv_benchmarks.Verifier;\n'

FAMILIES = {
    # name: (source, violating value of K / SAFE value of K, witness as a function of n)
    'count-below': ('''IMPORT
// loop-summary/count-below: count = number of i < n with i < val.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int count = 0;
        for (int i = 0; i < @N@; i++) {
            if (i < val) {
                count++;
            }
        }
        if (count == @K@) assert false;
    }
}
''', lambda n: n - 3, lambda n: n + 1, lambda n: ints([n - 3]),
        'count is at most n.'),
    'ternary-sum': ('''IMPORT
// loop-summary/ternary-sum: s grows by 2 in every iteration with i < val (a conditional expression).
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int s = 0;
        for (int i = 0; i < @N@; i++) {
            s = i < val ? s + 2 : s;
        }
        if (s == @K@) assert false;
    }
}
''', lambda n: 2 * (n - 3), lambda n: 2 * n + 2, lambda n: ints([n - 3]),
        's is at most 2n.'),
    'found-index': ('''IMPORT
// loop-summary/found-index: found = the index equal to val, or -1.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int found = -1;
        for (int i = 0; i < @N@; i++) {
            if (i == val) {
                found = i;
            }
        }
        if (found == @K@) assert false;
    }
}
''', lambda n: n - 2, lambda n: n, lambda n: ints([n - 2]),
        'found is below n.'),
    'count-up': ('''IMPORT
// loop-summary/count-up: a counting loop up to an input bound.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s += 3;
        }
        if (s == @K@) assert false;
    }
}
''', lambda n: 3 * n, lambda n: 3 * n + 1, lambda n: ints([n]),
        's = 3 * max(val, 0) with val <= 2^31 - 1, which is never 3n + 1 (mod 2^32).'),
    'count-down': ('''IMPORT
// loop-summary/count-down: a loop counting an input down to zero.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int i = val;
        int c = 0;
        while (i > 0) {
            i--;
            c += 2;
        }
        if (c == @K@) assert false;
    }
}
''', lambda n: 2 * n, lambda n: 2 * n + 1, lambda n: ints([n]),
        'c is even.'),
    'count-even': ('''IMPORT
// loop-summary/count-even: a nested condition per iteration (i % 2 on a constant divisor).
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int even = 0;
        int odd = 0;
        for (int i = 0; i < @N@; i++) {
            if (i < val) {
                if (i % 2 == 0) {
                    even++;
                } else {
                    odd++;
                }
            }
        }
        if (even == @K@) assert false;
    }
}
''', lambda n: (n - 3 + 1) // 2, lambda n: n // 2 + 1, lambda n: ints([n - 3]),
        'even counts the even i < min(val, n): at most n / 2.'),
    'count-upto': ('''IMPORT
// loop-summary/count-upto: a counting loop up to and including an input bound.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int i = 1;
        int s = 0;
        while (i <= val) {
            i++;
            s += 2;
        }
        if (s == @K@) assert false;
    }
}
''', lambda n: 2 * n, lambda n: 2 * n + 1, lambda n: ints([n]),
        's is even.'),
    'count-up-print': ('''IMPORT
// loop-summary/count-up-print: a counting loop up to an input bound that also prints.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s += 3;
            System.out.println(i);
        }
        if (s == @K@) assert false;
    }
}
''', lambda n: 3 * n, lambda n: 3 * n + 1, lambda n: ints([n]),
        's = 3 * max(val, 0) with val <= 2^31 - 1, which is never 3n + 1 (mod 2^32).'),
    'count-up-log': ('''IMPORT
// loop-summary/count-up-log: a counting loop that also logs into a field and a scratch array.
public class Main {
    static int logged;
    static int[] recent = new int[8];

    static void log(int i) {
        logged++;
        recent[i & 7] = i;
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s += 2;
            log(i);
        }
        if (s == @K@) assert false;
    }
}
''', lambda n: 2 * n, lambda n: 2 * n + 1, lambda n: ints([n]),
        's is even.'),
}

SOUNDNESS = {
    'body-field-relevant': ('''IMPORT
// A counting loop whose body also counts in a field the assert's guard reads: the field's count
// depends on the trip count but has no closed form, so the summary must not be used.
public class Main {
    static int hits;

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s++;
            hits = hits + 1;
        }
        if (hits == 5) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([5, 1001])),
    'body-call-relevant': ('''IMPORT
// A counting loop whose body calls a helper that sets a flag the assert's guard reads.
public class Main {
    static boolean reached;

    static void visit(int i) {
        if (i == 4) {
            reached = true;
        }
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int i = 0;
        while (i < val) {
            i++;
            visit(i);
        }
        if (reached) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([4, 1001])),
    'body-call': ('''IMPORT
// A counting loop with a call in its body: not a pure loop.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s += 2;
            System.out.println(i);
        }
        if (s == 10) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([5, 1001])),
    'body-branch': ('''IMPORT
// A counting loop with a branch in its body.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            if (i == 3) {
                s += 10;
            }
            s += 1;
        }
        if (s == 15) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([5, 1001])),
    'heap-in-loop': ('''import java.util.ArrayList;
import java.util.List;
IMPORT
// A counting loop that adds to a list, whose size the assert's guard reads.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        List<Integer> list = new ArrayList<Integer>();
        int i = 0;
        while (i < val) {
            i++;
            list.add(i);
        }
        if (list.size() == 4) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([4, 1001])),
    'non-strict-bound': ('''IMPORT
// i <= val: the loop never ends for val = 2^31 - 1, so it has no closed form.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (val > 100) return;
        int i = 0;
        int s = 0;
        while (i <= val) {
            i++;
            s++;
        }
        if (s == 6) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([5, 1001])),
    'counter-overflow': ('''IMPORT
// The merged counter must wrap around like Java's int: three steps of 10^9 are negative.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (i < val) {
                count += 1000000000;
            }
        }
        if (count < 0) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([3, 1001])),
    'step-overflow': ('''IMPORT
// The summarized loop's accumulator must wrap around like Java's int.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (val > 100) return;
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s += 1000000000;
        }
        if (s < 0) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([3, 1001])),
    'nested-loops': ('''IMPORT
// A counting loop inside a loop: c = 4 * max(val, 0).
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int c = 0;
        for (int k = 0; k < 4; k++) {
            int j = 0;
            while (j < val) {
                j++;
                c++;
            }
        }
        if (c == 12) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([3, 1001])),
    'diamond-then-check': ('''IMPORT
// Merged counters feed a later input-dependent check.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int even = 0;
        int odd = 0;
        for (int i = 0; i < 10; i++) {
            if (i < val) {
                if (i % 2 == 0) {
                    even++;
                } else {
                    odd++;
                }
            }
        }
        if (even == 3 && odd == 2) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([5, 1001])),
    'negative-start': ('''IMPORT
// A counting loop from an input start to an input bound.
public class Main {
    public static void main(String[] args) {
        int lo = Verifier.nondetInt();
        int hi = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (hi - lo > 50 || lo < -50) return;
        int i = lo;
        int n = 0;
        while (i < hi) {
            i++;
            n++;
        }
        if (n == 7 && i == 3) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([-4, 3, 1001])),
}

SAFE_REASON = 'the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.'


def all_loop_summary_tasks():
    for name, (src, k_violation, k_safe, witness, reason) in FAMILIES.items():
        for n in (10, 100, 1000):
            for safe in (True, False):
                k = k_safe(n) if safe else k_violation(n)
                text = src.replace('IMPORT\n', IMPORT).replace('@N@', str(n)).replace('@K@', str(k))
                yield Task('loop-summary', f'{name}_n{n:04d}', safe, text, witness(n), reason)
    for name, (src, witness) in SOUNDNESS.items():
        for safe in (True, False):
            yield Task('loop-summary-soundness', name, safe, render(src, safe), witness, SAFE_REASON)
