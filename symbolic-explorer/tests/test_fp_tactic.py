"""
Unit tests for solving queries with floating-point terms with Z3's qffp tactic first
(Z3Handler.check_query): which queries are selected, that its models look like the default
solver's, and when the default solver decides instead.

Needs the explorer's dependencies, so run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_fp_tactic -v
"""

import itertools
import os
import sys
import threading
import time
import unittest
from types import SimpleNamespace
from unittest import mock

import z3

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from data.Database import Database
from solver import SolverHandler as sh
from solver.SolverHandler import SATResult, Z3Handler, _has_fp, _entry_has_fp

DATA = os.path.join(os.path.dirname(__file__), 'data')

DECLS = '''
(declare-fun D_0 () (_ FloatingPoint 11 53))
(declare-fun D_8 () (_ FloatingPoint 11 53))
(declare-fun D_9 () (_ FloatingPoint 11 53))
(declare-fun F_5 () (_ FloatingPoint 8 24))
(declare-fun |java/lang/String_1| () String)
(declare-fun math_sin_3 ((_ FloatingPoint 11 53)) (_ FloatingPoint 11 53))
(declare-fun |[I_2| () (Array (_ BitVec 32) (_ BitVec 32)))
(declare-fun |[D_7| () (Array (_ BitVec 32) (_ FloatingPoint 11 53)))
(declare-fun |[I_2_length| () (_ BitVec 32))
(declare-fun I_4 () (_ BitVec 32))
(declare-fun I_6 () Int)
'''

# FP with all the other theories SWAT queries mix it with: a SampledMath UF, parseFloat (str.to_int
# + int_to_bv + to_fp), an array, BV and Int arithmetic over a string length.
MIXED = '''
(assert (fp.gt (math_sin_3 D_0) ((_ to_fp 11 53) RNE 0.5)))
(assert (fp.lt D_0 ((_ to_fp 11 53) RNE 10.0)))
(assert (fp.eq ((_ to_fp 11 53) RNE ((_ int2bv 64) (str.to_int |java/lang/String_1|))) (fp.add RNE D_0 ((_ to_fp 11 53) RNE 1.0))))
(assert (< (str.len |java/lang/String_1|) 3))
(assert (= (select |[I_2| I_4) #x00000007))
(assert (bvult I_4 |[I_2_length|))
(assert (bvult |[I_2_length| #x00000004))
(assert (fp.isNaN F_5))
(assert (> (+ (str.len |java/lang/String_1|) (bv2int I_4)) 1))
'''

# A pure QF_FP query (from float-nonlinear-calculation) neither solver decides within seconds
SLOW = '''
(assert (let ((s (fp.sqrt RNE D_0)) (q (fp.div RNE D_9 D_8)))
  (not (bvsle (ite (or (fp.isNaN s) false) #xffffffff (ite (fp.eq s q) #x00000000 (ite (fp.lt q s) #x00000001 #xffffffff)))
              #x00000000))))
'''


def parse(asserts, ctx=None):
    return list(z3.parse_smt2_string(DECLS + asserts, ctx=ctx or z3.Context()))


def default_solver(query):
    s = z3.Solver(ctx=query[0].ctx)
    s.add(*query)
    return s


class Stats:
    """Statistics with an rlimit count `used` above what the context had used before."""

    def __init__(self, used):
        self.count = sh._last_rlimit_count + used

    def keys(self):
        return ['rlimit count']

    def get_key_value(self, key):
        return self.count


class FakeTacticSolver:
    """Stands in for Tactic('qffp').solver(): returns `result` (with `reason` or `model`)."""

    def __init__(self, result, reason='', model=None, used=1000, raises=None):
        self.result, self.reason, self._model, self.used, self.raises = result, reason, model, used, raises
        self.params = {}

    def add(self, *a):
        pass

    def set(self, k, v):
        self.params[k] = v

    def check(self):
        if self.raises:
            raise self.raises
        return self.result

    def reason_unknown(self):
        return self.reason

    def model(self):
        return self._model

    def statistics(self):
        return Stats(self.used)


def with_tactic(fake):
    return mock.patch.object(sh.z3, 'Tactic', return_value=SimpleNamespace(solver=lambda: fake))


