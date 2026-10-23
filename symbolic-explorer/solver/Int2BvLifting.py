"""
Rewrites bit-vector comparisons over int_to_bv of bounded Integer terms into Integer comparisons.

The executor encodes Java ints, chars, ... as bit-vectors, and String lengths, indices and char
codes come from the string theory as Integers, so a query such as `s.length() > 2` reads
`(bvsgt ((_ int_to_bv 32) (str.len s)) (_ bv2 32))`. Z3 decides such mixed queries badly: trivially
satisfiable ones with a few chars and lengths run for minutes. When the Integer term is known to lie
in the range of the bit-vector, int_to_bv is a bijection there and the comparison can be made over
the Integers instead, where Z3 is fast.

`lift(e, signed)` finds an Integer term t with e == int_to_bv(t) whose value lies in the signed
(or unsigned) range of e's width, built from:
- bit-vector numerals,
- int_to_bv of an Integer term with a known range (numerals, str.len, str.indexof, str.to_code,
  ubv_to_int, + - * and ite of those),
- bvadd/bvsub/bvneg/bvmul/ite of liftable terms, as long as the range still fits,
- zero/sign extension of liftable terms.
A char code (str.to_code) of an SMT string lies in [-1, 0x10FFFF], which does not fit 16 bits: it is
lifted as ite(t < 0, t + 2^16, ite(t < 2^16, t, t mod 2^16)), which is exact, but the mod makes
Z3 slow. The executor encodes a Java string with one SMT char per UTF-16 char, so in a query in
which every char comes from a Java char, a char code lies in [-1, 0xFFFF] and is lifted as
ite(t < 0, t + 2^16, t) (Int2BvLifting(java_chars=True), for queries that `java_chars_only` accepts).

`rewrite(e)` replaces every comparison (=, distinct, bvs*, bvu*) between two liftable bit-vectors that
involves int_to_bv by the Integer comparison of their lifts, and ubv_to_int of a liftable term by its
lift. Everything else is left as it is.

The ranges of str.len and str.indexof assume that every string has at most 2^31 - 1 chars, as Java
strings do, and with java_chars=True the range of str.to_code assumes that every char is a Java
char. `rewrite` records the strings whose length and the char codes whose range it relied on;
`lift_query` adds the bound for each of them to the query, so that the rewritten query is
equivalent to the original one under these bounds in every model (and its models are models of
the original query).

The char code bounds hold in every model in which each string variable is a Java string, as long as
every char that the query itself puts into a string is a Java char: a string constant, and the
argument of str.from_code (taking the range of str.to_code as [-1, 0xFFFF], which holds for the
strings below it by induction over the term). `java_chars_only` checks that; other queries are
lifted with the full range (java_chars=False).
"""

import ctypes

import z3

JAVA_MAX_STRING_LENGTH = 2 ** 31 - 1
# Upper bound of str.to_code; Z3's char sort is never larger than Unicode.
MAX_CODE_POINT = 0x10FFFF
# Upper bound of a Java char (one UTF-16 code unit).
MAX_JAVA_CHAR = 0xFFFF

_SIGNED_CMP = {z3.Z3_OP_SLEQ, z3.Z3_OP_SLT, z3.Z3_OP_SGEQ, z3.Z3_OP_SGT}
_UNSIGNED_CMP = {z3.Z3_OP_ULEQ, z3.Z3_OP_ULT, z3.Z3_OP_UGEQ, z3.Z3_OP_UGT}
_INT_CMP = {
    z3.Z3_OP_SLEQ: lambda a, b: a <= b, z3.Z3_OP_ULEQ: lambda a, b: a <= b,
    z3.Z3_OP_SLT: lambda a, b: a < b, z3.Z3_OP_ULT: lambda a, b: a < b,
    z3.Z3_OP_SGEQ: lambda a, b: a >= b, z3.Z3_OP_UGEQ: lambda a, b: a >= b,
    z3.Z3_OP_SGT: lambda a, b: a > b, z3.Z3_OP_UGT: lambda a, b: a > b,
}


def _kind(e):
    return e.decl().kind() if z3.is_app(e) else None


def _view_range(width, signed):
    return (-(2 ** (width - 1)), 2 ** (width - 1) - 1) if signed else (0, 2 ** width - 1)


