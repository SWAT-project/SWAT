"""sa-soundness: SAFE/VIOLATION pairs around constructs where the SA graph and the executor can
disagree. In every pair the assert sits behind the risky construct, and the initial run (all
inputs 0) does not reach it: some input-dependent branch has to be flipped first, which a pruning
bug can wrongly skip. A false SAFE on a VIOLATION task means exactly that.

@HIT(x)@ is the condition at the assert: `x > 1000 && x < 100000` in the VIOLATION variant and
the unsatisfiable `x > 1000 && x < 1001` in the SAFE one. Each entry: name -> (source, witness),
witness being the violating inputs in nondet call order.
"""

import re

from task import Task, ints

SAFE_REASON = 'the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.'

IMPORT = 'import org.sosy_lab.sv_benchmarks.Verifier;\n'
AB = '''        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
'''
W = ints([1001, 7])  # a = 1001, b = 7

TASKS = {}


def task(name, witness=W):
    def register(fn):
        TASKS[name] = (fn.__doc__, witness)
        return fn
    return register


# --- arrays from uninstrumented JDK calls ------------------------------------------------------

@task('jdk-toarray')
def _():
    """import java.util.ArrayList;
import java.util.List;
IMPORT
// An array returned by an uninstrumented JDK call is read before the assert (bounds guard on an
// untracked array; regression test for the phantom bounds-guard bug).
public class Main {
    public static void main(String[] args) {
AB        List<Integer> list = new ArrayList<Integer>();
        list.add(3);
        list.add(5);
        Object[] arr = list.toArray();
        int x = (Integer) arr[1];
        if (b == 7) {
            if (@HIT(a + x - 5)@) assert false;
        }
    }
}
"""


@task('jdk-getmethods')
def _():
    """import java.lang.reflect.Method;
IMPORT
// Class.getMethods() returns an array the executor does not track; it is read before the assert.
public class Main {
    public static void main(String[] args) {
AB        Method[] ms = Main.class.getMethods();
        int n = ms.length;
        String name = ms[0].getName();
        if (b == 7) {
            if (@HIT(a)@) assert false;
        }
        if (n > 3) n--;
    }
}
"""


# --- implicit exceptions unwinding across frames -----------------------------------------------

@task('arith-catch-local')
def _():
    """IMPORT
// The assert is in a catch (ArithmeticException) in the method that divides.
public class Main {
    public static void main(String[] args) {
AB        int r;
        try {
            r = 100 / (b - 7);
        } catch (ArithmeticException e) {
            if (@HIT(a)@) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
"""


@task('arith-catch-up1')
def _():
    """IMPORT
// The assert is in a catch (ArithmeticException) one frame above the division.
public class Main {
    static int div(int x, int y) {
        int q = x / y;
        if (q > 10) q--;
        return q;
    }

    public static void main(String[] args) {
AB        int r;
        try {
            r = div(100, b - 7);
        } catch (ArithmeticException e) {
            if (@HIT(a)@) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
"""


@task('arith-catch-up2')
def _():
    """IMPORT
// The assert is in a catch (ArithmeticException) two frames above the division.
public class Main {
    static int div(int x, int y) {
        int q = x / y;
        if (q > 10) q--;
        return q;
    }

    static int mid(int x, int y) {
        int r = div(x, y);
        if (r < 0) r = -r;
        return r;
    }

    public static void main(String[] args) {
AB        int r;
        try {
            r = mid(100, b - 7);
        } catch (ArithmeticException e) {
            if (@HIT(a)@) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
"""


@task('bounds-catch-up1')
def _():
    """IMPORT
// The assert is in a catch (ArrayIndexOutOfBoundsException) one frame above the array read.
public class Main {
    static final int[] TABLE = {1, 2, 3};

    static int get(int i) {
        int v = TABLE[i];
        if (v > 1) v--;
        return v;
    }

    public static void main(String[] args) {
AB        int r;
        try {
            r = get(b);
        } catch (ArrayIndexOutOfBoundsException e) {
            if (@HIT(a)@) assert false;
            r = 0;
        }
        if (r > 1) r--;
    }
}
"""


