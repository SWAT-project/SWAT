import re
from data.trace.Input import Input
from z3 import Solver, is_true, Not
from enum import Enum
from typing import Union, Dict, Tuple, Any
class DataTypes(Enum):
    BOOLEAN = 'Z'
    CHAR = 'C'    
    BYTE = 'B'    
    SHORT = 'S'   
    INT = 'I'     
    FLOAT = 'F'    
    LONG = 'J'
    DOUBLE = 'D'
    STRING = 'java/lang/String'


def trace_value_as_input(dtype: DataTypes, value: Any) -> Any:
    """The value of an input as recorded in a trace, in the encoding of -Dswat.input.

    The trace records floating-point values as the hex digits of their bits
    (DoubleValue/FloatValue.getConcreteEncoded), but the executor's Verifier reads them as the
    unsigned decimal number of the bits, as the solver's encoded_value gives them. A value from the
    trace is passed on when the next solution leaves the input out (it is unconstrained there).
    """
    if dtype in (DataTypes.DOUBLE, DataTypes.FLOAT) and isinstance(value, str):
        try:
            return str(int(value, 16))
        except ValueError:
            return value
    return value


class SymbolicStorage:
    def __init__(self) -> None:
        self.vars = {}
        self.input_type = None

    def register_vars(self, vars: Dict[str, Input]) -> None:
        # Keyed by name: inputs of different types can share an index (I_0 and [I_0).
        for var in vars:
            v = SymbolicVar(identifier=var.name, value=var.value)
            v.value = trace_value_as_input(v.dType, v.value)
            self.vars[var.name] = v


    def store_solution(self, sol: dict):
        for name, s in sol.items():
            # The model also names symbolic values that are no Verifier input, e.g. an array
            # ([I_0) the executor made symbolic; those cannot be passed to the program.
            if name in self.vars:
                self.vars[name].newValue = s['encoded_value']

                
            

class SymbolicVar:
    def __init__(self, identifier: str, value: Any = None, newValue: Any = None ) -> None:
        match = re.match(r'([\w\/]+)_(\d+)', identifier)
        self.dType = DataTypes(match[1])
        self.idx = match[2]
        self.value = value
        self.newValue = newValue
    
    def __str__(self) -> str:
        return f'{self.dType.name}_{self.idx} = {self.value}'
