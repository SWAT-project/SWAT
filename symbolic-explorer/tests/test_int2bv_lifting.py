"""
Unit tests for solver/Int2BvLifting: comparisons over int_to_bv of bounded Integer terms are made over
the Integers, with the same truth value under every assignment.

Needs z3, so run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_int2bv_lifting -v
"""

import os
import random
import sys
import unittest

import z3

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from solver.Int2BvLifting import Int2BvLifting, lift_query, JAVA_MAX_STRING_LENGTH, MAX_JAVA_CHAR


def mixes_sorts(e):
    s = e.sexpr()
    return 'int_to_bv' in s or 'ubv_to_int' in s or 'int2bv' in s or 'bv2int' in s


class Terms:
    """Random terms over a string s, an 8-bit x and a 32-bit y, as the executor builds them."""

    def __init__(self, rnd):
        self.rnd = rnd
        self.s = z3.String('s')
        self.x = z3.BitVec('x', 8)
        self.y = z3.BitVec('y', 32)

    def int_leaf(self):
        r = self.rnd
        return r.choice([
            lambda: z3.Length(self.s),
            lambda: z3.IndexOf(self.s, z3.StringVal('a'), z3.IntVal(r.randint(-1, 2))),
            lambda: z3.StrToCode(z3.SubString(self.s, z3.IntVal(r.randint(-1, 2)), z3.IntVal(1))),
            lambda: z3.BV2Int(self.x),
            lambda: z3.BV2Int(self.y),
            lambda: z3.IntVal(r.choice([0, 1, -1, 3, 65535, 65536, 2 ** 31 - 1, 2 ** 31, -2 ** 31, 2 ** 32 - 1])),
        ])()

    def int_term(self, depth):
        r = self.rnd
        if depth == 0 or r.random() < 0.3:
            return self.int_leaf()
        a, b = self.int_term(depth - 1), self.int_term(depth - 1)
        return r.choice([
            lambda: a + b,
            lambda: a - b,
            lambda: a * z3.IntVal(r.choice([-1, 2, 3])),
            lambda: -a,
            lambda: z3.If(self.bool_term(depth - 1), a, b),
        ])()

    def bv_term(self, width, depth):
        r = self.rnd
        if depth == 0 or r.random() < 0.25:
            return r.choice([
                lambda: z3.Int2BV(self.int_term(2), width),
                lambda: z3.BitVecVal(r.randint(0, 2 ** width - 1), width),
                lambda: z3.BitVecVal(2 ** width - 1, width),
            ])()
        a, b = self.bv_term(width, depth - 1), self.bv_term(width, depth - 1)
        options = [
            lambda: a + b,
            lambda: a - b,
            lambda: -a,
            lambda: a * z3.BitVecVal(r.choice([2, 3, 2 ** width - 1]), width),
            lambda: z3.If(self.bool_term(depth - 1), a, b),
        ]
        if width > 8:
            narrow = 8 if width == 16 else 16
            options.append(lambda: z3.ZeroExt(width - narrow, self.bv_term(narrow, depth - 1)))
            options.append(lambda: z3.SignExt(width - narrow, self.bv_term(narrow, depth - 1)))
        return r.choice(options)()

    def bool_term(self, depth):
        r = self.rnd
        width = r.choice([8, 16, 32])
        a, b = self.bv_term(width, max(depth - 1, 0)), self.bv_term(width, max(depth - 1, 0))
        return r.choice([
            lambda: a == b, lambda: a != b,
            lambda: a <= b, lambda: a < b, lambda: a >= b, lambda: a > b,
            lambda: z3.ULE(a, b), lambda: z3.ULT(a, b), lambda: z3.UGE(a, b), lambda: z3.UGT(a, b),
            lambda: z3.BV2Int(a) == z3.IntVal(r.randint(0, 300)),
        ])()

    def assignment(self, java_chars=False):
        r = self.rnd
        chars = ['a', 'b', 'ÿ', '￿'] + ([] if java_chars else ['\U0001F600'])
        s = ''.join(r.choice(chars) for _ in range(r.randint(0, 3)))
        x = r.choice([0, 1, 127, 128, 255, r.randint(0, 255)])
        y = r.choice([0, 1, 2 ** 31 - 1, 2 ** 31, 2 ** 32 - 1, r.randint(0, 2 ** 32 - 1)])
        return [(self.s, z3.StringVal(s)), (self.x, z3.BitVecVal(x, 8)), (self.y, z3.BitVecVal(y, 32))]


