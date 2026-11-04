"""
Unit tests for preferring small models (SVCompDriver.prefer_small and its helpers).

Run from symbolic-explorer/ with the harness venv (needs z3):
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_prefer_small -v
"""
import os
import sys
import unittest

import z3

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from driver.SVCompDriver import SVCompDriver, LARGE_INPUT
from solver.SolverHandler import Z3Handler


def solution(**values):
    return {name: {'plain_value': v, 'encoded_value': str(v)} for name, v in values.items()}


class PreferSmallTest(unittest.TestCase):

    def test_large_bit_vector_and_integer_inputs_are_large(self):
        self.assertTrue(SVCompDriver.is_large(solution(I_0=z3.BitVecVal(2**30, 32))))
        self.assertTrue(SVCompDriver.is_large(solution(I_0=z3.BitVecVal(-2**30, 32))))
        self.assertTrue(SVCompDriver.is_large(solution(J_0=z3.IntVal(LARGE_INPUT + 1))))

    def test_large_and_infinite_floating_point_inputs_are_large(self):
        d = z3.Float64()
        self.assertTrue(SVCompDriver.is_large(solution(D_0=z3.FPVal(2.0 ** 67, d))))
        self.assertTrue(SVCompDriver.is_large(solution(D_0=z3.fpMinusInfinity(d))))
        self.assertTrue(SVCompDriver.is_large(solution(F_0=z3.FPVal(-1e6, z3.Float32()))))

    def test_small_and_non_numeric_inputs_are_not(self):
        self.assertFalse(SVCompDriver.is_large(solution(I_0=z3.BitVecVal(-LARGE_INPUT, 32))))
        self.assertFalse(SVCompDriver.is_large(solution(D_0=z3.FPVal(-2.07e-317, z3.Float64()))))
        self.assertFalse(SVCompDriver.is_large(solution(D_0=z3.fpNaN(z3.Float64()))))
        self.assertFalse(SVCompDriver.is_large(solution(S_0=z3.StringVal("x" * 100000))))
        self.assertFalse(SVCompDriver.is_large({}))

    def test_bound_constraints_bound_without_excluding_the_solution(self):
        sol = solution(I_0=z3.BitVecVal(2**30, 32))
        s = z3.Solver()
        x = z3.BitVec('I_0', 32)
        s.add(Z3Handler.bound_constraints(sol, 16))
        s.add(x > 3)
        self.assertEqual(s.check(), z3.sat)
        self.assertLessEqual(s.model()[x].as_signed_long(), 16)
        s.add(x == 2**30)
        self.assertEqual(s.check(), z3.unsat)

    def test_bound_constraints_bound_floating_point_inputs(self):
        d = z3.Float64()
        sol = solution(D_0=z3.FPVal(2.0 ** 67, d))
        x = z3.FP('D_0', d)
        s = z3.Solver()
        s.add(Z3Handler.bound_constraints(sol, 16))
        s.add(z3.fpGT(x, z3.FPVal(3.0, d)))
        self.assertEqual(s.check(), z3.sat)
        self.assertTrue(z3.is_true(z3.simplify(z3.fpLEQ(s.model()[x], z3.FPVal(16.0, d)))))
        for excluded in (z3.fpPlusInfinity(d), z3.fpNaN(d)):
            s2 = z3.Solver()
            s2.add(Z3Handler.bound_constraints(sol, 16))
            s2.add(z3.fpIsInf(x) if excluded.isInf() else z3.fpIsNaN(x))
            self.assertEqual(s2.check(), z3.unsat)


if __name__ == '__main__':
    unittest.main()
