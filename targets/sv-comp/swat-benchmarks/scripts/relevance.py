"""Tasks for the relevance analysis (slicing, see SWAT's Relevance.py).

`relevance`: tasks where most branches cannot affect the assert, so that reachability pruning keeps
them all and the search has to enumerate their combinations, while slicing does not flip them:
the example from the design discussion verbatim (`example`, `example-long`), and families over the
loop length n.

`relevance-soundness`: SAFE/VIOLATION pairs in which a branch that looks irrelevant at first sight
does matter (it writes what the assert reads, through a field, an array, an alias, a callee, a
library object, a callback or a static initializer; it reads inputs; it may end the program; it
throws to a handler elsewhere), or in which the initial path ends inside an irrelevant region. A
false SAFE on a VIOLATION task means the slice was wrong. @HIT(x)@ is `x > 1000 && x < HI` with
HI = 100000 (VIOLATION) or 1001 (SAFE, unsatisfiable), as in sa-soundness.
"""

from soundness import render
from task import Task, ints

SAFE_REASON = 'the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.'
IMPORT = 'import org.sosy_lab.sv_benchmarks.Verifier;\n'

EXAMPLE = '''IMPORT
// relevance/example: the loop's branches all lead to the assert either way, so reachability
// pruning keeps them; none of them can change val, so slicing does not flip them.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        for (int i = 0; i < @N@; i++) {
            if (i < val) {
                System.out.println(i);
            }
        }
        assert val != 3141592;
    }
}
'''

