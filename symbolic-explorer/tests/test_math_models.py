"""
Unit tests for the explorer side of the approximate Math models: definitions of model functions
are sliced to the ones a query needs, and a query over a model gets a bounded resource limit.

Needs the explorer's dependencies, so run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_math_models -v
"""

import os
import sys
import unittest
from types import SimpleNamespace
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from solver.SolverHandler import SATResult
from strategy import StrategyService as ss
from strategy.StrategyService import StrategyService, declared_ufs

FP = '(_ FloatingPoint 11 53)'
DECL_X = f'(declare-fun D_0 () {FP})'


def uf(name, arity=1):
    return f'(declare-fun {name} ({" ".join([FP] * arity)}) {FP})'


def definition(name, arg='D_0', *nested):
    return '\n'.join([DECL_X, *[uf(n) for n in nested], uf(name), f'(assert (= ({name} {arg}) (fp #b0 #b01111111111 #x0000000000000)))'])


def branch_on(name):
    return '\n'.join([DECL_X, uf(name), f'(assert (fp.gt ({name} D_0) (fp #b0 #b00000000000 #x0000000000000)))'])


class DeclaredUfsTest(unittest.TestCase):

    def test_variables_are_not_functions(self):
        self.assertEqual(declared_ufs(DECL_X), frozenset())
        self.assertEqual(declared_ufs(DECL_X + uf('math_tan_3') + uf('|math_pow_4|', 2)), {'math_tan_3', 'math_pow_4'})
        self.assertEqual(declared_ufs('(declare-fun toLowerCase (Int) Int)'), {'toLowerCase'})


class SliceTest(unittest.TestCase):

    def test_unrelated_definitions_are_left_out(self):
        tan, log = definition('math_tan_0'), definition('math_log_1')
        sliced, ufs = StrategyService.slice_uf_definitions([tan, log], [branch_on('math_log_1')])
        self.assertEqual(sliced, [log])
        self.assertEqual(ufs, {'math_log_1'})

    def test_nested_definitions_are_followed_in_order(self):
        # math_pow_2's argument is math_log10_0(D_0); a query on math_pow_2 needs both.
        inner = definition('math_log10_0')
        outer = definition('math_pow_2', '(math_log10_0 D_0)', 'math_log10_0')
        other = definition('math_tan_1')
        sliced, ufs = StrategyService.slice_uf_definitions([inner, other, outer], [branch_on('math_pow_2')])
        self.assertEqual(sliced, [inner, outer])
        self.assertEqual(ufs, {'math_pow_2', 'math_log10_0'})

    def test_constraints_without_functions_are_kept(self):
        bound = f'{DECL_X}(assert (fp.lt D_0 (fp #b0 #b10000000000 #x0000000000000)))'
        sliced, ufs = StrategyService.slice_uf_definitions([bound, definition('math_tan_0')], [DECL_X])
        self.assertEqual(sliced, [bound])
        self.assertEqual(ufs, set())


class ApproximateLimitTest(unittest.TestCase):

    def solve(self, constraint, ufs, result, **kwargs):
        node = SimpleNamespace(id=7, gid=7, trace_id='t', constraint={'t': constraint}, inputs=[],
                               ufs=[SimpleNamespace(definition=d) for d in ufs], parent=None)
        db = mock.Mock()
        with mock.patch.object(ss.Database, 'instance', return_value=db), \
                mock.patch.object(ss.Z3Handler, 'solve', return_value=(result, {})) as solve:
            sat, _ = StrategyService.solve_branch(node, solver_timeout_ms=None, **kwargs)
        return sat, solve.call_args.kwargs['rlimit']

    def test_query_over_a_model_is_capped_and_given_up(self):
        sat, rlimit = self.solve(branch_on('math_tan_0'), [definition('math_tan_0')], SATResult.LIMIT,
                                 approximate_rlimit=100)
        self.assertEqual((sat, rlimit), (SATResult.UNKNOWN, 100))

    def test_smaller_base_limit_still_escalates(self):
        sat, rlimit = self.solve(branch_on('math_tan_0'), [definition('math_tan_0')], SATResult.LIMIT,
                                 solver_rlimit=10, approximate_rlimit=100)
        self.assertEqual((sat, rlimit), (SATResult.LIMIT, 10))

    def test_exact_query_is_not_capped(self):
        # The trace has a model call, but this branch does not depend on it.
        sat, rlimit = self.solve(f'{DECL_X}(assert (fp.isZero D_0))', [definition('math_tan_0')], SATResult.UNSAT,
                                 approximate_rlimit=100)
        self.assertEqual((sat, rlimit), (SATResult.UNSAT, None))


if __name__ == '__main__':
    unittest.main()