@task('bounds-catch-up2')
def _():
    """IMPORT
// The assert is in a catch (ArrayIndexOutOfBoundsException) two frames above the array read.
public class Main {
    static final int[] TABLE = {1, 2, 3};

    static int get(int i) {
        int v = TABLE[i];
        if (v > 1) v--;
        return v;
    }

    static int lookup(int i) {
        int v = get(i);
        if (v < 0) v = 0;
        return v;
    }

    public static void main(String[] args) {
AB        int r;
        try {
            r = lookup(b);
        } catch (ArrayIndexOutOfBoundsException e) {
            if (@HIT(a)@) assert false;
            r = 0;
        }
        if (r > 1) r--;
    }
}
"""


@task('arith-catch-generic')
def _():
    """IMPORT
// The division's ArithmeticException is caught as RuntimeException one frame up.
public class Main {
    static int div(int x, int y) {
        return x / y;
    }

    public static void main(String[] args) {
AB        int r;
        try {
            r = div(100, b - 7);
            if (r > 3) r--;
        } catch (RuntimeException e) {
            if (@HIT(a)@) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
"""


# --- exceptions SWAT has no guard branch for ---------------------------------------------------

@task('npe-catch-local')
def _():
    """IMPORT
// The assert is in a catch (NullPointerException) in the method that dereferences.
public class Main {
    static class Box {
        int v = 3;
    }

    public static void main(String[] args) {
AB        Box box = b == 7 ? null : new Box();
        int r;
        try {
            r = box.v;
            if (r > 2) r--;
        } catch (NullPointerException e) {
            if (@HIT(a)@) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
"""


@task('npe-catch-up1')
def _():
    """IMPORT
// The assert is in a catch (NullPointerException) one frame above the dereference.
public class Main {
    static class Box {
        int v = 3;
    }

    static int read(Box box) {
        int v = box.v;
        if (v > 2) v--;
        return v;
    }

    public static void main(String[] args) {
AB        Box box = b == 7 ? null : new Box();
        int r;
        try {
            r = read(box);
        } catch (NullPointerException e) {
            if (@HIT(a)@) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
"""


@task('cce-catch')
def _():
    """IMPORT
// The assert is in a catch (ClassCastException).
public class Main {
    static class A {
    }

    static class B extends A {
        int v = 3;
    }

    public static void main(String[] args) {
AB        A o = b == 7 ? new A() : new B();
        int r;
        try {
            B bb = (B) o;
            r = bb.v;
            if (r > 2) r--;
        } catch (ClassCastException e) {
            if (@HIT(a)@) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
"""


# --- static initializers -----------------------------------------------------------------------

CLINIT = '''    static int input;

    static class Late {
        static int K;

        static {
            if (@HIT(Main.input)@) assert false;
            K = 1;
        }

        static int get() {
            return K;
        }
    }
'''


@task('clinit-static-call')
def _():
    """IMPORT
// The assert is in the static initializer of a class first used (static call) on an
// input-dependent path.
public class Main {
CLINIT
    public static void main(String[] args) {
AB        input = a;
        int r = 0;
        if (b == 7) {
            r = Late.get();
        }
        if (r > 3) r--;
    }
}
"""


@task('clinit-new')
def _():
    """IMPORT
// The assert is in the static initializer of a class first used (new) on an input-dependent path.
public class Main {
CLINIT
    public static void main(String[] args) {
AB        input = a;
        Object o = null;
        if (b == 7) {
            o = new Late();
        }
        if (o != null) input++;
    }
}
"""


@task('clinit-field')
def _():
    """IMPORT
// The assert is in the static initializer of a class first used (static field read) on an
// input-dependent path.
public class Main {
CLINIT
    public static void main(String[] args) {
AB        input = a;
        int r = 0;
        if (b == 7) {
            r = Late.K;
        }
        if (r > 3) r--;
    }
}
"""


@task('clinit-in-callee')
def _():
    """IMPORT
// The assert is in a static initializer triggered inside a callee that has no branches of its own.
public class Main {
CLINIT
    static int touch() {
        return Late.get() + 1;
    }

    public static void main(String[] args) {
AB        input = a;
        int r = 0;
        if (b == 7) {
            r = touch();
        }
        if (r > 3) r--;
    }
}
"""


# --- switch on String and enum -----------------------------------------------------------------

@task('string-switch')
def _():
    """IMPORT
// A switch on a String (hashCode lookupswitch + equals branches) selects the asserting case.
public class Main {
    public static void main(String[] args) {
AB        String cmd = b == 1 ? "start" : b == 7 ? "stop" : "status";
        int r;
        switch (cmd) {
            case "start": r = 1; break;
            case "stop":
                if (@HIT(a)@) assert false;
                r = 2;
                break;
            case "status": r = 3; break;
            default: r = 0;
        }
        if (r > 2) r--;
    }
}
"""


