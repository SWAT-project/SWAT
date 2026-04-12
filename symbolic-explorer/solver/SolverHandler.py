import re
from svcomp.SymbolicStorage import DataTypes
from z3 import Solver, is_true, Not, Optimize, Z3_OP_UNINTERPRETED, Abs
from enum import Enum
from typing import Union, Dict, Tuple, Any
from datetime import datetime
import os


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
            # Check for special values (NaN, Infinity)
            if value.isNaN():
                # IEEE 754: NaN = sign bit (any) | all 1s exponent | non-zero significand
                # Java's Float.NaN = 0x7fc00000 (quiet NaN, positive)
                encoded = 0x7fc00000
                return str(encoded)
            elif value.isInf():
                # IEEE 754: +Infinity = 0x7f800000, -Infinity = 0xff800000
                if value.isPositive():
                    encoded = 0x7f800000
                else:
                    encoded = 0xff800000
                print(f"[DEBUG FLOAT] Z3 value: Infinity -> {encoded:08x}")
                return str(encoded)

            # Extract sign, exponent, and significand from the Z3 float value
            sign = value.sign_as_bv().as_long()
            exponent = value.exponent_as_long()
            significand = value.significand_as_long()
            # Shift and combine
            encoded = (sign << 31) | (exponent << 23) | significand
            return str(encoded)

        elif prefix == DataTypes.DOUBLE.value:
            # Check for special values (NaN, Infinity)
            if value.isNaN():
                # IEEE 754: NaN = sign bit (any) | all 1s exponent | non-zero significand
                # Java's Double.NaN = 0x7ff8000000000000 (quiet NaN, positive)
                encoded = 0x7ff8000000000000
                return str(encoded)
            elif value.isInf():
                # IEEE 754: +Infinity = 0x7ff0000000000000, -Infinity = 0xfff0000000000000
                if value.isPositive():
                    encoded = 0x7ff0000000000000
                else:
                    encoded = 0xfff0000000000000
                return str(encoded)

            # Extract sign, exponent, and significand from the Z3 float value
            sign = value.sign_as_bv().as_long()
            exponent = value.exponent_as_long()
            significand = value.significand_as_long()
            # Shift and combine
            encoded = (sign << 63) | (exponent << 52) | significand

            return str(encoded)
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

            # Handle list element variables: List_{parent_uid}_{type_prefix}_{element_index}
            # Example: List_20_I_0, List_20_I_1, etc.
            if name.startswith('List_'):
                # Parse list element name: List_{parent_uid}_{type_prefix}_{element_index}
                parts = name.split('_')
                if len(parts) >= 4:
                    # parts = ['List', parent_uid, type_prefix, element_index, ...]
                    parent_uid = parts[1]
                    type_prefix = parts[2]
                    element_index = parts[3]

                    # Use the element type prefix for encoding
                    prefix = type_prefix
                    # Use element index as the index (though it's not used for list elements)
                    idx = element_index
                else:
                    # Malformed list variable
                    raise
            else:
                # Regular variable: prefix_index
                parts = name.split('_', 1)  # Split on first underscore only
                if len(parts) == 2:
                    prefix, idx = parts
                else:
                    # Handle edge case where variable might have multiple underscores
                    # Use rsplit to get the last part as index
                    parts = name.rsplit('_', 1)
                    if len(parts) == 2:
                        prefix, idx = parts
                    else:
                        continue

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
    def write_optimizer_to_file(optimizer: Optimize, output_dir: str = "smt_files") -> str:
        """
        Write the optimizer output to an SMT-LIB format file.

        Args:
        - optimizer (Optimize): The Z3 optimizer instance
        - output_dir (str): Directory to store the SMT files

        Returns:
        - str: Path to the created file
        """
        # Create output directory if it doesn't exist
        os.makedirs(output_dir, exist_ok=True)
        
        # Generate filename with timestamp
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"optimizer_{timestamp}.smt2"
        filepath = os.path.join(output_dir, filename)
        
        # Write optimizer output to file
        with open(filepath, 'w') as f:
            f.write(str(optimizer))
            # Add get-model command after check-sat
            f.write("\n(get-model)\n")
            
        return filepath

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
        #print("Constraint: ")
        optimizer = Optimize()
        optimizer.add(c)
        #print(c)

        #print("Path constraints: ")
        for path_constraint in path_constraints:
            p = Z3Handler.string_to_expr(path_constraint)
            optimizer.add(p)
            #print(p)

        seen_vars = set()
        for assertion in optimizer.assertions():
            for var in assertion.children():
                if var.decl().kind() == Z3_OP_UNINTERPRETED and var not in seen_vars:
                    seen_vars.add(var)
                    if var.sort().name() in ["Int", "Real"]:
                        optimizer.minimize(Abs(var))

        optimizer.set("timeout", 60 * 1000)
        #print(optimizer.to_smt2())
        # Save optimizer output to file
        smt_file = Z3Handler.write_optimizer_to_file(optimizer)
        print(f"Saved SMT-LIB file to: {smt_file}")
        
        res = optimizer.check()

        if str(res) == SATResult.SAT.value:
            sol = optimizer.model()
            #print('Problem:')
            #print(optimizer.__repr__())
            #print('Model:')
            #print(sol)
            encoded_sol = Z3Handler.extract_and_encode_values(sol)
            return SATResult.SAT, encoded_sol
        else:
            return SATResult.UNSAT if str(res) == SATResult.UNSAT.value else SATResult.UNKNOWN, {}