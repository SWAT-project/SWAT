
from typing import List, Optional

from pydantic import BaseModel


class TraceItem(BaseModel):
    iid: int
    constraint: Optional[str] = None
    branched: bool
    stackSize: int
    localsSize: int
    callSize: int
    type: str
    inst: Optional[str] = None

class InputItem(BaseModel):
    name: str
    value: str
    type: str
    lowerBound: str
    upperBound: str

class ConstraintRequest(BaseModel):
    trace: List[TraceItem]
    inputs: List[InputItem]