@task('string-switch-nondet', witness=[('string', 'stop'), ('int', 1001)])
def _():
    """IMPORT
// A switch on a nondet String selects the asserting case.
public class Main {
    public static void main(String[] args) {
        String cmd = Verifier.nondetString();
        int a = Verifier.nondetInt();
        int r;
        switch (cmd) {
            case "start": r = 1; break;
            case "stop":
                if (@HIT(a)@) assert false;
                r = 2;
                break;
            case "status": r = 3; break;
            default: r = 0;
        }
        if (r > 2) r--;
    }
}
"""


@task('enum-switch')
def _():
    """IMPORT
// A switch on an enum (synthetic $SwitchMap class with its own <clinit>) selects the asserting case.
public class Main {
    enum Op { START, STOP, STATUS }

    public static void main(String[] args) {
AB        Op op = b == 1 ? Op.START : b == 7 ? Op.STOP : Op.STATUS;
        int r;
        switch (op) {
            case START: r = 1; break;
            case STOP:
                if (@HIT(a)@) assert false;
                r = 2;
                break;
            default: r = 3;
        }
        if (r > 2) r--;
    }
}
"""


# --- try/finally -------------------------------------------------------------------------------

@task('finally-exits')
def _():
    """IMPORT
// try/finally with return and throw inside try: javac copies the finally block (with its branch)
// onto each exit path. The assert is behind the throwing exit.
public class Main {
    static int cnt;

    static int f(int a, int b) {
        try {
            if (b == 8) return 1;
            if (b == 7) throw new IllegalStateException();
            return 0;
        } finally {
            cnt++;
            if (a > 50) cnt++;
        }
    }

    public static void main(String[] args) {
AB        int r;
        try {
            r = f(a, b);
        } catch (IllegalStateException e) {
            r = 2;
        }
        if (r == 2) {
            if (@HIT(a)@) assert false;
        }
    }
}
"""


@task('finally-assert')
def _():
    """IMPORT
// The assert is inside a finally block, which javac copies onto every exit path of try.
public class Main {
    static int cnt;

    static void g(int a, int b) {
        try {
            if (b == 7) return;
            cnt++;
            if (b == 8) throw new IllegalStateException();
        } finally {
            if (b == 7) {
                if (@HIT(a)@) assert false;
            }
        }
    }

    public static void main(String[] args) {
AB        try {
            g(a, b);
        } catch (IllegalStateException e) {
            cnt--;
        }
    }
}
"""


# --- reference comparisons ---------------------------------------------------------------------

@task('ref-eq')
def _():
    """IMPORT
// Object == before the assert (rewritten to UtilInstrumented.refEquals, which adds branch events).
public class Main {
    public static void main(String[] args) {
AB        Object o1 = new Object();
        Object o2 = b == 7 ? o1 : new Object();
        if (o1 == o2) {
            if (@HIT(a)@) assert false;
        }
    }
}
"""


@task('ref-ne-chain')
def _():
    """IMPORT
// Several object != comparisons, then branches, then the assert.
public class Main {
    public static void main(String[] args) {
AB        Object o1 = new Object();
        Object o2 = b == 7 ? o1 : new Object();
        Object o3 = new Object();
        int r = 0;
        if (o1 != o3) r++;
        if (o2 != o3) r++;
        if (a > 5) r++;
        if (o1 != o2) return;
        if (r == 3) {
            if (@HIT(a)@) assert false;
        }
    }
}
"""


# --- lambdas and streams -----------------------------------------------------------------------

@task('lambda-predicate')
def _():
    """import java.util.function.IntPredicate;
IMPORT
// The assert condition is evaluated by a lambda (invokedynamic) created in main.
public class Main {
    public static void main(String[] args) {
AB        IntPredicate p = x -> @HIT(x)@;
        if (b == 7) {
            if (p.test(a)) assert false;
        }
    }
}
"""


@task('lambda-consumer')
def _():
    """import java.util.function.IntConsumer;
IMPORT
// The assert is inside a lambda created in main and called on an input-dependent path.
public class Main {
    public static void main(String[] args) {
AB        IntConsumer check = x -> {
            if (@HIT(x)@) assert false;
        };
        if (b == 7) check.accept(a);
        if (a > 3) a--;
    }
}
"""