def evaluate(e, assignment):
    v = z3.simplify(z3.substitute(e, *assignment))
    if not (z3.is_true(v) or z3.is_false(v)):
        raise AssertionError(f'{e} does not evaluate to a constant: {v}')
    return z3.is_true(v)


class LiftingTest(unittest.TestCase):

    def test_random_comparisons_keep_their_truth_value(self):
        rnd = random.Random(7)
        terms = Terms(rnd)
        lifted = 0
        for _ in range(150):
            e = terms.bool_term(3)
            lifting = Int2BvLifting()
            r = lifting.rewrite(e)
            lifted += not r.eq(e)
            for _ in range(12):
                a = terms.assignment()
                self.assertEqual(evaluate(e, a), evaluate(r, a), f'\n{e}\nrewritten to\n{r}\nunder {a}')
        self.assertGreater(lifted, 50)

    def test_string_length_comparisons_become_integer_comparisons(self):
        s = z3.String('s')
        length = z3.Int2BV(z3.Length(s), 32)
        e = z3.And(z3.Not(length <= z3.BitVecVal(2, 32)),
                   z3.Int2BV(z3.StrToCode(z3.SubString(s, z3.IntVal(1), z3.IntVal(1))), 16) != z3.BitVecVal(38, 16))
        out = lift_query(Int2BvLifting(), [e])
        self.assertFalse(any(mixes_sorts(x) for x in out))
        # The length bound the rewrite relies on is part of the query.
        self.assertTrue(any(x.eq(z3.Length(s) <= JAVA_MAX_STRING_LENGTH) for x in out))
        solver = z3.Solver()
        solver.add(*out)
        self.assertEqual(solver.check(), z3.sat)
        self.assertTrue(z3.is_true(solver.model().eval(e, model_completion=True)))

    def test_overflowing_length_arithmetic_wraps(self):
        # s.length() + 3 overflows for long strings: the lift must keep the wrap.
        s = z3.String('s')
        e = z3.BitVecVal(0, 32) > z3.Int2BV(z3.Length(s) + 3, 32)
        r = Int2BvLifting().rewrite(e)
        self.assertFalse(mixes_sorts(r))
        # (A model would need a string of 2^31 chars, so evaluate at given lengths instead.)
        for n, holds in [(0, False), (2 ** 31 - 4, False), (2 ** 31 - 3, True), (2 ** 31 - 1, True)]:
            v = z3.simplify(z3.substitute(r, (z3.Length(s), z3.IntVal(n))))
            self.assertEqual(z3.is_true(v), holds, f'length {n}: {v}')

    def test_signed_bv2int_of_a_length_is_the_length(self):
        # JavaSMT's signed bv2int: ite(0 <= x, ubv_to_int(x), ubv_to_int(x) - 2^32)
        s = z3.String('s')
        x = z3.Int2BV(z3.Length(s), 32) + z3.BitVecVal(2 ** 32 - 1, 32)
        signed = z3.If(z3.BitVecVal(0, 32) <= x, z3.BV2Int(x), z3.IntVal(-2 ** 32) + z3.BV2Int(x))
        e = z3.SubString(s, signed, z3.IntVal(1)) == z3.StringVal('a')
        out = lift_query(Int2BvLifting(), [e])
        self.assertFalse(any(mixes_sorts(x) for x in out))

    def test_terms_without_int2bv_are_left_alone(self):
        y = z3.BitVec('y', 32)
        e = z3.And(y + 1 > z3.BitVecVal(3, 32), z3.BV2Int(y) < 7)
        self.assertTrue(Int2BvLifting().rewrite(e).eq(e))


def char_code(s, i):
    return z3.StrToCode(z3.SubString(s, z3.IntVal(i), z3.IntVal(1)))


def char_at(s, i):
    """s.charAt(i) as the executor builds it (CharValue.java)"""
    return z3.Int2BV(char_code(s, i), 16)


