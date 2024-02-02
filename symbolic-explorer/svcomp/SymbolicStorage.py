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
    STRING = 'Ljava/lang/String'


class SymbolicStorage:
    def __init__(self) -> None:
        self.vars = {}
        self.input_type = None

    def register_vars(self, vars: Dict[str, Input]) -> None:
        for var in vars:
            v = SymbolicVar(identifier=var.name, value=var.value)
            self.vars[v.idx] = v


    def store_solution(self, sol: dict):
        for s in sol.values():
            self.vars[s['index']].newValue = s['encoded_value']

                
            

class SymbolicVar:
    def __init__(self, identifier: str, value: Any = None, newValue: Any = None ) -> None:
        match = re.match(r'([\w\/]+)_(\d+)', identifier)
        self.dType = DataTypes(match[1])
        self.idx = match[2]
        self.value = value
        self.newValue = newValue
    
    def __str__(self) -> str:
        return f'{self.dType.name}_{self.idx} = {self.value}'
