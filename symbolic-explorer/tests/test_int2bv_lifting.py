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

from solver.Int2BvLifting import Int2BvLifting, lift_query, bound_fact, JAVA_MAX_STRING_LENGTH


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

    def assignment(self):
        r = self.rnd
        chars = ['a', 'b', 'ÿ', '￿', '\U0001F600']
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
        compared = 0
        for _ in range(150):
            e = terms.bool_term(3)
            lifting = Int2BvLifting()
            r = lifting.rewrite(e)
            lifted += not r.eq(e)
            bounds = [bound_fact(t) for t in lifting.needs(e)]
            for _ in range(12):
                a = terms.assignment()
                # Equivalent only for Java strings: no char above 0xFFFF where the rewrite relied on that.
                if not all(evaluate(b, a) for b in bounds):
                    continue
                compared += 1
                self.assertEqual(evaluate(e, a), evaluate(r, a), f'\n{e}\nrewritten to\n{r}\nunder {a}')
        self.assertGreater(lifted, 50)
        self.assertGreater(compared, 1000)

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

    def test_chars_lift_without_modulo(self):
        # s.charAt(i) == '\\' over the Integers, with the char bound in the query
        s = z3.String('s')
        code = z3.StrToCode(z3.SubString(s, z3.IntVal(3), z3.IntVal(1)))
        e = z3.Int2BV(code, 16) == z3.BitVecVal(92, 16)
        out = lift_query(Int2BvLifting(), [e])
        self.assertFalse(any(mixes_sorts(x) for x in out))
        self.assertNotIn('mod', ' '.join(x.sexpr() for x in out))
        self.assertTrue(any(x.eq(code <= 0xFFFF) for x in out))
        # The char of a missing index (-1) is 0xFFFF as a Java char, as with int_to_bv.
        r = Int2BvLifting().rewrite(z3.Int2BV(code, 16) == z3.BitVecVal(0xFFFF, 16))
        self.assertTrue(z3.is_true(z3.simplify(z3.substitute(r, (code, z3.IntVal(-1))))))

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

    def test_char_widened_to_int_is_the_char_code(self):
        # (int) s.charAt(0) as the executor builds it: the signed bv2int of the zero-extended char,
        # with ubv_to_int(concat ...) expanded as Z3 does it.
        s = z3.String('s')
        code = z3.StrToCode(z3.SubString(s, z3.IntVal(0), z3.IntVal(1)))
        char = z3.Int2BV(code, 16)
        x = z3.Concat(z3.BitVecVal(0, 16), char)
        unsigned = 65536 * z3.BV2Int(z3.BitVecVal(0, 16)) + z3.BV2Int(char)
        widened = z3.If(z3.BitVecVal(0, 32) <= x, unsigned, z3.IntVal(-2 ** 32) + unsigned)
        e = z3.StrFromCode(widened) == z3.StringVal('a')
        out = lift_query(Int2BvLifting(), [e])
        self.assertFalse(any(mixes_sorts(x) for x in out))
        solver = z3.Solver()
        solver.add(*out)
        self.assertEqual(solver.check(), z3.sat)
        self.assertEqual(solver.model().eval(z3.SubString(s, 0, 1)).as_string(), 'a')

    def test_terms_without_int2bv_are_left_alone(self):
        y = z3.BitVec('y', 32)
        e = z3.And(y + 1 > z3.BitVecVal(3, 32), z3.BV2Int(y) < 7)
        self.assertTrue(Int2BvLifting().rewrite(e).eq(e))


if __name__ == '__main__':
    unittest.main()