class JavaCharsLiftingTest(unittest.TestCase):
    """Int2BvLifting(java_chars=True): char codes in [-1, 0xFFFF], each relied-on one bounded."""

    def test_random_comparisons_keep_their_truth_value_under_the_bounds(self):
        rnd = random.Random(11)
        terms = Terms(rnd)
        lifted = 0
        for _ in range(80):
            e = terms.bool_term(3)
            out = lift_query(Int2BvLifting(java_chars=True), [e])
            r, bounds = out[0], out[1:]
            lifted += not r.eq(e)
            for _ in range(8):
                # Also with a char above 0xFFFF: then the bounds may exclude the assignment, but
                # wherever they hold, the rewrite has the truth value of e.
                a = terms.assignment(java_chars=rnd.random() < 0.7)
                self.assertEqual(evaluate(z3.And(*out), a), evaluate(z3.And(e, *bounds), a),
                                 f'\n{e}\nrewritten to\n{out}\nunder {a}')
            for _ in range(4):
                a = terms.assignment(java_chars=True)
                self.assertTrue(all(evaluate(b, a) for b in bounds),
                                f'a bound excludes a Java string: {bounds} under {a}')
        self.assertGreater(lifted, 25)

    def test_char_comparisons_differ_only_where_the_bound_excludes_the_string(self):
        # A char above 0xFFFF is truncated by int_to_bv but not by the lift: the two disagree only
        # where the char code bound is false, which no Java string makes it.
        s = z3.String('s')
        ops = [lambda a, b: a == b, lambda a, b: a != b, lambda a, b: a < b, lambda a, b: a <= b,
               lambda a, b: a > b, lambda a, b: a >= b, z3.ULT, z3.ULE, z3.UGT, z3.UGE]
        differ = 0
        for op in ops:
            for c in [0, 0x41, 0x7FFF, 0x8000, 0xF600, 0xF601, 0xFFFF]:
                for i, d in [(0, 0), (1, 0), (0, 1)]:
                    e = op(char_at(s, i) + z3.BitVecVal(d, 16), z3.BitVecVal(c, 16))
                    out = lift_query(Int2BvLifting(java_chars=True), [e])
                    self.assertFalse(any(mixes_sorts(x) for x in out))
                    for v in ['', 'A', '\uf600', '\uffff', 'A\uffff', '\U0001F600', 'A\U0001F600']:
                        a = [(s, z3.StringVal(v))]
                        java = all(ord(ch) <= MAX_JAVA_CHAR for ch in v)
                        self.assertEqual(evaluate(z3.And(*out), a), evaluate(z3.And(e, *out[1:]), a), f'{e} under {v!r}')
                        if java:
                            self.assertEqual(evaluate(z3.And(*out), a), evaluate(e, a), f'{e} under {v!r}')
                        elif evaluate(out[0], a) != evaluate(e, a):
                            differ += 1
                            self.assertFalse(evaluate(z3.And(*out[1:]), a))
        self.assertGreater(differ, 0)

    def test_char_code_lift_has_no_mod_and_its_bound_is_in_the_query(self):
        # java-ranger LoopCharAt: s.length() > 5 && s.charAt(2) == 'B'
        s = z3.String('s')
        e = z3.And(z3.Int2BV(z3.Length(s), 32) > z3.BitVecVal(5, 32), char_at(s, 2) == z3.BitVecVal(66, 16))
        out = lift_query(Int2BvLifting(java_chars=True), [e])
        self.assertFalse(any(mixes_sorts(x) for x in out))
        self.assertNotIn('mod', ' '.join(x.sexpr() for x in out))
        self.assertTrue(any(x.eq(char_code(s, 2) <= MAX_JAVA_CHAR) for x in out))
        unicode = lift_query(Int2BvLifting(), [e])
        self.assertIn('mod', ' '.join(x.sexpr() for x in unicode))
        solver = z3.Solver()
        solver.add(*out)
        self.assertEqual(solver.check(), z3.sat)
        self.assertEqual(solver.model().eval(z3.SubString(s, 2, 1)).as_string(), 'B')

    def test_a_char_above_0xffff_cannot_satisfy_a_char_comparison(self):
        # Truncated to 16 bits, U+1F600 reads as the Java char 0xF600. The full range keeps that
        # (exact for SMT strings); with Java chars, only the char 0xF600 itself is a solution.
        s = z3.String('s')
        e = z3.And(char_at(s, 0) == z3.BitVecVal(0xF600, 16), char_code(s, 0) > 0xFFFF)
        self.assertEqual(self.check(lift_query(Int2BvLifting(), [e])), z3.sat)
        self.assertEqual(self.check(lift_query(Int2BvLifting(java_chars=True), [e])), z3.unsat)
        e = char_at(s, 0) == z3.BitVecVal(0xF600, 16)
        solver = z3.Solver()
        solver.add(*lift_query(Int2BvLifting(java_chars=True), [e]))
        self.assertEqual(solver.check(), z3.sat)
        self.assertEqual(solver.model().eval(char_code(s, 0)).as_long(), 0xF600)

    @staticmethod
    def check(exprs):
        solver = z3.Solver()
        solver.add(*exprs)
        return solver.check()

    def test_java_chars_only(self):
        lifting = Int2BvLifting(java_chars=True)
        s = z3.String('s')
        c16, c32 = z3.BitVec('c16', 16), z3.BitVec('c32', 32)
        ok = lambda x: lifting.java_chars_only(x == s)
        self.assertTrue(ok(z3.Concat(s, z3.StringVal('a\u00ff\ud83d\uffff'))))
        # Character.toString(char), new String(char[]): str.from_code of a 16-bit char
        self.assertTrue(ok(z3.StrFromCode(z3.BV2Int(c16))))
        # StringBuilder.reverse: str.from_code of a char code of a string of Java chars
        self.assertTrue(ok(z3.StrFromCode(char_code(s, 0))))
        # Integer.toHexString: digits
        d = z3.BV2Int(z3.Extract(3, 0, c32))
        self.assertTrue(ok(z3.StrFromCode(d + z3.If(d > 9, z3.IntVal(87), z3.IntVal(48)))))
        self.assertTrue(ok(z3.IntToStr(z3.BV2Int(c32))))
        # a literal with a surrogate pair is one SMT char above 0xFFFF (JavaSMT's makeString)
        self.assertFalse(ok(z3.Concat(s, z3.StringVal('\U0001F600'))))
        self.assertFalse(lifting.java_chars_only(z3.InRe(s, z3.Range('a', '\U0001F600'))))
        # an int code point (appendCodePoint, indexOf(int)), or a char code above 0xFFFF
        self.assertFalse(ok(z3.StrFromCode(z3.BV2Int(c32))))
        self.assertFalse(ok(z3.StrFromCode(char_code(s, 0) + 1)))
        self.assertFalse(ok(z3.StrFromCode(z3.Int('i'))))
        self.assertFalse(ok(z3.StrFromCode(z3.IntVal(0x10000))))
        self.assertTrue(ok(z3.StrFromCode(z3.IntVal(0xFFFF))))
        # Char-sorted terms: the executor builds none
        self.assertFalse(lifting.java_chars_only(z3.SubSeq(s, 0, 1) == z3.Unit(z3.CharVal(65))))
        # deep terms do not hit the recursion limit
        deep = s
        for i in range(5000):
            deep = z3.Concat(deep, z3.StrFromCode(z3.BV2Int(c16) + i % 2))
        self.assertFalse(ok(deep))


class LiftInt2BvTest(unittest.TestCase):
    """Z3Handler.lift_int2bv picks the lifting by the query."""

    def lift(self, assertions):
        from types import SimpleNamespace
        from solver.SolverHandler import Z3Handler
        return Z3Handler.lift_int2bv(SimpleNamespace(), assertions)

    def test_java_chars_unless_the_query_has_a_char_above_0xffff(self):
        s = z3.String('s')
        e = char_at(s, 0) == z3.BitVecVal(66, 16)
        out = self.lift([e, z3.PrefixOf(z3.StringVal('B\u00e9'), s)])
        self.assertNotIn('mod', ' '.join(x.sexpr() for x in out))
        self.assertTrue(any(x.eq(char_code(s, 0) <= MAX_JAVA_CHAR) for x in out))
        out = self.lift([e, z3.PrefixOf(z3.StringVal('B\U0001F600'), s)])
        self.assertIn('mod', ' '.join(x.sexpr() for x in out))
        self.assertFalse(any(x.eq(char_code(s, 0) <= MAX_JAVA_CHAR) for x in out))
        self.assertFalse(any(mixes_sorts(x) for x in out))


if __name__ == '__main__':
    unittest.main()
