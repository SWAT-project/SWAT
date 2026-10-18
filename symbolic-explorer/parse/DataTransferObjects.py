
from typing import List, Optional

from pydantic import BaseModel


class TraceItem(BaseModel):
    iid: int
    constraint: Optional[str] = None
    branched: bool
    type: str
    inst: Optional[str] = None
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


class CoverageRequest(BaseModel):
    ids: List[int]
    total: int