@task('lambda-static-field')
def _():
    """import java.util.function.IntConsumer;
IMPORT
// The assert is inside a lambda stored in a static field (created in <clinit>) and called on an
// input-dependent path.
public class Main {
    static final IntConsumer CHECK = x -> {
        if (@HIT(x)@) assert false;
    };

    public static void main(String[] args) {
AB        if (b == 7) CHECK.accept(a);
        if (a > 3) a--;
    }
}
"""


@task('stream-foreach')
def _():
    """import java.util.stream.IntStream;
IMPORT
// The asserting method is called back from JDK stream code through a method reference.
public class Main {
    static void check(int x) {
        if (@HIT(x)@) assert false;
    }

    public static void main(String[] args) {
AB        if (b == 7) {
            IntStream.of(a).forEach(Main::check);
        }
        if (a > 3) a--;
    }
}
"""


# --- reflection --------------------------------------------------------------------------------

@task('reflection-invoke')
def _():
    """import java.lang.reflect.Method;
IMPORT
// The asserting method is called through Method.invoke on an input-dependent path.
public class Main {
    public static void check(int x) {
        if (@HIT(x)@) assert false;
    }

    public static void main(String[] args) throws Exception {
AB        Method m = Main.class.getMethod("check", int.class);
        if (b == 7) {
            m.invoke(null, a);
        }
        if (a > 3) a--;
    }
}
"""


@task('reflection-desync', witness=ints([1001]))
def _():
    """import java.lang.reflect.Method;
IMPORT
// Method.invoke runs a method with branches unconditionally; main has a branch of its own after
// the call, which the walk may pair with the callee's branches.
public class Main {
    static int cnt;

    public static void check(int x) {
        if (@HIT(x)@) assert false;
    }

    public static void main(String[] args) throws Exception {
        int a = Verifier.nondetInt();
        Method m = Main.class.getMethod("check", int.class);
        m.invoke(null, a);
        if (a > 3) cnt++;
        if (a > 5) cnt++;
    }
}
"""


# --- recursion ---------------------------------------------------------------------------------

@task('recursion-depth', witness=ints([1001, 4]))
def _():
    """IMPORT
// The assert is at an input-dependent recursion depth (b = 4).
public class Main {
    static void rec(int depth, int a, int b) {
        if (depth == b && depth > 3) {
            if (@HIT(a)@) assert false;
            return;
        }
        if (depth < 6) rec(depth + 1, a, b);
    }

    public static void main(String[] args) {
AB        rec(0, a, b);
    }
}
"""


@task('recursion-mutual', witness=ints([1001, 5]))
def _():
    """IMPORT
// Mutual recursion; the assert is only reachable at an odd depth b > 3 (b = 5).
public class Main {
    static void even(int depth, int a, int b) {
        if (depth < 8) odd(depth + 1, a, b);
    }

    static void odd(int depth, int a, int b) {
        if (depth == b && depth > 3) {
            if (@HIT(a)@) assert false;
            return;
        }
        if (depth < 8) even(depth + 1, a, b);
    }

    public static void main(String[] args) {
AB        even(0, a, b);
    }
}
"""


# --- unusual bytecode: default methods, anonymous and inner classes, synchronized ---------------

@task('default-method')
def _():
    """IMPORT
// The assert is in an interface default method, called on a class that does not override it.
public class Main {
    interface Checker {
        default void check(int a) {
            if (@HIT(a)@) assert false;
        }
    }

    static class Impl implements Checker {
    }

    static class Other implements Checker {
        int cnt;

        public void check(int a) {
            cnt++;
        }
    }

    public static void main(String[] args) {
AB        if (b == 7) {
            new Impl().check(a);
        } else {
            new Other().check(a);
        }
        if (a > 3) a--;
    }
}
"""


@task('anonymous-class')
def _():
    """IMPORT
// The assert is in an anonymous class's run(), called on an input-dependent path.
public class Main {
    static int input;

    public static void main(String[] args) {
AB        input = a;
        Runnable r = new Runnable() {
            public void run() {
                if (@HIT(input)@) assert false;
            }
        };
        if (b == 7) r.run();
        if (a > 3) a--;
    }
}
"""


@task('anonymous-static-field')
def _():
    """IMPORT
// The assert is in an anonymous class stored in a static field (receiver read from a field).
public class Main {
    static int input;

    static final Runnable CHECK = new Runnable() {
        public void run() {
            if (@HIT(input)@) assert false;
        }
    };

    public static void main(String[] args) {
AB        input = a;
        if (b == 7) CHECK.run();
        if (a > 3) a--;
    }
}
"""


