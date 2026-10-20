"""Parameterised task families that are expected to benefit from SA pruning, plus a control.

Each family is a function (variant, n, safe) -> Task. All families share TARGET: the only
assertion, reached through three input-dependent branches. The VIOLATION and SAFE variants differ
in one comparison only, so both have the same branch structure and the same SA graph shape.
"""

from task import Task, ints

# The VIOLATION variant fails for a = 1001, b = 2996. In the SAFE variant b = 3a - 7 and b < a
# would need a < 3.5, which contradicts a > 1000; a < 100000 rules out int overflow in 3 * a.
TARGET_WITNESS = [1001, 2996]


def target(safe: bool, indent: str = '    ', static: bool = True, name: str = 'target') -> str:
    op = '<' if safe else '>'
    return f'''{indent}// The only assertion in the program.
{indent}{'static ' if static else ''}void {name}(int a, int b) {{
{indent}    if (a > 1000 && a < 100000) {{
{indent}        if (b == 3 * a - 7) {{
{indent}            if (b {op} a) {{
{indent}                assert false;
{indent}            }}
{indent}        }}
{indent}    }}
{indent}}}
'''


SAFE_REASON = ('target() needs a > 1000, b == 3 * a - 7 and b < a, i.e. a < 3.5: unsatisfiable. '
               'a < 100000 rules out overflow in 3 * a.')


def handler(i: int, k: int, static: bool = True, name: str = None, indent: str = '    ') -> str:
    """A method with k independent input-dependent branches (2^k feasible paths), no assert."""
    conds = [f'a > {i}', f'b > {2 * i}', f'c < {-i}', f'd == {i + 5}'][:k]
    body = ''.join(f'{indent}    if ({cond}) r += {1 << j};\n' for j, cond in enumerate(conds))
    return f'''{indent}{'static ' if static else ''}int {name or f'handler{i}'}(int a, int b, int c, int d) {{
{indent}    int r = {i};
{body}{indent}    return r;
{indent}}}
'''


HEADER = '''import org.sosy_lab.sv_benchmarks.Verifier;

'''


def read_inputs(names, indent='        '):
    return ''.join(f'{indent}int {v} = Verifier.nondetInt();\n' for v in names)


# --- dispatcher ------------------------------------------------------------------------------
# Input `cmd` picks one of n commands. Commands 0 .. n-2 run a handler with 4 independent
# branches (16 paths each); command n-1, the last one, runs target(). Unpruned DFS explores
# ~16 (n - 1) paths, pruned DFS only the dispatch and target().

DISPATCHER_VARIANTS = ['switch', 'ifchain', 'instance', 'loop']


def dispatcher(variant: str, n: int, safe: bool) -> Task:
    k = 4
    inputs = ['cmd', 'a', 'b', 'c', 'd']
    witness = [n - 1] + TARGET_WITNESS + [0, 0]
    static = variant != 'instance'
    recv = 'm.' if variant == 'instance' else ''
    handlers = ''.join(handler(i, k, static=static) for i in range(n - 1))

    if variant in ('switch', 'instance', 'loop'):
        cases = ''.join(f'            case {i}: r = {recv}handler{i}(a, b, c, d); break;\n' for i in range(n - 1))
        dispatch = f'''        int r;
        switch (cmd) {{
{cases}            case {n - 1}: {recv}target(a, b); r = 0; break;
            default: r = -1;
        }}
        return r;
'''
    elif variant == 'ifchain':
        chain = ''.join(f'        {"else " if i else ""}if (cmd == {i}) r = handler{i}(a, b, c, d);\n' for i in range(n - 1))
        dispatch = f'''        int r = -1;
{chain}        {"else " if n > 1 else ""}if (cmd == {n - 1}) {{ target(a, b); r = 0; }}
        return r;
'''
    else:
        raise ValueError(variant)

    if variant == 'instance':
        main = f'''    int calls;

    public static void main(String[] args) {{
{read_inputs(inputs)}        Main m = new Main();
        m.calls = dispatch(m, cmd, a, b, c, d);
    }}

    static int dispatch(Main m, int cmd, int a, int b, int c, int d) {{
{dispatch}    }}
'''
    elif variant == 'loop':
        # Two rounds of dispatch: a handler in round 0 can still reach target() in round 1, so
        # only round 1 can be pruned (the event-loop shape, as in MinePump).
        main = f'''    public static void main(String[] args) {{
        for (int round = 0; round < 2; round++) {{
{read_inputs(inputs, indent='            ')}            dispatch(cmd, a, b, c, d);
        }}
    }}

    static int dispatch(int cmd, int a, int b, int c, int d) {{
{dispatch}    }}
'''
    else:
        main = f'''    public static void main(String[] args) {{
{read_inputs(inputs)}        dispatch(cmd, a, b, c, d);
    }}

    static int dispatch(int cmd, int a, int b, int c, int d) {{
{dispatch}    }}
'''
    src = f'''{HEADER}// dispatcher/{variant}: cmd picks one of {n} commands, only the last one (cmd == {n - 1}) can reach the assert.
public class Main {{
{main}
{handlers}
{target(safe, static=static)}}}
'''
    return Task('dispatcher', f'{variant}_n{n:02d}', safe, src, ints(witness), SAFE_REASON)


