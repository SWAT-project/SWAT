import re
from data.trace.Input import Input
from z3 import Solver, is_true, Not
from enum import Enum
from typing import Union, Dict, Tuple, Any
from log import logger

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

    def register_vars(self, sym_vars: [str]) -> None:
        for idx, var in enumerate(sym_vars):
            v = SymbolicVar(dtype=DataTypes(var), idx=idx)
            self.vars[int(v.idx)] = v
            logger.info(f'[EXPLORER] Registered symbolic variable {v.dType.name}_{v.idx}')

    def init_values(self):
        for var in self.vars.values():
            match var.dType:
                case DataTypes.BOOLEAN:
                    var.value = False
                case DataTypes.CHAR:
                    var.value = '\u0000'
                case DataTypes.BYTE:
                    var.value = 0
                case DataTypes.SHORT:
                    var.value = 0
                case DataTypes.INT:
                    var.value = 0
                case DataTypes.FLOAT:
                    var.value = 0.0
                case DataTypes.LONG:
                    var.value = 0
                case DataTypes.DOUBLE:
                    var.value = 0.0
                case DataTypes.STRING:
                    var.value = 'x'


    def store_solution(self, sol: dict):
        for s in sol.values():
            self.vars[int(s['index'])].newValue = s['encoded_value']


class SymbolicVar:
    def __init__(self, dtype: DataTypes, idx: int) -> None:
        self.dType = dtype
        self.idx = idx
        self.value = None
        self.newValue = None

    def __str__(self) -> str:
        return f'{self.dType.name}_{self.idx} = {self.value}'