@task('inner-accessor')
def _():
    """IMPORT
// An inner class reads a private field and calls a private method of its outer class through
// javac's synthetic access$ methods (Java 8 target).
public class Main {
    private int limit = 100000;

    private boolean inRange(int a) {
        return a > 1000;
    }

    class Inner {
        void check(int a) {
            if (inRange(a) && a < limit) {
                if (@HIT(a)@) assert false;
            }
        }
    }

    public static void main(String[] args) {
AB        Main m = new Main();
        Main.Inner in = m.new Inner();
        if (b == 7) in.check(a);
        if (a > 3) a--;
    }
}
"""


@task('synchronized-block')
def _():
    """IMPORT
// The assert is inside a synchronized block (monitorexit exception handler) and a synchronized
// method.
public class Main {
    static final Object LOCK = new Object();
    static int cnt;

    static synchronized void bump(int a) {
        if (a > 3) cnt++;
    }

    public static void main(String[] args) {
AB        bump(a);
        synchronized (LOCK) {
            if (b == 7) {
                if (@HIT(a)@) assert false;
            }
        }
        if (a > 5) cnt++;
    }
}
"""


@task('synchronized-throw')
def _():
    """IMPORT
// An ArithmeticException leaves a synchronized block (through its catch-any handler, which
// rethrows) and is caught outside, where the assert is.
public class Main {
    static final Object LOCK = new Object();

    public static void main(String[] args) {
AB        int r;
        try {
            synchronized (LOCK) {
                r = 100 / (b - 7);
                if (r > 3) r--;
            }
        } catch (ArithmeticException e) {
            if (@HIT(a)@) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
"""


# --- narrowing tasks (added after the first runs) ------------------------------------------------
# The lambda and reflection pairs above lose the symbolic argument in the JDK frames between the
# call and the callee, so SWAT misses the violation even without pruning. These variants pass the
# input through a static field instead, which keeps it symbolic, to separate that executor
# limitation from what pruning does.

@task('lambda-static-field-noarg')
def _():
    """IMPORT
// The assert is inside a no-argument lambda stored in a static field; it reads the input from a
// static field, so no argument passes through JDK frames.
public class Main {
    static int input;

    static final Runnable CHECK = () -> {
        if (@HIT(input)@) assert false;
    };

    public static void main(String[] args) {
AB        input = a;
        if (b == 7) CHECK.run();
        if (a > 3) a--;
    }
}
"""


@task('lambda-local-noarg')
def _():
    """IMPORT
// As lambda-static-field-noarg, but the lambda is created in main (invokedynamic in the graph).
public class Main {
    static int input;

    public static void main(String[] args) {
AB        input = a;
        Runnable check = () -> {
            if (@HIT(input)@) assert false;
        };
        if (b == 7) check.run();
        if (a > 3) a--;
    }
}
"""


@task('reflection-noarg')
def _():
    """import java.lang.reflect.Method;
IMPORT
// The asserting no-argument method is called through Method.invoke; it reads the input from a
// static field.
public class Main {
    static int input;

    public static void check() {
        if (@HIT(input)@) assert false;
    }

    public static void main(String[] args) throws Exception {
AB        input = a;
        Method m = Main.class.getMethod("check");
        if (b == 7) {
            m.invoke(null);
        }
        if (a > 3) a--;
    }
}
"""


# Exceptions thrown where the graph has no edge for them, caught by the application.

@task('library-throw-catch')
def _():
    """IMPORT
// A JDK method (Integer.parseInt) throws, and the application catches it: the usual way CLI tools
// handle a malformed argument. The assert is in the handler.
public class Main {
    public static void main(String[] args) {
AB        String arg = b == 7 ? "x7" : "12";
        int v;
        try {
            v = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            if (@HIT(a)@) assert false;
            v = 0;
        }
        if (v > 5) v--;
    }
}
"""


@task('cce-catch-up1')
def _():
    """IMPORT
// The assert is in a catch (ClassCastException) one frame above the cast.
public class Main {
    static class A {
    }

    static class B extends A {
        int v = 3;
    }

    static int unwrap(A o) {
        B bb = (B) o;
        int r = bb.v;
        if (r > 2) r--;
        return r;
    }

    public static void main(String[] args) {
AB        A o = b == 7 ? new A() : new B();
        int r;
        try {
            r = unwrap(o);
        } catch (ClassCastException e) {
            if (@HIT(a)@) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
"""