# --- validation-gauntlet ---------------------------------------------------------------------
# n checks, each rejecting its own input v_i and leaving early, then target(). The early-exit
# sides lead away from the assert.
#   return: `if (v_i < i) return;`                     a leaf: pruning saves one round per check
#   report: `if (v_i < i) { report(i, v_i); return; }` report() has 3 branches on v_i
#   throw:  `if (v_i < i) throw new IllegalArgumentException();`, caught in main, which returns
#   exit:   `if (v_i < i) { report(i, v_i); System.exit(1); }`  the graph does not know exit()
#           ends the program, so it sees target() as reachable: no gain expected

GAUNTLET_VARIANTS = ['return', 'report', 'throw', 'exit']

REPORT = '''    static int errors;

    // Classifies a rejected value; three branches, no assert.
    static void report(int check, int v) {
        if (v < -1000) errors += 100;
        if (v < -10) errors += 10;
        if (v == check - 1) errors += 1;
    }
'''


def gauntlet(variant: str, n: int, safe: bool) -> Task:
    vs = [f'v{i}' for i in range(n)]
    inputs = ['a', 'b'] + vs
    witness = TARGET_WITNESS + [i for i in range(n)]  # v_i = i passes check i
    reject = {
        'return': 'return;',
        'report': '{{ report({i}, v{i}); return; }}',
        'throw': 'throw new IllegalArgumentException();',
        'exit': '{{ report({i}, v{i}); System.exit(1); }}',
    }[variant]
    checks = ''.join(f'        if (v{i} < {i}) ' + reject.format(i=i) + '\n' for i in range(n))
    params = ', '.join(f'int {v}' for v in inputs)
    args = ', '.join(inputs)
    if variant == 'throw':
        call = f'''        try {{
            run({args});
        }} catch (IllegalArgumentException e) {{
            return;
        }}
'''
    else:
        call = f'        run({args});\n'
    src = f'''{HEADER}// validation-gauntlet/{variant}: {n} checks that each leave early, then the only assert.
public class Main {{
    public static void main(String[] args) {{
{read_inputs(inputs)}{call}    }}

    static void run({params}) {{
{checks}        stage1(a, b);
    }}

    static void stage1(int a, int b) {{
        stage2(a + 1, b);
    }}

    static void stage2(int a, int b) {{
        target(a - 1, b);
    }}

{REPORT if variant in ('report', 'exit') else ''}
{target(safe)}}}
'''
    return Task('validation-gauntlet', f'{variant}_n{n:02d}', safe, src, ints(witness), SAFE_REASON)


# --- error-handling --------------------------------------------------------------------------
# n steps, each in its own try/catch in main. A step fails on a bad input; its catch block logs
# (three branches) and returns. The assert is on the success path after all steps.
#   explicit: the step throws IllegalArgumentException itself (one frame below the catch)
#   deep:     the throw is two frames below the catch
#   arith:    implicit ArithmeticException from `100 / (x - i)`
#   bounds:   implicit ArrayIndexOutOfBoundsException from `TABLE[x]`

