
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


class ConstraintRequest(BaseModel):
    trace: List[TraceItem]
    inputs: List[InputItem]
    ufs: List[UFItem]
    missingInvocations: List[InvocationItem] = []
    symbolicContextLoss: bool
    symbolicPrecisionLoss: bool
    referenceSemanticChange: bool = False


class CoverageRequest(BaseModel):
    ids: List[int]
    total: int