@task('explicit-throw-generic-catch')
def _():
    """IMPORT
// Control: an explicit throw of IllegalStateException one frame down, caught as RuntimeException.
// The graph models explicit throws, so this one must work.
public class Main {
    static int check(int b) {
        if (b == 7) throw new IllegalStateException();
        return b;
    }

    public static void main(String[] args) {
AB        int r;
        try {
            r = check(b);
        } catch (RuntimeException e) {
            if (@HIT(a)@) assert false;
            r = 0;
        }
        if (r > 50) r--;
    }
}
"""


@task('clinit-unconditional')
def _():
    """IMPORT
// Control for the clinit pairs: the class is first used on every path, so its <clinit> runs in
// the first round, inside the masked part of the walk. Only the input-dependent first use fails.
public class Main {
CLINIT
    public static void main(String[] args) {
AB        input = a;
        int r = Late.get();
        if (b == 7) r++;
        if (r > 3) r--;
    }
}
"""


# Executor limitations that give false SAFE verdicts without pruning too (controls: both
# configurations must agree on them).

@task('exec-system-exit')
def _():
    """IMPORT
// The first run (b = 0) ends in System.exit before the assert.
public class Main {
    public static void main(String[] args) {
AB        if (b != 7) {
            System.exit(0);
        }
        if (@HIT(a)@) assert false;
    }
}
"""


@task('exec-system-exit-late')
def _():
    """IMPORT
// As exec-system-exit, but a branch on the input is recorded before the exit.
public class Main {
    static int cnt;

    public static void main(String[] args) {
AB        if (a > 3) cnt++;
        if (b != 7) {
            System.exit(0);
        }
        if (@HIT(a)@) assert false;
    }
}
"""


@task('exec-object-array-index', witness=ints([1001, 2]))
def _():
    """IMPORT
// An object read from an array at an input-dependent index decides whether the assert is reached.
public class Main {
    static class C {
        final int k;

        C(int k) {
            this.k = k;
        }
    }

    static final C[] TABLE = {new C(0), new C(0), new C(7)};

    public static void main(String[] args) {
AB        if (b >= 0 && b < TABLE.length) {
            if (TABLE[b].k == 7) {
                if (@HIT(a)@) assert false;
            }
        }
    }
}
"""


@task('exec-int-array-index', witness=ints([1001, 2]))
def _():
    """IMPORT
// Control for exec-object-array-index: the same with an int array.
public class Main {
    static final int[] TABLE = {0, 0, 7};

    public static void main(String[] args) {
AB        if (b >= 0 && b < TABLE.length) {
            if (TABLE[b] == 7) {
                if (@HIT(a)@) assert false;
            }
        }
    }
}
"""


@task('exec-string-char', witness=[('string', '-'), ('int', 1001)])
def _():
    """IMPORT
// Z3 may solve s.charAt(0) == '-' with a supplementary code point (e.g. U+2002D): the executor
// encodes charAt as the code point truncated to 16 bits, and String inputs have no range bound,
// so the next run diverges.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        int a = Verifier.nondetInt();
        if (s.length() == 1 && s.charAt(0) == '-') {
            if (@HIT(a)@) assert false;
        }
    }
}
"""

@task('exec-stringbuilder-setlength', witness=[('string', 'ab'), ('int', 1001)])
def _():
    """IMPORT
// A StringBuilder is reused with setLength(0), as parsers do between fields.
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        int a = Verifier.nondetInt();
        StringBuilder sb = new StringBuilder();
        sb.append(s);
        sb.setLength(0);
        sb.append('x');
        if (sb.length() == 1 && s.length() == 2) {
            if (@HIT(a)@) assert false;
        }
    }
}
"""


def render(src: str, safe: bool) -> str:
    src = src.replace('IMPORT\n', IMPORT).replace('CLINIT\n', CLINIT)
    src = re.sub(r'^AB', AB, src, flags=re.M)
    hi = '1001' if safe else '100000'
    return re.sub(r'@HIT\((.*?)\)@', lambda m: f'{m.group(1)} > 1000 && {m.group(1)} < {hi}', src)


def all_soundness_tasks():
    for name, (src, witness) in TASKS.items():
        for safe in (True, False):
            yield Task('sa-soundness', name, safe, render(src, safe), witness, SAFE_REASON)