ERROR_VARIANTS = ['explicit', 'deep', 'arith', 'bounds']


def error_handling(variant: str, n: int, safe: bool) -> Task:
    xs = [f'x{i}' for i in range(n)]
    inputs = ['a', 'b'] + xs
    exc = {'explicit': 'IllegalArgumentException', 'deep': 'IllegalArgumentException',
           'arith': 'ArithmeticException', 'bounds': 'ArrayIndexOutOfBoundsException'}[variant]
    if variant == 'explicit':
        step = '''    static int step{i}(int x) {{
        if (x < {lo}) throw new IllegalArgumentException();
        return x + {i};
    }}
'''
        ok = lambda i: 0
    elif variant == 'deep':
        step = '''    static int step{i}(int x) {{
        return check{i}(x) + {i};
    }}

    static int check{i}(int x) {{
        if (x < {lo}) throw new IllegalArgumentException();
        return x;
    }}
'''
        ok = lambda i: 0
    elif variant == 'arith':
        # Fails for x == i + 1 only; x = 0 passes every step (divisor -i - 1 != 0).
        step = '''    static int step{i}(int x) {{
        return 100 / (x - {i} - 1);
    }}
'''
        ok = lambda i: 0
    elif variant == 'bounds':
        step = '''    static int step{i}(int x) {{
        return TABLE[x] + {i};
    }}
'''
        ok = lambda i: 0
    steps = ''.join(step.format(i=i, lo=-(i + 1)) for i in range(n))
    tries = ''.join(f'''        try {{
            acc += step{i}(x{i});
        }} catch ({exc} e) {{
            log({i}, x{i});
            return;
        }}
''' for i in range(n))
    witness = TARGET_WITNESS + [ok(i) for i in range(n)]
    table = '    static final int[] TABLE = {3, 1, 4, 1};\n\n' if variant == 'bounds' else ''
    src = f'''{HEADER}// error-handling/{variant}: {n} steps that can fail with {exc}; each catch block logs and returns.
public class Main {{
{table}    static int logged;

    public static void main(String[] args) {{
{read_inputs(inputs)}        int acc = 0;
{tries}        target(a, b);
    }}

{steps}
    // Logs a failed step; three branches, no assert.
    static void log(int step, int x) {{
        if (x < -1000) logged += 100;
        if (x > 1000) logged += 10;
        if (x == step) logged += 1;
    }}

{target(safe)}}}
'''
    return Task('error-handling', f'{variant}_n{n:02d}', safe, src, ints(witness), SAFE_REASON)


# --- dead-callees ----------------------------------------------------------------------------
# n calls to helpers with two branches each (3 paths), on paths that cannot reach the assert.
#   suffix:  the calls come after target(); unpruned DFS explores 3^n paths behind it
#   guarded: the calls are made, and main returns, if `mode == 1`; target() comes after
#   shared:  as guarded, but all calls go to one helper that the live path also calls before
#            target(): only a context-sensitive walk can prune the dead calls

DEAD_VARIANTS = ['suffix', 'guarded', 'shared']


def dead_callees(variant: str, n: int, safe: bool) -> Task:
    cs = [f'c{i}' for i in range(n)]
    inputs = ['mode', 'p', 'a', 'b'] + cs
    witness = [0, 2] + TARGET_WITNESS + [0] * n
    if variant == 'shared':
        helpers = '''    // Two branches, three paths.
    static int classify(int x) {
        if (x < 0) return 0;
        if (x < 100) return 1;
        return 2;
    }
'''
        calls = ''.join(f'            stat += classify(c{i});\n' for i in range(n))
    else:
        helpers = ''.join(f'''    static int helper{i}(int x) {{
        if (x < {i}) return 0;
        if (x < {100 + i}) return 1;
        return 2;
    }}

''' for i in range(n))
        calls = ''.join(f'            stat += helper{i}(c{i});\n' for i in range(n))
    if variant == 'suffix':
        body = f'''        target(a, b);
        int stat = 0;
{calls.replace('            ', '        ')}        statistics = stat;
'''
    elif variant == 'guarded':
        body = f'''        if (mode == 1) {{
            int stat = 0;
{calls}            statistics = stat;
            return;
        }}
        target(a, b);
'''
    else:
        body = f'''        if (mode == 1) {{
            int stat = 0;
{calls}            statistics = stat;
            return;
        }}
        if (classify(p) == 2) {{
            target(a, b);
        }}
'''
        witness = [0, 100] + TARGET_WITNESS + [0] * n
    src = f'''{HEADER}// dead-callees/{variant}: {n} helper calls that cannot reach the assert.
public class Main {{
    static int statistics;

    public static void main(String[] args) {{
{read_inputs(inputs)}{body}    }}

{helpers}
{target(safe)}}}
'''
    return Task('dead-callees', f'{variant}_n{n:02d}', safe, src, ints(witness), SAFE_REASON)