FAMILIES = {
    # name: (source, witness); @N@ is the loop length
    'loop-print': ('''IMPORT
// relevance/loop-print: an input-dependent branch per iteration that only prints.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        for (int i = 0; i < @N@; i++) {
            if (i < val) {
                System.out.println(i);
            }
        }
        if (@HIT(val)@) assert false;
    }
}
''', ints([1001])),
    'loop-helper': ('''IMPORT
// relevance/loop-helper: each iteration calls a helper with input-dependent branches that only
// update a counter the assert does not read.
public class Main {
    static int logged;

    static void log(int i, int v) {
        if (i % 2 == 0) {
            if (i < v) {
                logged++;
            }
        } else if (i > v) {
            logged += 2;
        }
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        for (int i = 0; i < @N@; i++) {
            log(i, val);
        }
        if (@HIT(val)@) assert false;
    }
}
''', ints([1001])),
    'scratch-array': ('''IMPORT
// relevance/scratch-array: the loop fills a scratch array (bounds-checked) the assert never reads.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int[] hist = new int[@N@];
        for (int i = 0; i < @N@; i++) {
            if (i < val) {
                hist[i] = i;
            } else {
                hist[i] = -i;
            }
        }
        System.out.println(hist.length);
        if (@HIT(val)@) assert false;
    }
}
''', ints([1001])),
    'two-inputs': ('''IMPORT
// relevance/two-inputs: the loop branches on one input, the assert on another.
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < @N@; i++) {
            if (i < a) {
                System.out.print('.');
            } else if (i == a + 1) {
                System.out.print('#');
            }
        }
        System.out.println();
        if (b > a) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([0, 1001])),
}

SOUNDNESS = {
    'relevant-counter': ('''IMPORT
// The loop's branch counts, and the count decides whether the assert is reached: relevant.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int count = 0;
        for (int i = 0; i < 10; i++) {
            if (i < val) {
                count++;
            }
        }
        if (count == 7) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([7, 1001])),
    'field-write': ('''IMPORT
// The region writes a static field the assert's guard reads.
public class Main {
    static int flag;

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                flag = 1;
            }
        }
        if (flag == 1) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([3, 1001])),
    'array-write': ('''IMPORT
// The region writes an array element that is summed up later.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int[] marks = new int[10];
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                marks[i] = 1;
            }
        }
        int sum = 0;
        for (int j = 0; j < 10; j++) {
            sum += marks[j];
        }
        if (sum == 1) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([3, 1001])),
    'alias-field': ('''IMPORT
// The region writes a field through an alias of the object the assert's guard reads.
public class Main {
    static class Box {
        int v;
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Box p = new Box();
        Box q = p;
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                q.v = 1;
            }
        }
        if (p.v == 1) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([3, 1001])),
    'callee-write': ('''IMPORT
// The region calls a method that writes a field the assert's guard reads.
public class Main {
    static boolean marked;

    static void mark() {
        marked = true;
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                mark();
            }
        }
        if (marked) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([3, 1001])),
    'return-value': ('''IMPORT
// The region decides a callee's return value.
public class Main {
    static int scan(int val) {
        int found = -1;
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                found = i;
            }
        }
        return found;
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (scan(val) == 4) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([4, 1001])),
    'loop-break': ('''IMPORT
// The branch leaves the loop early, which decides the counter read after it.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int i;
        for (i = 0; i < 10; i++) {
            if (i == val) {
                break;
            }
        }
        if (i == 6) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([6, 1001])),
    'input-shift': ('''IMPORT
// The region reads inputs, which shifts which input every later read gets.
public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        for (int i = 0; i < 3; i++) {
            if (i < x) {
                int junk = Verifier.nondetInt();
                System.out.println(junk);
            }
        }
        int z = Verifier.nondetInt();
        if (x == 2) {
            if (@HIT(z)@) assert false;
        }
    }
}
''', ints([2, 0, 0, 1001])),
    'assume-halt': ('''IMPORT
// An assumption in the region ends the initial run (val = 0) before the assert.
// By design, the all-zero run ends early.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 10; i++) {
            if (val < i) {
                Verifier.assume(false);
            }
        }
        if (@HIT(b)@) assert false;
    }
}
''', ints([9, 1001])),
    'exit-in-region': ('''IMPORT
// System.exit in the region ends the initial run (val = 0) before the assert.
// By design, the all-zero run ends early.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 10; i++) {
            if (val < i) {
                System.out.println("bye");
                System.exit(0);
            }
        }
        if (@HIT(b)@) assert false;
    }
}
''', ints([9, 1001])),
    'exception-skip': ('''IMPORT
// The region throws to a handler in the caller, which skips a write the assert's guard reads.
public class Main {
    static int done;

    static void work(int val) {
        for (int i = 0; i < 10; i++) {
            if (i == val - 5) {
                throw new IllegalStateException("stop");
            }
        }
        done = 1;
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        try {
            work(val);
        } catch (IllegalStateException e) {
            System.out.println("caught");
        }
        if (done == 0) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([5, 1001])),
    'local-handler': ('''IMPORT
// The region catches an exception of a library call locally and records it.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int state = 0;
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                try {
                    Integer.parseInt("x" + i);
                } catch (NumberFormatException e) {
                    state = 1;
                }
            }
        }
        if (state == 1) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([3, 1001])),
    'crash-on-initial-path': ('''IMPORT
// The initial run (val = 0) ends with an uncaught exception inside the region, before the assert.
// By design, the all-zero run ends early.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int[] a = new int[5];
        for (int i = 0; i < 10; i++) {
            if (i >= val) {
                a[i] = 1;
            }
        }
        if (@HIT(b)@) assert false;
    }
}
''', ints([10, 1001])),
    'crash-in-callee': ('''IMPORT
// The initial run (val = 0) throws an uncaught exception in a callee inside the region.
// By design, the all-zero run ends early.
public class Main {
    static void check(int i, int v) {
        if (i > v + 5) {
            throw new RuntimeException("too far");
        }
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 10; i++) {
            if (i != val) {
                check(i, val);
            }
        }
        if (@HIT(b)@) assert false;
    }
}
''', ints([4, 1001])),
    'library-state': ('''import java.util.ArrayList;
import java.util.List;
IMPORT
// The region adds to a library list whose size the assert's guard reads.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                list.add(i);
            }
        }
        if (list.size() == 1) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([3, 1001])),
    'string-builder': ('''IMPORT
// The region appends to a StringBuilder whose length the assert's guard reads.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i == val) {
                sb.append('x');
            }
        }
        if (sb.length() == 1) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([3, 1001])),
    'callback-write': ('''import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
IMPORT
// The region calls library code that calls back into a comparator, which writes a field the
// assert's guard reads.
public class Main {
    static boolean compared;

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        List<Integer> list = new ArrayList<Integer>();
        list.add(2);
        list.add(1);
        for (int i = 0; i < 5; i++) {
            if (i == val) {
                Collections.sort(list, new Comparator<Integer>() {
                    public int compare(Integer x, Integer y) {
                        compared = true;
                        return x - y;
                    }
                });
            }
        }
        if (compared) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([2, 1001])),
    'print-callback': ('''IMPORT
// The region prints an object whose toString() (called back by the library) counts.
public class Main {
    static int printed;

    static class Item {
        public String toString() {
            printed++;
            return "item";
        }
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Item it = new Item();
        for (int i = 0; i < 5; i++) {
            if (i == val) {
                System.out.println(it);
            }
        }
        if (printed == 1) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([2, 1001])),
    'clinit-in-region': ('''IMPORT
// The region is where a class is first used; its static initializer writes a field the assert's
// guard reads.
public class Main {
    static boolean initialized;

    static class Config {
        static int level;

        static {
            level = 3;
            Main.initialized = true;
        }
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 5; i++) {
            if (i == val) {
                System.out.println(Config.level);
            }
        }
        if (initialized) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([2, 1001])),
    'shared-helper': ('''IMPORT
// A helper with a branch is called both in an irrelevant region and to compute what the assert's
// guard reads.
public class Main {
    static int clamp(int x) {
        if (x > 100) {
            return 100;
        }
        return x;
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 10; i++) {
            if (i < val) {
                System.out.println(clamp(i));
            }
        }
        if (clamp(val) == 100) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([101, 1001])),
    'switch-in-region': ('''IMPORT
// A switch in the region sets the mode the assert's guard reads.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int mode = 0;
        for (int i = 0; i < 4; i++) {
            switch (i - val) {
                case 0:
                    mode = 2;
                    break;
                case 1:
                    System.out.println("one");
                    break;
                default:
                    break;
            }
        }
        if (mode == 2) {
            if (@HIT(b)@) assert false;
        }
    }
}
''', ints([2, 1001])),
}


def all_relevance_tasks():
    for n, base in ((100, 'example'), (100000, 'example-long')):
        src = EXAMPLE.replace('IMPORT\n', IMPORT).replace('@N@', str(n))
        yield Task('relevance', base, False, src, ints([3141592]), '')
    for name, (src, witness) in FAMILIES.items():
        for n in (10, 100, 1000):
            for safe in (True, False):
                yield Task('relevance', f'{name}_n{n:04d}', safe,
                           render(src.replace('@N@', str(n)), safe), witness, SAFE_REASON)
    for name, (src, witness) in SOUNDNESS.items():
        for safe in (True, False):
            yield Task('relevance-soundness', name, safe, render(src, safe), witness, SAFE_REASON)