class SelectionTest(unittest.TestCase):

    def has_fp(self, asserts):
        return _has_fp(parse(asserts))

    def test_fp(self):
        self.assertTrue(self.has_fp('(assert (fp.isNaN D_0))'))
        self.assertTrue(self.has_fp('(assert (fp.lt F_5 ((_ to_fp 8 24) RNE 1.0)))'))

    def test_fp_from_bits_only(self):
        # No FP input, but an FP term (Float.intBitsToFloat)
        self.assertTrue(self.has_fp('(assert (fp.isNaN ((_ to_fp 8 24) I_4)))'))

    def test_fp_with_uf(self):
        self.assertTrue(self.has_fp('(assert (fp.gt (math_sin_3 D_0) D_0))'))

    def test_fp_with_string(self):
        self.assertTrue(self.has_fp('(assert (fp.isZero ((_ to_fp 11 53) RNE ((_ int2bv 64) (str.to_int |java/lang/String_1|)))))'))

    def test_fp_with_array(self):
        self.assertTrue(self.has_fp('(assert (fp.isZero (select |[D_7| I_4)))'))
        self.assertTrue(self.has_fp('(assert (and (= (select |[I_2| I_4) #x00000001) (fp.isZero D_0)))'))
        self.assertTrue(self.has_fp(MIXED))

    def test_no_fp(self):
        self.assertFalse(self.has_fp('(assert (bvslt I_4 #x00000003))'))
        self.assertFalse(self.has_fp('(assert (and (> I_6 2) (= (str.len |java/lang/String_1|) I_6)))'))
        self.assertFalse(self.has_fp('(assert (= (str.to_int |java/lang/String_1|) (bv2int (select |[I_2| I_4))))'))
        self.assertFalse(_has_fp([]))

    def test_shared_subterms_and_deep_terms(self):
        # A deep DAG is walked without recursion, each shared subterm once.
        x = z3.BitVec('I_4', 32)
        for _ in range(5000):
            x = x + x
        self.assertFalse(_has_fp([x == 0]))
        self.assertTrue(_has_fp([z3.And(x == 0, z3.fpIsNaN(z3.FP('D_0', z3.Float64())))]))

    def test_entry_flag_is_kept(self):
        entry = {'exprs': parse('(assert (fp.isNaN D_0))')}
        self.assertTrue(_entry_has_fp(entry))
        entry['exprs'] = parse('(assert (bvslt I_4 #x00000003))')  # not looked at again
        self.assertTrue(_entry_has_fp(entry))