class Int2BvLifting:
    """Rewrites expressions of one Z3 context; caches results across queries.

    The caches are keyed by AST id. Z3 reuses the id of an AST that has been freed, so every
    expression that serves as a key is kept alive in self._alive.
    """

    def __init__(self, java_chars=False):
        """java_chars: take the range of str.to_code as [-1, 0xFFFF] and record each char code
        that relies on it, for queries that java_chars_only accepts."""
        self.java_chars = java_chars
        self._alive = []
        self._rewritten = {}
        self._lifted = {}
        self._ranges = {}
        self._mentions_int2bv = {}
        # Strings whose length bound (and char codes whose Java char bound) a rewrite relied on,
        # by expression id
        self._bounded = {}
        self._needs = {}
        self._java_chars_only = {}

    # Integer ranges

    def int_range(self, t):
        """(lo, hi) such that lo <= t <= hi in every model (under the string length bound), or None.
        Records the strings whose length bound the range relies on in self._needs[t]."""
        key = t.get_id()
        if key in self._ranges:
            return self._ranges[key]
        self._alive.append(t)
        r, needs = self._int_range(t)
        self._ranges[key] = r
        self._needs[key] = needs if r is not None else ()
        return r

    def _needs_of(self, *terms):
        out = []
        for t in terms:
            out.extend(self._needs.get(t.get_id(), ()))
        return tuple(out)

    def _int_range(self, t):
        v = _int_const(t)
        if v is not None:
            return (v, v), ()
        k = _kind(t)
        args = t.children() if z3.is_app(t) else []
        if k == z3.Z3_OP_SEQ_LENGTH:
            return (0, JAVA_MAX_STRING_LENGTH), (args[0],)
        if k == z3.Z3_OP_SEQ_INDEX:
            return (-1, JAVA_MAX_STRING_LENGTH), (args[0],)
        if k == z3.Z3_OP_STR_TO_CODE:
            if self.java_chars:
                return (-1, MAX_JAVA_CHAR), (t,)
            return (-1, MAX_CODE_POINT), ()
        if k == z3.Z3_OP_BV2INT:
            if z3.is_bv_value(args[0]):
                return (args[0].as_long(), args[0].as_long()), ()
            return (0, 2 ** args[0].size() - 1), ()
        if k == z3.Z3_OP_SBV2INT:
            return _view_range(args[0].size(), True), ()
        if k in (z3.Z3_OP_ADD, z3.Z3_OP_SUB, z3.Z3_OP_MUL) and args:
            ranges = [self.int_range(a) for a in args]
            if any(r is None for r in ranges):
                return None, ()
            lo, hi = ranges[0]
            for (l2, h2) in ranges[1:]:
                if k == z3.Z3_OP_ADD:
                    lo, hi = lo + l2, hi + h2
                elif k == z3.Z3_OP_SUB:
                    lo, hi = lo - h2, hi - l2
                else:
                    products = [lo * l2, lo * h2, hi * l2, hi * h2]
                    lo, hi = min(products), max(products)
            return (lo, hi), self._needs_of(*args)
        if k == z3.Z3_OP_UMINUS:
            r = self.int_range(args[0])
            return (None, ()) if r is None else ((-r[1], -r[0]), self._needs_of(args[0]))
        if k == z3.Z3_OP_ITE:
            ra, rb = self.int_range(args[1]), self.int_range(args[2])
            if ra is None or rb is None:
                return None, ()
            return (min(ra[0], rb[0]), max(ra[1], rb[1])), self._needs_of(args[1], args[2])
        return None, ()

    def java_chars_only(self, e):
        """Whether every char that e puts into a string is a Java char (at most 0xFFFF): no string
        constant has a larger char, every str.from_code argument is at most 0xFFFF (by its range,
        with str.to_code in [-1, 0xFFFF]) and there is no Char-sorted term. Needs java_chars=True."""
        assert self.java_chars
        memo = self._java_chars_only
        # Iterative post-order: string concatenations can be deeper than Python's recursion limit.
        stack = [(e, False)]
        while stack:
            t, expanded = stack.pop()
            key = t.get_id()
            if key in memo:
                continue
            children = t.children() if z3.is_app(t) else []
            if not expanded and children:
                stack.append((t, True))
                stack.extend((c, False) for c in children if c.get_id() not in memo)
                continue
            self._alive.append(t)
            if t.sort().kind() == z3.Z3_CHAR_SORT:
                ok = False
            elif z3.is_string_value(t):
                ok = all(c <= MAX_JAVA_CHAR for c in _string_codes(t))
            elif z3.is_app(t):
                ok = all(memo[c.get_id()] for c in children)
                if ok and _kind(t) == z3.Z3_OP_STR_FROM_CODE:
                    r = self.int_range(t.arg(0))
                    ok = r is not None and r[1] <= MAX_JAVA_CHAR
            else:
                # quantifiers and bound variables: the executor builds none
                ok = False
            memo[key] = ok
        return memo[e.get_id()]

    # Bit-vector lifting

    def lift(self, e, signed):
        """(t, needs): an Integer term with e == int_to_bv(t) and t in the signed/unsigned range of
        e's width, and the strings whose length bound that relies on; or None."""
        key = (e.get_id(), signed)
        if key not in self._lifted:
            self._alive.append(e)
            self._lifted[key] = self._lift(e, signed)
        return self._lifted[key]

    def _lift(self, e, signed):
        width = e.size()
        lo_view, hi_view = _view_range(width, signed)
        if z3.is_bv_value(e):
            return (z3.IntVal(e.as_signed_long() if signed else e.as_long(), e.ctx), ())
        k = _kind(e)
        args = e.children() if z3.is_app(e) else []
        if k == z3.Z3_OP_INT2BV:
            t = self.rewrite(args[0])
            r = self.int_range(args[0])
            if r is None:
                return None
            needs = self._needs.get(args[0].get_id(), ()) + self.needs(args[0])
            wrapped = _wrap(t, r, lo_view, hi_view, 2 ** width)
            return None if wrapped is None else (wrapped, needs)
        if k in (z3.Z3_OP_BADD, z3.Z3_OP_BSUB, z3.Z3_OP_BMUL) and args:
            parts = [self._lift_ranged(a, signed) for a in args]
            if any(p is None for p in parts):
                return None
            t, (lo, hi), needs = parts[0]
            for (t2, (l2, h2), n2) in parts[1:]:
                if k == z3.Z3_OP_BADD:
                    t, lo, hi = t + t2, lo + l2, hi + h2
                elif k == z3.Z3_OP_BSUB:
                    t, lo, hi = t - t2, lo - h2, hi - l2
                else:
                    products = [lo * l2, lo * h2, hi * l2, hi * h2]
                    t, lo, hi = t * t2, min(products), max(products)
                needs = needs + n2
            wrapped = _wrap(t, (lo, hi), lo_view, hi_view, 2 ** width)
            return None if wrapped is None else (wrapped, needs)
        if k == z3.Z3_OP_BNEG:
            p = self._lift_ranged(args[0], signed)
            if p is None:
                return None
            t, (lo, hi), needs = p
            wrapped = _wrap(-t, (-hi, -lo), lo_view, hi_view, 2 ** width)
            return None if wrapped is None else (wrapped, needs)
        if k == z3.Z3_OP_ITE:
            a, b = self.lift(args[1], signed), self.lift(args[2], signed)
            if a is None or b is None:
                return None
            return z3.If(self.rewrite(args[0]), a[0], b[0]), a[1] + b[1] + self.needs(args[0])
        if k in (z3.Z3_OP_ZERO_EXT, z3.Z3_OP_SIGN_EXT):
            inner = self.lift(args[0], k == z3.Z3_OP_SIGN_EXT)
            if inner is None:
                return None
            # The inner value fits the inner width's range, so it fits both views of the
            # extended width, except a negative value in the unsigned view.
            if not signed and k == z3.Z3_OP_SIGN_EXT:
                return None
            return inner
        if k == z3.Z3_OP_CONCAT and len(args) == 2 and z3.is_bv_value(args[0]) and args[0].as_long() == 0:
            # A zero extension, as the executor widens a char to an int
            return self.lift(args[1], False)
        return None

    def _lift_ranged(self, e, signed):
        """(t, (lo, hi), needs) for a liftable e, with the range of its lift."""
        lifted = self.lift(e, signed)
        if lifted is None:
            return None
        t, needs = lifted
        # The lift lies in the view's range by construction; its own range may be tighter.
        lo, hi = _view_range(e.size(), signed)
        r = self.int_range(t)
        if r is not None:
            lo, hi = max(lo, r[0]), min(hi, r[1])
            needs = needs + self._needs.get(t.get_id(), ())
        return t, (lo, hi), needs

    # Rewriting

    def mentions_int2bv(self, e):
        key = e.get_id()
        if key not in self._mentions_int2bv:
            self._alive.append(e)
            self._mentions_int2bv[key] = z3.is_app(e) and (
                _kind(e) == z3.Z3_OP_INT2BV or any(self.mentions_int2bv(c) for c in e.children()))
        return self._mentions_int2bv[key]

    def rewrite(self, e):
        """e with comparisons over int_to_bv of bounded terms made over the Integers."""
        key = e.get_id()
        if key in self._rewritten:
            return self._rewritten[key]
        self._alive.append(e)
        result, needs = e, ()
        if z3.is_app(e) and self.mentions_int2bv(e):
            result, needs = self._rewrite_app(e)
        self._rewritten[key] = result
        self._bounded[key] = needs
        return result

    def needs(self, e):
        """The strings whose length bound (and char codes whose Java char bound) rewrite(e) relies on."""
        self.rewrite(e)
        return self._bounded[e.get_id()]

    def _rewrite_app(self, e):
        k = _kind(e)
        args = e.children()
        equality = k in (z3.Z3_OP_EQ, z3.Z3_OP_DISTINCT)
        if len(args) == 2 and z3.is_bv(args[0]) and (equality or k in _SIGNED_CMP or k in _UNSIGNED_CMP):
            views = [True, False] if equality else [k in _SIGNED_CMP]
            for signed in views:
                a, b = self.lift(args[0], signed), self.lift(args[1], signed)
                if a is not None and b is not None:
                    if equality:
                        cmp = (lambda x, y: x == y) if k == z3.Z3_OP_EQ else (lambda x, y: x != y)
                    else:
                        cmp = _INT_CMP[k]
                    return cmp(a[0], b[0]), a[1] + b[1]
        if k == z3.Z3_OP_BV2INT:
            a = self.lift(args[0], False)
            if a is not None:
                return a
        if k == z3.Z3_OP_SBV2INT:
            a = self.lift(args[0], True)
            if a is not None:
                return a
        if k == z3.Z3_OP_ITE:
            x = _signed_bv2int_operand(e)
            if x is not None:
                a = self.lift(x, True)
                if a is not None:
                    return a
        children = [self.rewrite(c) for c in args]
        needs = tuple(n for c in args for n in self._bounded[c.get_id()])
        if all(c.eq(o) for c, o in zip(children, args)):
            return e, needs
        return e.decl()(*children), needs