# --- virtual-dispatch ------------------------------------------------------------------------
# cmd selects one of n subclasses of Cmd; only the last one's run() calls target().
#   local:   `case i: new Ci().run(...)`: the receiver type is exact, so the call resolves
#   factory: `make(cmd).run(...)`: make() can return any Ci, so the call is ambiguous and the
#            graph is incomplete there; no gain expected, but the verdict must stay right
#   array:   `TABLE[cmd].run(...)`: a receiver read from an array is untraceable; likewise

VIRTUAL_VARIANTS = ['local', 'factory', 'array']


def virtual_dispatch(variant: str, n: int, safe: bool) -> Task:
    k = 4
    inputs = ['cmd', 'a', 'b', 'c', 'd']
    witness = [n - 1] + TARGET_WITNESS + [0, 0]
    classes = ''.join(f'''    static class C{i} extends Cmd {{
{handler(i, k, static=False, name='run', indent='        ')}    }}

''' for i in range(n - 1))
    classes += f'''    static class C{n - 1} extends Cmd {{
        int run(int a, int b, int c, int d) {{
            target(a, b);
            return 0;
        }}
    }}
'''
    if variant == 'local':
        cases = ''.join(f'            case {i}: return new C{i}().run(a, b, c, d);\n' for i in range(n))
        dispatch = f'''        switch (cmd) {{
{cases}            default: return -1;
        }}
'''
        extra = ''
    elif variant == 'factory':
        cases = ''.join(f'            case {i}: return new C{i}();\n' for i in range(n))
        dispatch = '''        Cmd command = make(cmd);
        if (command == null) return -1;
        return command.run(a, b, c, d);
'''
        extra = f'''    static Cmd make(int cmd) {{
        switch (cmd) {{
{cases}            default: return null;
        }}
    }}

'''
    elif variant == 'array':
        dispatch = '''        if (cmd < 0 || cmd >= TABLE.length) return -1;
        return TABLE[cmd].run(a, b, c, d);
'''
        extra = '    static final Cmd[] TABLE = {' + ', '.join(f'new C{i}()' for i in range(n)) + '};\n\n'
    src = f'''{HEADER}// virtual-dispatch/{variant}: cmd selects one of {n} subclasses, only C{n - 1}.run() reaches the assert.
public class Main {{
    static abstract class Cmd {{
        abstract int run(int a, int b, int c, int d);
    }}

{classes}
{extra}    public static void main(String[] args) {{
{read_inputs(inputs)}        dispatch(cmd, a, b, c, d);
    }}

    static int dispatch(int cmd, int a, int b, int c, int d) {{
{dispatch}    }}

{target(safe)}}}
'''
    return Task('virtual-dispatch', f'{variant}_n{n:02d}', safe, src, ints(witness), SAFE_REASON)


# --- irrelevant-prefix (control) -------------------------------------------------------------
# n independent branches on their own inputs before target(). Every path reaches the assert,
# so reachability pruning cannot remove anything: 2^n paths either way.

PREFIX_VARIANTS = ['count']


