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

    def test_small_and_non_numeric_inputs_are_not(self):
        self.assertFalse(SVCompDriver.is_large(solution(I_0=z3.BitVecVal(-LARGE_INPUT, 32))))
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


if __name__ == '__main__':
    unittest.main()