class TacticResultTest(unittest.TestCase):

    def check(self, query, **kw):
        self.default = default_solver(query)
        return Z3Handler.check_query(self.default, query, query, **kw)

    def test_model_has_the_default_solvers_shape(self):
        query = parse(MIXED)
        res_t, model_t, solver_t, _, _ = self.check(query, fp_tactic=True)
        self.assertIsNot(solver_t, self.default)
        res_d, model_d, solver_d, _, _ = self.check(query, fp_tactic=False)
        self.assertIs(solver_d, self.default)
        self.assertEqual((res_t, res_d), (SATResult.SAT, SATResult.SAT))

        def shape(m):
            return {str(d.name()): (d.arity(), type(m[d]).__name__) for d in m.decls()}
        self.assertEqual(shape(model_t), shape(model_d))
        self.assertEqual(shape(model_t), {
            'D_0': (0, 'FPNumRef'), 'F_5': (0, 'FPNumRef'), 'I_4': (0, 'BitVecNumRef'),
            '[I_2': (0, 'ArrayRef'), '[I_2_length': (0, 'BitVecNumRef'),
            'java/lang/String_1': (0, 'SeqRef'), 'math_sin_3': (1, 'FuncInterp')})
        self.assertEqual(set(Z3Handler.extract_and_encode_values(model_t)), set(Z3Handler.extract_and_encode_values(model_d)))

    def test_model_check_does_not_complete_the_model(self):
        # An input the query does not constrain stays out of the model, as with the default solver
        query = parse('(assert (fp.isNaN D_0)) (assert (= I_4 I_4))')
        _, model, _, _, _ = self.check(query, fp_tactic=True)
        self.assertEqual([str(d.name()) for d in model.decls()], ['D_0'])

    def test_unsat(self):
        query = parse('(assert (fp.lt (math_sin_3 D_0) (math_sin_3 D_0)))'
                      '(assert (= (str.len |java/lang/String_1|) 2))')
        res, model, solver, _, _ = self.check(query, fp_tactic=True)
        self.assertEqual(res, SATResult.UNSAT)
        self.assertIsNot(solver, self.default)

    def test_slow_fp_uf_core_is_solved_quickly(self):
        # A query from float-nonlinear-calculation (SampledMath math_log UFs): the default solver
        # needs over 10 s (and minutes at seed 0), the qffp tactic well under a second.
        with open(os.path.join(DATA, 'fp_uf_core.smt2')) as f:
            query = list(z3.parse_smt2_string(f.read(), ctx=z3.Context()))
        t = time.perf_counter()
        res, model, solver, _, _ = self.check(query, fp_tactic=True, timeout_ms=30_000)
        self.assertEqual(res, SATResult.SAT)
        self.assertIsNot(solver, self.default)
        self.assertLess(time.perf_counter() - t, 10)

    def test_rlimit_runs_out_in_the_tactic(self):
        with open(os.path.join(DATA, 'fp_uf_core.smt2')) as f:
            query = list(z3.parse_smt2_string(f.read(), ctx=z3.Context()))
        res, _, solver, _, used = self.check(query, fp_tactic=True, rlimit=100_000)
        self.assertEqual(res, SATResult.LIMIT)
        self.assertIsNot(solver, self.default)  # no fallback
        self.assertLessEqual(used, 100_000 + 1000)
        res, _, _, _, _ = self.check(query, fp_tactic=True, rlimit=100_000_000)
        self.assertEqual(res, SATResult.SAT)

    def test_interrupt_stops_the_tactic(self):
        query = parse(SLOW)
        ctx = query[0].ctx
        timer = threading.Timer(0.3, ctx.interrupt)
        timer.start()
        t = time.perf_counter()
        try:
            res, _, solver, _, _ = self.check(query, fp_tactic=True, rlimit=None)
        finally:
            timer.cancel()
        self.assertEqual(res, SATResult.UNKNOWN)
        self.assertIsNot(solver, self.default)  # no fallback
        self.assertLess(time.perf_counter() - t, 20)

    def test_timeout_in_the_tactic(self):
        res, _, solver, _, _ = self.check(parse(SLOW), fp_tactic=True, timeout_ms=300)
        self.assertEqual(res, SATResult.UNKNOWN)
        self.assertIsNot(solver, self.default)