def _wrap(t, r, lo_view, hi_view, m):
    """The value of int_to_bv(t) in the view [lo_view, hi_view] (of size m), for t in r = (lo, hi):
    t itself where it lies in the view, else t shifted by m (or reduced modulo m, if the range is
    wider than that), as an ite with only the cases r allows. None if t can lie below the view by
    more than m."""
    lo, hi = r
    if lo < lo_view - m:
        return None
    result = t
    if hi > hi_view:
        high = t - m if hi <= hi_view + m else (t - lo_view) % m + lo_view
        result = z3.If(t <= hi_view, t, high)
    if lo < lo_view:
        result = z3.If(t < lo_view, t + m, result)
    return result


def _string_codes(e):
    """The char codes of a string constant."""
    n = z3.Z3_get_string_length(e.ctx_ref(), e.as_ast())
    buf = (ctypes.c_uint * n)()
    z3.Z3_get_string_contents(e.ctx_ref(), e.as_ast(), n, buf)
    return list(buf)


def _int_const(e):
    """The value of an Integer numeral, also when written as (- n); else None."""
    if z3.is_int_value(e):
        return e.as_long()
    if _kind(e) == z3.Z3_OP_UMINUS and z3.is_int_value(e.arg(0)):
        return -e.arg(0).as_long()
    return None