def irrelevant_prefix(variant: str, n: int, safe: bool) -> Task:
    xs = [f'x{i}' for i in range(n)]
    inputs = ['a', 'b'] + xs
    witness = TARGET_WITNESS + [0] * n
    branches = ''.join(f'        if (x{i} > 0) count++;\n' for i in range(n))
    src = f'''{HEADER}// irrelevant-prefix/{variant}: {n} independent branches before the only assert (control: nothing to prune).
public class Main {{
    static int count;

    public static void main(String[] args) {{
{read_inputs(inputs)}{branches}        target(a, b);
    }}

{target(safe)}}}
'''
    return Task('irrelevant-prefix', f'{variant}_n{n:02d}', safe, src, ints(witness), SAFE_REASON)


# --- exception-dispatch ----------------------------------------------------------------------
# Measures what SA loses after an exception it does not model. The command is parsed from input
# t; for t < 0 parsing throws and the handler derives the command from t as well, so both paths
# lead into the same dispatcher as in dispatcher/switch (only the last command reaches the
# assert).
#   npe:      a NullPointerException (no SWAT guard: after it the walk gives up for the path, so
#             the dead handlers behind the exception path are not pruned)
#   explicit: control, an explicit IllegalArgumentException, which the graph models precisely

EXCEPTION_DISPATCH_VARIANTS = ['npe', 'explicit']


def exception_dispatch(variant: str, n: int, safe: bool) -> Task:
    k = 4
    inputs = ['t', 'a', 'b', 'c', 'd']
    witness = [-n] + TARGET_WITNESS + [0, 0]  # t = -n: the exception path, then cmd = n - 1
    if variant == 'npe':
        parse = '''    static int parse(int t) {
        Holder h = t < 0 ? null : HOLDER;
        return h.base + t;
    }
'''
        caught = 'NullPointerException'
    else:
        parse = '''    static int parse(int t) {
        if (t < 0) throw new IllegalArgumentException();
        return HOLDER.base + t;
    }
'''
        caught = 'IllegalArgumentException'
    handlers = ''.join(handler(i, k) for i in range(n - 1))
    cases = ''.join(f'            case {i}: r = handler{i}(a, b, c, d); break;\n' for i in range(n - 1))
    src = f'''{HEADER}// exception-dispatch/{variant}: the command is parsed from t; for t < 0 parsing throws
// ({caught}) and the handler derives the command as -t - 1. Only command {n - 1} reaches the assert.
public class Main {{
    static class Holder {{
        int base = 0;
    }}

    static final Holder HOLDER = new Holder();

{parse}
    public static void main(String[] args) {{
{read_inputs(inputs)}        int cmd;
        try {{
            cmd = parse(t);
        }} catch ({caught} e) {{
            cmd = -t - 1;
        }}
        dispatch(cmd, a, b, c, d);
    }}

    static int dispatch(int cmd, int a, int b, int c, int d) {{
        int r;
        switch (cmd) {{
{cases}            case {n - 1}: target(a, b); r = 0; break;
            default: r = -1;
        }}
        return r;
    }}

{handlers}
{target(safe)}}}
'''
    return Task('exception-dispatch', f'{variant}_n{n:02d}', safe, src, ints(witness), SAFE_REASON)


FAMILIES = {
    'dispatcher': (dispatcher, DISPATCHER_VARIANTS, [2, 4, 8, 16, 32]),
    'validation-gauntlet': (gauntlet, GAUNTLET_VARIANTS, [2, 4, 8, 16, 32]),
    'error-handling': (error_handling, ERROR_VARIANTS, [2, 4, 8, 16, 32]),
    'dead-callees': (dead_callees, DEAD_VARIANTS, [1, 2, 3, 4, 5, 6]),
    'virtual-dispatch': (virtual_dispatch, VIRTUAL_VARIANTS, [2, 4, 8, 16, 32]),
    'irrelevant-prefix': (irrelevant_prefix, PREFIX_VARIANTS, [2, 4, 6, 8]),
    'exception-dispatch': (exception_dispatch, EXCEPTION_DISPATCH_VARIANTS, [2, 4, 8, 16, 32]),
}


def all_family_tasks():
    for fn, variants, ns in FAMILIES.values():
        for variant in variants:
            for n in ns:
                for safe in (True, False):
                    yield fn(variant, n, safe)
