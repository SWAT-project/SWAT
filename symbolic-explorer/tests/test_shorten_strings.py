"""
Unit tests for Z3Handler.shorten_strings (--prefer-short-strings): the string inputs of a model are
made as short as the assertions allow, one input after the other.

Needs z3, so run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_shorten_strings -v
"""

import os
import sys
import unittest

import z3

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from solver.SolverHandler import Z3Handler


class ShortenStringsTest(unittest.TestCase):

    def test_inputs_get_the_shortest_length_the_query_allows(self):
        s = z3.String('java/lang/String_0')
        t = z3.String('java/lang/String_1')
        solver = z3.Solver()
        solver.add(z3.PrefixOf(z3.StringVal('-x'), s), z3.Length(s) + z3.Length(t) >= 7, z3.Length(t) >= 3)
        solver.push()
        solver.add(z3.Length(s) == 6, z3.Length(t) == 9)  # a model with longer strings than needed
        self.assertEqual(solver.check(), z3.sat)
        model = solver.model()
        solver.pop()
        self.assertEqual(solver.check(), z3.sat)
        shorter = Z3Handler.shorten_strings(solver, model, 0.01)
        lengths = sorted(shorter.eval(z3.Length(v)).as_long() for v in (s, t))
        self.assertEqual(sum(lengths), 7)
        self.assertTrue(shorter.eval(z3.PrefixOf(z3.StringVal('-x'), s)))

    def test_other_variables_are_left_alone(self):
        i = z3.BitVec('I_0', 32)
        tmp = z3.String('tmp')  # not an input name
        solver = z3.Solver()
        solver.add(z3.Length(tmp) > 4, i == 3)
        self.assertEqual(solver.check(), z3.sat)
        model = Z3Handler.shorten_strings(solver, solver.model(), 0.01)
        self.assertEqual(model.eval(i).as_long(), 3)


if __name__ == '__main__':
    unittest.main()
