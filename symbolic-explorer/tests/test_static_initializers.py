import unittest

from data.trace.Branch import Branch
from data.trace.Special import Special, CLINIT, CLINIT_END
from data.trace.StaticInitializers import compose_deltas, strip_static_initializers


def branch(id, pops=0, pushes=()):
    return Branch(id, 't', False, '(assert true)', pops=pops, pushes=tuple(pushes))


def special(id, inst):
    return Special(id, 't', False, inst)


class StripStaticInitializersTest(unittest.TestCase):
    def test_compose(self):
        self.assertEqual(compose_deltas((0, ('a', 'b')), (1, ('c',))), (0, ('a', 'c')))
        self.assertEqual(compose_deltas((1, ('a',)), (3, ())), (3, ()))
        self.assertEqual(compose_deltas((0, ()), (2, ('x',))), (2, ('x',)))

    def test_initializer_left_out_and_its_frames_carried_over(self):
        # b1 in the endpoint; b2 inside <clinit> (one frame pushed); b3 in a callee after the
        # initializer returned (its frame popped, the callee's pushed)
        trace = [branch(1), special(10, CLINIT), branch(2, 0, ('clinit',)), special(11, CLINIT_END),
                 branch(3, 1, ('callee',))]
        out = strip_static_initializers(trace)
        self.assertEqual([e.id for e in out], [1, 3])
        self.assertEqual((out[1].pops, out[1].pushes), (0, ('callee',)))

    def test_nested_initializers(self):
        trace = [special(10, CLINIT), branch(2, 0, ('c1',)), special(12, CLINIT), branch(4, 0, ('c2',)),
                 special(13, CLINIT_END), special(11, CLINIT_END), branch(5, 2, ())]
        out = strip_static_initializers(trace)
        self.assertEqual([e.id for e in out], [5])
        self.assertEqual((out[0].pops, out[0].pushes), (0, ()))

    def test_initializer_left_by_an_exception(self):
        trace = [branch(1), special(10, CLINIT), branch(2)]
        self.assertEqual([e.id for e in strip_static_initializers(trace)], [1])


if __name__ == '__main__':
    unittest.main()
