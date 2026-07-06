
from typing import List, Optional

from pydantic import BaseModel


class TraceItem(BaseModel):
    iid: int
    constraint: Optional[str] = None
    branched: bool
    type: str
    inst: Optional[str] = None
    # Executor's per-branch precision-loss verdict. Parsed for forward compatibility; the current
    # verdict still uses the aggregate symbolicPrecisionLoss. A future explorer-side, CFG-reachability
    # -aware decision keys this by iid to a CFG node. Default keeps older traces parseable.
    precisionLoss: bool = False
class UFItem(BaseModel):
    definition: str 


class InputItem(BaseModel):
    name: str
    value: str
    type: str
    lowerBound: str
    upperBound: str


class ConstraintRequest(BaseModel):
    trace: List[TraceItem]
    inputs: List[InputItem]
    ufs: List[UFItem]
    symbolicContextLoss: bool
    symbolicPrecisionLoss: bool


class CoverageRequest(BaseModel):
    ids: List[int]
    total: int