def _normal(e):
    """e simplified to a fixpoint (one simplify of ubv_to_int(concat ...) leaves 2^k * ubv_to_int(0))."""
    for _ in range(4):
        n = z3.simplify(e)
        if n.eq(e):
            break
        e = n
    return e


def _signed_bv2int_operand(e):
    """x if e is the signed bv2int ite(bvsle(0, x), ubv_to_int(x), ubv_to_int(x) - 2^w) (in the
    forms JavaSMT builds it, or with ubv_to_int(x) expanded, as Z3 does for a concat), else None."""
    cond, then, other = e.children()
    if _kind(cond) != z3.Z3_OP_SLEQ:
        return None
    zero, x = cond.children()
    if not (z3.is_bv_value(zero) and zero.as_long() == 0):
        return None
    m = 2 ** x.size()
    if _kind(then) != z3.Z3_OP_BV2INT or not then.arg(0).eq(x):
        unsigned = z3.BV2Int(x)
        if _normal(then).eq(_normal(unsigned)) and _normal(other).eq(_normal(unsigned - m)):
            return x
        return None
    if _kind(other) == z3.Z3_OP_ADD and other.num_args() == 2:
        a, b = other.children()
        for u, c in ((a, b), (b, a)):
            if u.eq(then) and _int_const(c) == -m:
                return x
    if _kind(other) == z3.Z3_OP_SUB and other.num_args() == 2:
        a, b = other.children()
        if a.eq(then) and _int_const(b) == m:
            return x
    return None


def lift_query(lifting, exprs):
    """The rewritten expressions, plus the length bounds of the strings and the Java char bounds of
    the char codes they rely on."""
    out = [lifting.rewrite(x) for x in exprs]
    needed = {}
    for x in exprs:
        for s in lifting.needs(x):
            needed[s.get_id()] = s
    for s in needed.values():
        # (A char code's index may itself be lifted.)
        out.append(z3.Length(s) <= JAVA_MAX_STRING_LENGTH if z3.is_string(s) else lifting.rewrite(s) <= MAX_JAVA_CHAR)
    return out
