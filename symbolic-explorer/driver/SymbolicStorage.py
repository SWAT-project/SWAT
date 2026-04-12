import re
from data.trace.Input import Input
from z3 import Solver, is_true, Not
from enum import Enum
from typing import Union, Dict, Tuple, Any

import log
logger = log.get_logger()

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
    LIST = 'Ljava/util/List;'


class SymbolicStorage:

    def __init__(self) -> None:
        self.vars = {}
        self.input_type = None

    def register_vars(self, sym_vars: [str], indices: [int] = None) -> None:
        """Register symbolic variables with their actual indices from DSE.
        
        Args:
            sym_vars: List of variable type strings (e.g., ['I', 'F', 'java/lang/String'])
            indices: List of actual indices from DSE (e.g., [5, 10, 15]). If None, uses enumerate starting from 0.
        """
        if indices is None:
            # Fallback to enumerate for backward compatibility
            indices = list(range(len(sym_vars)))
        
        if len(sym_vars) != len(indices):
            raise ValueError(f"Length mismatch: sym_vars has {len(sym_vars)} elements, indices has {len(indices)}")
        
        for var, idx in zip(sym_vars, indices):
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
        """Store solution from solver, handling both simple and list element variables."""
        # Store the raw solution for list element access
        self.raw_solution = sol

        # Update registered variables with new values
        for var_name, var_data in sol.items():
            idx = int(var_data['index'])
            if idx in self.vars:
                self.vars[idx].newValue = var_data['encoded_value']


class SymbolicVar:
    def __init__(self, dtype: DataTypes, idx: int) -> None:
        self.dType = dtype
        self.idx = idx
        self.value = None
        self.newValue = None

    def __str__(self) -> str:
        return f'{self.dType.name}_{self.idx} = {self.value} -> {self.newValue}'