class FallbackTest(unittest.TestCase):
    """The tactic is replaced by a fake; the default solver is real, or a mock to see it is not run."""

    QUERY = '(assert (fp.gt D_0 ((_ to_fp 11 53) RNE 1.0)))'

    def run_with(self, fake, query=None, **kw):
        query = query or parse(self.QUERY)
        with with_tactic(fake):
            return Z3Handler.check_query(default_solver(query), query, query, fp_tactic=True, **kw)

    def no_fallback(self, fake, **kw):
        query = parse(self.QUERY)
        solver = mock.Mock()
        with with_tactic(fake):
            res = Z3Handler.check_query(solver, query, query, fp_tactic=True, **kw)
        solver.check.assert_not_called()
        return res

    def assert_default_solved(self, out):
        res, model, solver, _, _ = out
        self.assertEqual(res, SATResult.SAT)
        self.assertIsInstance(solver, z3.Solver)
        self.assertTrue(sh._satisfies(model, solver.assertions()))

    def test_unknown_falls_back(self):
        self.assert_default_solved(self.run_with(FakeTacticSolver(z3.unknown, 'smt tactic failed to show goal to be sat/unsat (incomplete (theory fpa))')))

    def test_exception_falls_back(self):
        self.assert_default_solved(self.run_with(FakeTacticSolver(None, raises=z3.Z3Exception('tactic failed'))))

    def test_tactic_that_cannot_be_made_falls_back(self):
        query = parse(self.QUERY)
        with mock.patch.object(sh.z3, 'Tactic', side_effect=z3.Z3Exception('unknown tactic')):
            self.assert_default_solved(Z3Handler.check_query(default_solver(query), query, query, fp_tactic=True))

    def test_wrong_model_falls_back(self):
        query = parse(self.QUERY)
        other = default_solver(parse('(assert (fp.isZero D_0))', ctx=query[0].ctx))
        self.assertEqual(other.check(), z3.sat)
        self.assert_default_solved(self.run_with(FakeTacticSolver(z3.sat, model=other.model()), query=query))

    def test_model_must_satisfy_the_assertions_before_lifting(self):
        query = parse(self.QUERY)
        ctx = query[0].ctx
        good = default_solver(query)
        self.assertEqual(good.check(), z3.sat)
        original = query + [z3.FP('D_0', z3.Float64(ctx)) != good.model().eval(z3.FP('D_0', z3.Float64(ctx)))]
        with with_tactic(FakeTacticSolver(z3.sat, model=good.model())):
            res, model, solver, _, _ = Z3Handler.check_query(default_solver(original), query, original, fp_tactic=True)
        self.assertEqual(res, SATResult.SAT)
        self.assertIsInstance(solver, z3.Solver)

    def test_fallback_gets_what_is_left_of_the_budget(self):
        query = parse(self.QUERY)
        solver = mock.Mock()
        solver.check.return_value = z3.unsat
        solver.statistics.return_value = Stats(0)
        with with_tactic(FakeTacticSolver(z3.unknown, 'incomplete', used=1000)):
            res = Z3Handler.check_query(solver, query, query, fp_tactic=True, rlimit=5000, timeout_ms=60_000)
        self.assertEqual(res[0], SATResult.UNSAT)
        solver.set.assert_any_call('rlimit', 4000)
        timeout = [c.args[1] for c in solver.set.call_args_list if c.args[0] == 'timeout'][0]
        self.assertTrue(59_000 < timeout <= 60_000)

    def test_rlimit_exhausted_is_limit(self):
        for reason in ('canceled', 'max. resource limit exceeded'):
            fake = FakeTacticSolver(z3.unknown, reason)
            self.assertEqual(self.no_fallback(fake, rlimit=5000)[0], SATResult.LIMIT)
            self.assertEqual(fake.params, {'rlimit': 5000})

    def test_interrupt_and_timeout_are_unknown(self):
        self.assertEqual(self.no_fallback(FakeTacticSolver(z3.unknown, 'interrupted'), rlimit=5000)[0], SATResult.UNKNOWN)
        self.assertEqual(self.no_fallback(FakeTacticSolver(z3.unknown, 'canceled'))[0], SATResult.UNKNOWN)
        fake = FakeTacticSolver(z3.unknown, 'timeout')
        self.assertEqual(self.no_fallback(fake, timeout_ms=1000)[0], SATResult.UNKNOWN)
        self.assertEqual(fake.params, {'timeout': 1000})


class SolveTest(unittest.TestCase):
    """Z3Handler.solve picks the tactic for a query with FP, unless it is switched off."""

    gids = itertools.count(1_000_000)

    def solve(self, constraint, path=()):
        # The constraint cache keeps a node's constraint by gid: one per test
        gid = next(self.gids)
        node = SimpleNamespace(id=gid, gid=gid, trace_id='t', constraint={'t': constraint})
        return Z3Handler.solve(node, list(path), timeout_ms=None)

    def tactic_used(self, constraint, path=(), enabled=True):
        db = Database.instance()
        before = getattr(db, 'fp_tactic', True)
        db.fp_tactic = enabled
        try:
            with mock.patch.object(sh.z3, 'Tactic', wraps=z3.Tactic) as tactic:
                res, sol = self.solve(constraint, path)
        finally:
            db.fp_tactic = before
        return tactic.called, res, sol

    def test_fp_query_uses_the_tactic(self):
        used, res, sol = self.tactic_used(DECLS + '(assert (fp.isNaN D_0))')
        self.assertTrue(used)
        self.assertEqual(res, SATResult.SAT)
        self.assertEqual(set(sol), {'D_0'})

    def test_fp_in_the_path_only(self):
        used, res, _ = self.tactic_used(DECLS + '(assert (bvslt I_4 #x00000000))', [DECLS + '(assert (fp.isNaN D_0))'])
        self.assertTrue(used)
        self.assertEqual(res, SATResult.SAT)

    def test_query_without_fp_does_not(self):
        used, res, _ = self.tactic_used(DECLS + '(assert (bvslt I_4 #x00000000))')
        self.assertFalse(used)
        self.assertEqual(res, SATResult.SAT)

    def test_switched_off(self):
        used, res, sol = self.tactic_used(DECLS + '(assert (fp.isNaN D_0))', enabled=False)
        self.assertFalse(used)
        self.assertEqual(res, SATResult.SAT)
        self.assertEqual(set(sol), {'D_0'})


if __name__ == '__main__':
    unittest.main()
