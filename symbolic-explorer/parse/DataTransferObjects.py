
from typing import List, Optional

from pydantic import BaseModel


class TraceItem(BaseModel):
    iid: int
    constraint: Optional[str] = None
    branched: bool
    type: str
    inst: Optional[str] = None
    # A branch the executor records only to mirror a guard of the SA graph; never a solver target.
    concreteOnly: bool = False
    # Where the branch is (see the executor's BranchKeys): its key, and how the frame stack changed
    # since the previous branch -- frames popped, then the call sites (keys) of the frames pushed.
    key: Optional[str] = None
    pops: int = 0
    pushes: list[Optional[str]] = []
    # Recorded in application code that library code called back: not in the SA graph.
    callback: bool = False
    # A branch the executor adds to enumerate the values of a symbolic operand (e.g. the element a
    # symbolic array index selects): flipped like any other, but not in the SA graph.
    choice: bool = False
    # Hashes of the loop-head states the execution reached since the previous branch, in order.
    loopStates: list[str] = []
class UFItem(BaseModel):
    definition: str 


class InputItem(BaseModel):
    name: str
    value: str
    type: str
    lowerBound: str
    upperBound: str


class InvocationItem(BaseModel):
    """A method invocation that could not be modelled symbolically during execution.

    The full list is the superset of missing invocations; entries with ``contextLoss`` set are the
    dangerous subset that received symbolic arguments and caused symbolic context loss.
    """
    owner: str
    name: str
    desc: str
    isInstance: bool
    isSymbolic: bool
    contextLoss: bool
    count: int


class NondetReturnItem(BaseModel):
    """A value returned by a ``Verifier.nondet*()`` call and the location of the call.

    ``value`` is the witness string form (integers decimal, char as code point, float/double as
    Java's ``Float/Double.toString``, boolean ``true``/``false``, String raw); ``bits`` carries the
    exact IEEE-754 bits of a float/double as ``0x``-prefixed hex. ``callIndex`` is the 0-based
    index among the nondet calls on that source line of that class, in evaluation order;
    ``method`` is the calling method (name + descriptor).
    """
    function: str
    type: str
    value: str
    bits: Optional[str] = None
    className: str
    # The calling method as name + descriptor, e.g. main([Ljava/lang/String;)V
    method: Optional[str] = None
    sourceFile: str = ""
    line: int
    callIndex: int = 0


class FrameItem(BaseModel):
    """A stack frame of the throwable that escaped the entry method."""
    className: str
    sourceFile: str = ""
    line: int


class ViolationItem(BaseModel):
    """The throwable that escaped the entry method, with its frames innermost first."""
    throwable: str
    frames: List[FrameItem] = []


class ConstraintRequest(BaseModel):
    trace: List[TraceItem]
    inputs: List[InputItem]
    ufs: List[UFItem]
    missingInvocations: List[InvocationItem] = []
    # Witness data: the nondet return values of the execution and the uncaught throwable (if any)
    nondetReturns: List[NondetReturnItem] = []
    violation: Optional[ViolationItem] = None
    symbolicContextLoss: bool
    symbolicPrecisionLoss: bool
    referenceSemanticChange: bool = False
    # An approximate model was used that is precision loss wherever it is used: solutions from this
    # trace are guesses. Queries over sampled Math functions are recognised by their function names.
    approximateModel: bool = False
    # A StackOverflowError reached the target's code: the path depends on the JVM's stack size, so a
    # violation of this execution is not a verdict (the executor also reports context loss).
    stackOverflow: bool = False


class CoverageRequest(BaseModel):
    ids: List[int]
    total: int
