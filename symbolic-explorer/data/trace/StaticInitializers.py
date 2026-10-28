"""
Static initializers in the traces of a long-running target.

A JVM runs a class's <clinit> once, so in a target that serves many requests (http mode) only the
first trace to use a class contains its static initializer (CLINIT ... INVOKECLINIT_END), and no
later run can repeat it. Left in, the traces disagree at that depth without a branch to tell them
apart, so the tree drops the rest of every later trace there (Tree.add, tree_mismatch). The
initializer's branches cannot be explored by re-running either, and the SA graph does not have them
(dfs masks them), so they are taken out of the trace.
"""

from data.trace.Branch import Branch
from data.trace.Special import Special, CLINIT, CLINIT_END


def compose_deltas(first: tuple[int, tuple], second: tuple[int, tuple]) -> tuple[int, tuple]:
    """The stack change (frames popped, keys pushed) of `first` followed by `second`."""
    pops1, pushes1 = first
    pops2, pushes2 = second
    kept = len(pushes1) - min(pops2, len(pushes1))
    return pops1 + max(0, pops2 - len(pushes1)), tuple(pushes1[:kept]) + tuple(pushes2)


def strip_static_initializers(trace: list) -> list:
    """
    The trace without its static initializers (nested ones included). The stack change of the
    branches taken out is carried over to the next branch kept, so that the call stack the SA walk
    rebuilds from the deltas stays right. A <clinit> left by an exception has no end marker; its
    rest is left out up to the end of the trace.
    """
    out = []
    depth = 0
    carried = (0, ())  # stack change of the branches left out since the last kept one
    for element in trace:
        inst = getattr(element, 'inst', None) if isinstance(element, Special) else None
        if inst == CLINIT:
            depth += 1
            continue
        if inst == CLINIT_END and depth > 0:
            depth -= 1
            continue
        if depth > 0:
            if isinstance(element, Branch):
                carried = compose_deltas(carried, (element.pops or 0, tuple(element.pushes or ())))
            continue
        if isinstance(element, Branch) and carried != (0, ()):
            element.pops, element.pushes = compose_deltas(carried, (element.pops or 0, tuple(element.pushes or ())))
            carried = (0, ())
        out.append(element)
    return out
