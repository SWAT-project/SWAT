import re
from svcomp.SymbolicStorage import DataTypes
from z3 import Solver, is_true, Not, Optimize, Z3_OP_UNINTERPRETED, Abs
from enum import Enum
from typing import Union, Dict, Tuple, Any


class SATResult(Enum):
    SAT = 'sat'
    UNSAT = 'unsat'
    UNKNOWN = 'unknown'
    
class Z3Handler:
    """Class to handle symbolic solving and related tasks."""

    @staticmethod
    def encode_value(prefix: str, value: Any) -> str:
        """
        Encode a Z3 value based on its prefix type.

        Args:
        - prefix (str): The data type prefix (from DataTypes enum).
        - value (Any): The Z3 value to encode.

        Returns:
        - str: The encoded value.
        """
        if prefix == DataTypes.BOOLEAN.value:
            return "true" if is_true(value) else "false"
        elif prefix in {DataTypes.CHAR.value, DataTypes.BYTE.value, DataTypes.SHORT.value, DataTypes.INT.value, DataTypes.LONG.value}:
            return str(value.as_long())
        elif prefix == DataTypes.FLOAT.value:
            # Extract sign, exponent, and significand from the Z3 float value
            sign = value.sign_as_bv().as_long()
            exponent = value.exponent_as_long()
            significand = value.significand_as_long()
            # Shift and combine
            return str((sign << 31) | (exponent << 23) | significand)
        
        elif prefix == DataTypes.DOUBLE.value:
            # Extract sign, exponent, and significand from the Z3 float value
            sign = value.sign_as_bv().as_long()
            exponent = value.exponent_as_long()
            significand = value.significand_as_long()
            
            # Shift and combine
            return str((sign << 63) | (exponent << 52) | significand)
        elif prefix == DataTypes.STRING.value:
            return value.as_string()
        else:
            raise ValueError(f"Unsupported prefix {prefix} for value {value}")


    @staticmethod
    def extract_and_encode_values(model: Any) -> Dict[str, Dict[str, Union[str, int]]]:
        """
        Extract and encode values from a Z3 model.

        Args:
        - model (Any): The Z3 model.

        Returns:
        - Dict[str, Dict[str, Union[str, int]]]: Dictionary of encoded values.
        """
        encoded_values = {}
        for var_decl in model.decls():
            # Extract the variable (as an expression) and its value
            name = str(var_decl.name())
            if not re.match(r'([\w\/]+)_(\d+)', name):
                continue
            value = model[var_decl]
            
            prefix, idx = name.split('_')
            
            encoded_values[name] = {
                'encoded_value': Z3Handler.encode_value(prefix, value),
                'plain_value': value, 
                'index': idx
            }
        return encoded_values


    @staticmethod
    def string_to_expr(s: str) -> Any:
        """
        Convert a string constraint to a Z3 expression.

        Args:
        - s (str): String representation of the constraint.

        Returns:
        - Any: Z3 expression corresponding to the string constraint.
        """
        solver = Solver()
        s = s.replace('\\', '')
        solver.from_string(s)
        return solver.assertions()[0]


        
    @staticmethod
    def solve_opt(node: Any, path_constraints: list) -> Tuple[SATResult, Dict[str, Any]]:
        """
        Solve for the given node and path constraints, 
        trying to minimize the absolute values of the variables.

        Args:
        - node (Any): The node for which to solve.
        - path_constraints (list): List of path constraints.

        Returns:
        - Tuple[bool, Union[Dict[str, Any], None]]: Tuple containing a boolean indicating if the solution is satisfiable, 
                                                    and the solution dictionary or None.
        """
        c = Not(Z3Handler.string_to_expr(node.constraint[node.trace_id]))

        optimizer = Optimize()
        optimizer.add(c)

        for path_constraint in path_constraints:
            p = Z3Handler.string_to_expr(path_constraint)
            optimizer.add(p)

        seen_vars = set()
        for assertion in optimizer.assertions():
            for var in assertion.children():
                if var.decl().kind() == Z3_OP_UNINTERPRETED and var not in seen_vars:
                    seen_vars.add(var)
                    if var.sort().name() in ["Int", "Real"]:
                        optimizer.minimize(Abs(var))

        optimizer.set("timeout", 60 * 1000) 
        res = optimizer.check()

        if str(res) == SATResult.SAT.value:
            sol = optimizer.model()
            encoded_sol = Z3Handler.extract_and_encode_values(sol)
            return SATResult.SAT, encoded_sol
        else:
            return SATResult.UNSAT if str(res) == SATResult.UNSAT.value else SATResult.UNKNOWN, {}