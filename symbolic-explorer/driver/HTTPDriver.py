import os
import re
import json
import copy
import signal
import requests
from enum import Enum
from typing import Dict, List, Tuple, Optional, Any

from data.Database import Database
from driver.SymbolicStorage import SymbolicStorage, DataTypes
from solver.SolverHandler import SATResult
from strategy.StrategyService import StrategyService

import log
logger = log.get_logger()


class ExecutionStatus(Enum):
    SUCCESS = 1
    ERROR = 2
    TIMEOUT = 3
    CRASH = 4
    VIOLATION = 5


class Verdict(Enum):
    VIOLATION = "== ERROR"
    SAFE = "== OK"
    UNKNOWN = "== DONT-KNOW"
    NO_SYMBOLIC_VARS = "== NON-SYMBOLIC"


class Action(Enum):
    RANDOMNEXT = 1
    SYMBOLICNEXT = 2
    REPORTVERDICT = 3


class State:
    def __init__(self):
        self.verdict = Verdict.UNKNOWN


class HTTPDriver:
    def __init__(self, args):
        self.state = State()
        self.sym_storage = SymbolicStorage()
        self.sym_storage.raw_solution = {}  # Initialize raw solution storage
        self.endpoint_id = None
        self.args = args
        self.base_url = f"http://{args.host}:{args.http_port}"

        # Parse the target specification (METHOD|URL|BODY)
        self._parse_target_specification(args.url_template)

        # Parse templates for URL and body
        self.parsed_url_vars = self._parse_url_template()
        self.parsed_body_vars = self._parse_json_template(self.body_template) if self.body_template else []

    def _parse_target_specification(self, target_spec: str):
        """Parse the target specification format: METHOD|URL|BODY"""
        parts = target_spec.split('|', 2)

        if len(parts) >= 2:
            # Format: METHOD|URL or METHOD|URL|BODY
            self.http_method = parts[0].upper()
            self.url_template = parts[1]
            self.body_template = json.loads(parts[2]) if len(parts) > 2 else None
        else:
            # Legacy format: just URL (defaults to GET)
            self.http_method = self.args.http_method.upper() if hasattr(self.args, 'http_method') else 'GET'
            self.url_template = target_spec
            self.body_template = None

        logger.info(f'[HTTP DRIVER] Method: {self.http_method}')
        logger.info(f'[HTTP DRIVER] URL Template: {self.url_template}')
        if self.body_template:
            logger.info(f'[HTTP DRIVER] Body Template: {json.dumps(self.body_template)}')

    def _get_type_mapping(self) -> Dict[str, DataTypes]:
        """Get mapping from type strings to DataTypes."""
        return {
            'int': DataTypes.INT,
            'float': DataTypes.FLOAT,
            'double': DataTypes.DOUBLE,
            'string': DataTypes.STRING,
            'boolean': DataTypes.BOOLEAN,
            'bool': DataTypes.BOOLEAN,  # Allow both boolean and bool
            'char': DataTypes.CHAR,
            'byte': DataTypes.BYTE,
            'short': DataTypes.SHORT,
            'long': DataTypes.LONG
        }

    def _parse_url_template(self) -> List[Dict]:
        """Parse URL template to extract symbolic variable placeholders."""
        # Pattern for list types with optional size: <list[element_type,size]_uid> or <list[element_type]_uid>
        list_pattern = r'<list\[(\w+)(?:,(\d+))?\]_(\d+)>'
        # Pattern for simple types: <type_uid>
        simple_pattern = r'<(\w+)_(\d+)>'

        variables = []
        type_mapping = self._get_type_mapping()

        # First, find all list declarations
        list_matches = re.findall(list_pattern, self.url_template)
        for element_type_str, size_str, idx_str in list_matches:
            idx = int(idx_str)
            initial_size = int(size_str) if size_str else 0  # Default to 0 if not specified

            if element_type_str not in type_mapping:
                raise ValueError(f"Unsupported list element type '{element_type_str}' in URL template")

            # Construct the original placeholder
            if size_str:
                placeholder = f'<list[{element_type_str},{size_str}]_{idx}>'
            else:
                placeholder = f'<list[{element_type_str}]_{idx}>'

            variables.append({
                'type': DataTypes.LIST,
                'element_type': type_mapping[element_type_str],
                'index': idx,
                'placeholder': placeholder,
                'source': 'url',
                'param_name': None,  # Will be extracted from URL context
                'initial_size': initial_size
            })

        # Then find simple (non-list) types
        # Remove list patterns first to avoid matching them
        url_without_lists = re.sub(list_pattern, '', self.url_template)
        simple_matches = re.findall(simple_pattern, url_without_lists)

        for type_str, idx_str in simple_matches:
            idx = int(idx_str)

            if type_str not in type_mapping:
                raise ValueError(f"Unsupported type '{type_str}' in URL template")

            variables.append({
                'type': type_mapping[type_str],
                'index': idx,
                'placeholder': f'<{type_str}_{idx}>',
                'source': 'url'
            })

        logger.info(f'[HTTP DRIVER] Parsed {len(variables)} variables from URL template')
        for var in variables:
            if var['type'] == DataTypes.LIST:
                size_info = f" (initial size: {var['initial_size']})" if var.get('initial_size', 0) > 0 else ""
                logger.info(f'[HTTP DRIVER] URL Variable: List[{var["element_type"].name}]_{var["index"]}{size_info}')
            else:
                logger.info(f'[HTTP DRIVER] URL Variable: {var["type"].name}_{var["index"]}')

        return variables

    def _parse_json_template(self, template: Any, path: str = "") -> List[Dict]:
        """Recursively parse JSON template to extract symbolic variable placeholders."""
        variables = []
        list_pattern = r'<list\[(\w+)(?:,(\d+))?\]_(\d+)>'
        simple_pattern = r'<(\w+)_(\d+)>'
        type_mapping = self._get_type_mapping()

        def extract_vars(obj, current_path=""):
            if isinstance(obj, str):
                # Check for list pattern first
                list_match = re.match(list_pattern, obj)
                if list_match:
                    element_type_str, size_str, idx_str = list_match.groups()
                    idx = int(idx_str)
                    initial_size = int(size_str) if size_str else 0

                    if element_type_str not in type_mapping:
                        raise ValueError(f"Unsupported list element type '{element_type_str}' in JSON template")

                    variables.append({
                        'type': DataTypes.LIST,
                        'element_type': type_mapping[element_type_str],
                        'index': idx,
                        'placeholder': obj,
                        'path': current_path,
                        'source': 'body',
                        'initial_size': initial_size
                    })
                    return

                # Then check for simple pattern
                simple_match = re.match(simple_pattern, obj)
                if simple_match:
                    type_str, idx_str = simple_match.groups()
                    idx = int(idx_str)

                    if type_str not in type_mapping:
                        raise ValueError(f"Unsupported type '{type_str}' in JSON template")

                    variables.append({
                        'type': type_mapping[type_str],
                        'index': idx,
                        'placeholder': obj,
                        'path': current_path,
                        'source': 'body'
                    })
            elif isinstance(obj, dict):
                for key, value in obj.items():
                    new_path = f"{current_path}.{key}" if current_path else key
                    extract_vars(value, new_path)
            elif isinstance(obj, list):
                for i, item in enumerate(obj):
                    extract_vars(item, f"{current_path}[{i}]")

        extract_vars(template)

        logger.info(f'[HTTP DRIVER] Parsed {len(variables)} variables from JSON body template')
        for var in variables:
            if var['type'] == DataTypes.LIST:
                size_info = f" (initial size: {var['initial_size']})" if var.get('initial_size', 0) > 0 else ""
                logger.info(f'[HTTP DRIVER] Body Variable: List[{var["element_type"].name}]_{var["index"]}{size_info} at path: {var["path"]}')
            else:
                logger.info(f'[HTTP DRIVER] Body Variable: {var["type"].name}_{var["index"]} at path: {var["path"]}')

        return variables

    def _register_symbolic_vars(self):
        """Register symbolic variables from both URL and body templates."""
        all_vars = self.parsed_url_vars + self.parsed_body_vars

        # Collect unique variables (by index)
        unique_vars = {}
        for var in all_vars:
            idx = var['index']
            if idx not in unique_vars:
                unique_vars[idx] = var

        # Register all unique variables (excluding LIST types - they're registered dynamically at runtime)
        non_list_vars = {k: v for k, v in unique_vars.items() if v['type'] != DataTypes.LIST}

        var_types = [var['type'].value for var in non_list_vars.values()]
        var_indices = list(non_list_vars.keys())

        if var_types:
            self.sym_storage.register_vars(var_types, var_indices)
            self.sym_storage.init_values()

        logger.info(f'[HTTP DRIVER] Registered {len(non_list_vars)} unique symbolic variables')

        # Track list variables separately for URL building
        self.list_vars = {k: v for k, v in unique_vars.items() if v['type'] == DataTypes.LIST}
        if self.list_vars:
            logger.info(f'[HTTP DRIVER] Found {len(self.list_vars)} list variables (elements registered at runtime)')

    def _decode_numeric_value(self, value: Any, data_type: DataTypes) -> Any:
        """Decode numeric values from solver encoding to proper Python types."""
        if data_type == DataTypes.DOUBLE:
            # Value is encoded as a 64-bit integer, decode it back to float
            if isinstance(value, str):
                value = int(value)
            if isinstance(value, int):
                import struct
                # Convert the long integer back to double using IEEE 754 format
                # Use unsigned interpretation (Q) since bit patterns can be > 2^63-1
                # Handle negative values by converting to unsigned 64-bit range
                if value < 0:
                    value = (1 << 64) + value  # Convert to unsigned equivalent
                return struct.unpack('d', struct.pack('Q', value))[0]
        elif data_type == DataTypes.FLOAT:
            # Value is encoded as a 32-bit integer, decode it back to float
            if isinstance(value, str):
                value = int(value)
            if isinstance(value, int):
                import struct
                # Convert the integer back to float using IEEE 754 format
                # Use unsigned interpretation (I) since bit patterns can be > 2^31-1
                if value < 0:
                    value = (1 << 32) + value  # Convert to unsigned equivalent
                return struct.unpack('f', struct.pack('I', value))[0]
        return value

    def _build_url(self) -> str:
        """Build the actual URL by substituting symbolic values."""
        import urllib.parse

        url = self.url_template

        logger.debug(f'[HTTP DRIVER] Building URL from template: {self.url_template}')
        logger.debug(f'[HTTP DRIVER] Available variables: {list(self.sym_storage.vars.keys())}')

        # First, handle list variables
        for var in self.parsed_url_vars:
            if var['type'] != DataTypes.LIST:
                continue

            idx = var['index']
            placeholder = var['placeholder']

            # Extract the parameter name from the URL (e.g., "nums" from "?nums=<list[int]_20>")
            # Look for pattern: paramName=<list[...]_uid>
            param_pattern = rf'(\w+)={re.escape(placeholder)}'
            param_match = re.search(param_pattern, url)

            if not param_match:
                logger.warning(f'[HTTP DRIVER] Could not extract parameter name for list variable {idx}')
                continue

            param_name = param_match.group(1)

            # Find all list element variables matching List_{idx}_*
            # The solver returns variable names like "List_20_I_0", "List_20_I_1", etc.
            list_elements = []
            element_type = var['element_type']

            # Get the type prefix for the element type
            # Note: For STRING elements, the instrumentation uses the full type name
            type_prefix_map = {
                DataTypes.INT: 'I',
                DataTypes.LONG: 'J',
                DataTypes.DOUBLE: 'D',
                DataTypes.FLOAT: 'F',
                DataTypes.STRING: 'java/lang/String',
                DataTypes.BOOLEAN: 'Z',
                DataTypes.BYTE: 'B',
                DataTypes.SHORT: 'S',
                DataTypes.CHAR: 'C'
            }

            type_prefix = type_prefix_map.get(element_type, 'I')

            # Collect list elements from raw solution
            # Pattern: List_{parent_uid}_{type_prefix}_{element_index}
            # For strings, we need to escape special regex characters
            escaped_prefix = re.escape(type_prefix) if '/' in type_prefix else type_prefix
            list_pattern = rf'List_{idx}_{escaped_prefix}_(\d+)'

            matching_elements = {}
            for var_name, var_data in self.sym_storage.raw_solution.items():
                match = re.match(list_pattern, var_name)
                if match:
                    element_idx = int(match.group(1))
                    # Decode the value based on element type
                    value = self._decode_numeric_value(var_data['plain_value'], element_type)
                    matching_elements[element_idx] = value
                    logger.debug(f'[HTTP DRIVER] Found list element: {var_name} = {value}')

            # Sort by element index and build list
            if matching_elements:
                sorted_indices = sorted(matching_elements.keys())
                list_elements = [matching_elements[i] for i in sorted_indices]
                logger.info(f'[HTTP DRIVER] List variable {idx} has {len(list_elements)} elements: {list_elements}')

            # Build the repeated parameter string
            if list_elements:
                # URL encode values if needed
                if element_type == DataTypes.STRING:
                    encoded_elements = [urllib.parse.quote(str(elem)) for elem in list_elements]
                else:
                    encoded_elements = [str(elem) for elem in list_elements]

                repeated_params = '&'.join([f'{param_name}={elem}' for elem in encoded_elements])
                url = url.replace(f'{param_name}={placeholder}', repeated_params)
            else:
                # No elements found yet (initial run), use default initial values
                initial_size = var.get('initial_size', 0)
                if initial_size > 0:
                    # Generate default initial values based on type
                    default_values = []
                    for i in range(initial_size):
                        if element_type == DataTypes.STRING:
                            default_values.append(f'x{i}')
                        elif element_type == DataTypes.BOOLEAN:
                            default_values.append('false')
                        elif element_type in [DataTypes.INT, DataTypes.LONG, DataTypes.SHORT, DataTypes.BYTE]:
                            default_values.append(str(i))
                        elif element_type in [DataTypes.FLOAT, DataTypes.DOUBLE]:
                            default_values.append(f'{float(i)}.0')
                        elif element_type == DataTypes.CHAR:
                            default_values.append('a')
                        else:
                            default_values.append('0')

                    # URL encode if needed
                    if element_type == DataTypes.STRING:
                        encoded_values = [urllib.parse.quote(v) for v in default_values]
                    else:
                        encoded_values = default_values

                    repeated_params = '&'.join([f'{param_name}={v}' for v in encoded_values])
                    url = url.replace(f'{param_name}={placeholder}', repeated_params)
                    logger.debug(f'[HTTP DRIVER] Using {initial_size} default initial values for list variable {idx}')
                else:
                    # No initial size specified, remove parameter entirely
                    url = re.sub(rf'{param_name}={re.escape(placeholder)}&?', '', url)
                    url = url.rstrip('&')
                    logger.debug(f'[HTTP DRIVER] No elements found for list variable {idx}, using empty')

        # Then handle simple variables
        for var in self.parsed_url_vars:
            if var['type'] == DataTypes.LIST:
                continue  # Already handled

            idx = var['index']
            placeholder = var['placeholder']

            if idx in self.sym_storage.vars:
                sym_var = self.sym_storage.vars[idx]
                # Get the current value, handling None properly
                if sym_var.newValue is not None:
                    value = sym_var.newValue
                    # Decode numeric values if needed
                    value = self._decode_numeric_value(value, var['type'])
                    # Update the base value for consistency
                    sym_var.value = value
                else:
                    value = sym_var.value

                logger.debug(f'[HTTP DRIVER] Variable {idx}: value={value}, newValue={sym_var.newValue}')

                # Convert value to string format appropriate for URL
                if var['type'] == DataTypes.STRING:
                    # URL encode string values
                    value = urllib.parse.quote(str(value))
                else:
                    value = str(value)

                url = url.replace(placeholder, value)
            else:
                logger.warning(f'[HTTP DRIVER] Symbolic variable {idx} not found in storage')

        logger.debug(f'[HTTP DRIVER] Built URL: {self.base_url + url}')
        return self.base_url + url

    def _build_json_body(self) -> Optional[Dict]:
        """Build JSON body by substituting symbolic values."""
        if not self.body_template:
            return None

        body = copy.deepcopy(self.body_template)

        def substitute_vars(obj):
            if isinstance(obj, str):
                # Check for list pattern first
                list_pattern = r'^<list\[(\w+)\]_(\d+)>$'
                list_match = re.match(list_pattern, obj)
                if list_match:
                    element_type_str, idx_str = list_match.groups()
                    idx = int(idx_str)

                    # Find the element type from parsed_body_vars
                    element_type = None
                    for var in self.parsed_body_vars:
                        if var['index'] == idx and var['type'] == DataTypes.LIST:
                            element_type = var['element_type']
                            break

                    if not element_type:
                        logger.warning(f'[HTTP DRIVER] List variable {idx} not found in parsed body vars')
                        return []

                    # Get the type prefix for collecting list elements
                    type_prefix_map = {
                        DataTypes.INT: 'I',
                        DataTypes.LONG: 'J',
                        DataTypes.DOUBLE: 'D',
                        DataTypes.FLOAT: 'F',
                        DataTypes.STRING: 'java/lang/String',
                        DataTypes.BOOLEAN: 'Z',
                        DataTypes.BYTE: 'B',
                        DataTypes.SHORT: 'S',
                        DataTypes.CHAR: 'C'
                    }

                    type_prefix = type_prefix_map.get(element_type, 'I')
                    escaped_prefix = re.escape(type_prefix) if '/' in type_prefix else type_prefix
                    list_pattern_match = rf'List_{idx}_{escaped_prefix}_(\d+)'

                    # Collect list elements from raw solution
                    matching_elements = {}
                    for var_name, var_data in self.sym_storage.raw_solution.items():
                        match = re.match(list_pattern_match, var_name)
                        if match:
                            element_idx = int(match.group(1))
                            value = self._decode_numeric_value(var_data['plain_value'], element_type)
                            matching_elements[element_idx] = value

                    # Sort by element index and build list
                    if matching_elements:
                        sorted_indices = sorted(matching_elements.keys())
                        list_elements = [matching_elements[i] for i in sorted_indices]
                        logger.debug(f'[HTTP DRIVER] List variable {idx} in body has {len(list_elements)} elements')
                        return list_elements
                    else:
                        # No elements from solver, use default initial values if specified
                        initial_size = 0
                        for var in self.parsed_body_vars:
                            if var['index'] == idx and var['type'] == DataTypes.LIST:
                                initial_size = var.get('initial_size', 0)
                                break

                        if initial_size > 0:
                            # Generate default values based on element type
                            default_list = []
                            for i in range(initial_size):
                                if element_type == DataTypes.STRING:
                                    default_list.append(f'x{i}')
                                elif element_type == DataTypes.BOOLEAN:
                                    default_list.append(False)
                                elif element_type in [DataTypes.INT, DataTypes.LONG, DataTypes.SHORT, DataTypes.BYTE]:
                                    default_list.append(i)
                                elif element_type in [DataTypes.FLOAT, DataTypes.DOUBLE]:
                                    default_list.append(float(i))
                                elif element_type == DataTypes.CHAR:
                                    default_list.append('a')
                                else:
                                    default_list.append(0)
                            logger.debug(f'[HTTP DRIVER] Using {initial_size} default initial values for list variable {idx} in body')
                            return default_list
                        else:
                            return []

                # Check for simple pattern
                simple_pattern = r'^<(\w+)_(\d+)>$'
                simple_match = re.match(simple_pattern, obj)
                if simple_match:
                    type_str, idx_str = simple_match.groups()
                    idx = int(idx_str)
                    if idx in self.sym_storage.vars:
                        sym_var = self.sym_storage.vars[idx]
                        value = sym_var.newValue if sym_var.newValue is not None else sym_var.value

                        # Find the variable type from parsed_body_vars
                        var_type = None
                        for var in self.parsed_body_vars:
                            if var['index'] == idx:
                                var_type = var['type']
                                break

                        # Decode numeric values if we know the type
                        if var_type:
                            value = self._decode_numeric_value(value, var_type)

                        # Return the actual value (not stringified)
                        return value
                return obj
            elif isinstance(obj, dict):
                return {k: substitute_vars(v) for k, v in obj.items()}
            elif isinstance(obj, list):
                return [substitute_vars(item) for item in obj]
            return obj

        result = substitute_vars(body)
        logger.debug(f'[HTTP DRIVER] Built JSON body: {json.dumps(result)}')
        return result

    def send_http_request(self, timeout: int = 60) -> Tuple[ExecutionStatus, Dict]:
        """Send HTTP request to the target and return status and response."""
        url = self._build_url()
        json_body = self._build_json_body()

        logger.info(f'[HTTP DRIVER] Sending {self.http_method} request to: {url}')
        if json_body:
            logger.info(f'[HTTP DRIVER] Request body: {json.dumps(json_body)}')

        try:
            # Prepare headers
            headers = {}
            if json_body:
                headers['Content-Type'] = 'application/json'

            # Send request based on method
            if self.http_method == 'GET':
                response = requests.get(url, timeout=timeout, headers=headers)
            elif self.http_method == 'POST':
                if json_body:
                    response = requests.post(url, json=json_body, timeout=timeout, headers=headers)
                else:
                    response = requests.post(url, timeout=timeout, headers=headers)
            elif self.http_method == 'PUT':
                if json_body:
                    response = requests.put(url, json=json_body, timeout=timeout, headers=headers)
                else:
                    response = requests.put(url, timeout=timeout, headers=headers)
            elif self.http_method == 'DELETE':
                if json_body:
                    response = requests.delete(url, json=json_body, timeout=timeout, headers=headers)
                else:
                    response = requests.delete(url, timeout=timeout, headers=headers)
            elif self.http_method == 'PATCH':
                if json_body:
                    response = requests.patch(url, json=json_body, timeout=timeout, headers=headers)
                else:
                    response = requests.patch(url, timeout=timeout, headers=headers)
            else:
                logger.error(f'[HTTP DRIVER] Unsupported HTTP method: {self.http_method}')
                return ExecutionStatus.ERROR, {'error': f'Unsupported method: {self.http_method}'}

            logger.info(f'[HTTP DRIVER] Response status: {response.status_code}')

            response_data = {
                'status_code': response.status_code,
                'headers': dict(response.headers),
                'url': response.url,
                'text': response.text[:1000]  # Limit response text for logging
            }
            
            # Only treat 5xx server errors as ERROR status
            # 4xx client errors are valid responses from the target and should be treated as SUCCESS
            if response.status_code >= 500:
                logger.warning(f'[HTTP DRIVER] Server error: {response.status_code}')
                return ExecutionStatus.ERROR, response_data
            
            # All other responses (1xx, 2xx, 3xx, 4xx) are considered successful communication with target
            if response.status_code >= 400:
                logger.info(f'[HTTP DRIVER] Client error response: {response.status_code} (valid target response)')
            else:
                logger.info(f'[HTTP DRIVER] Successful response: {response.status_code}')
            
            return ExecutionStatus.SUCCESS, response_data
            
        except requests.exceptions.Timeout:
            logger.warning(f'[HTTP DRIVER] Request timeout after {timeout}s')
            return ExecutionStatus.TIMEOUT, {'error': 'timeout'}
        except requests.exceptions.ConnectionError:
            logger.error(f'[HTTP DRIVER] Connection error to {url}')
            return ExecutionStatus.CRASH, {'error': 'connection_error'}
        except Exception as e:
            logger.critical(f'[HTTP DRIVER] Exception during request: {e}')
            return ExecutionStatus.CRASH, {'error': str(e)}

    def record_violation(self):
        """Records the violation in the database."""
        db = Database.instance()
        db.add_violation(endpoint_id=self.endpoint_id, sym_vars=list(self.sym_storage.vars.values()))

    def determine_next_step(self, status: ExecutionStatus, response: Dict) -> Action:
        """Determines the next step based on the HTTP response."""
        match status:
            case ExecutionStatus.SUCCESS:
                # Check response for assertion violations or error indicators
                response_text = response.get('text', '')
                if 'java.lang.AssertionError' in response_text:
                    logger.info(f'[HTTP DRIVER] Assertion error detected in response')
                    self.record_violation()
                    return Action.SYMBOLICNEXT
                elif 'VIOLATION' in response_text or 'ERROR' in response_text:
                    logger.info(f'[HTTP DRIVER] Violation detected in response')
                    self.record_violation()
                    return Action.SYMBOLICNEXT
                return Action.SYMBOLICNEXT
            case ExecutionStatus.VIOLATION:
                self.record_violation()
                logger.info(f'[HTTP DRIVER] Violation recorded!')
                return Action.SYMBOLICNEXT
            case ExecutionStatus.TIMEOUT:
                logger.info(f'[HTTP DRIVER] Timeout!')
                self.state.verdict = Verdict.UNKNOWN
                return Action.REPORTVERDICT
            case ExecutionStatus.CRASH:
                logger.info(f'[HTTP DRIVER] Crash!')
                self.state.verdict = Verdict.UNKNOWN
                return Action.REPORTVERDICT
            case ExecutionStatus.ERROR:
                logger.info(f'[HTTP DRIVER] Error!')
                self.state.verdict = Verdict.UNKNOWN
                return Action.REPORTVERDICT

        raise Exception(f'Unknown execution status: {status}')

    def retrieve_solution(self):
        """Retrieve next solution using symbolic exploration."""
        possible_branches = StrategyService.select_branch(endpoint_id=self.endpoint_id)
        logger.info(f'[HTTP DRIVER] Found {len(possible_branches)} possible branches')
        
        symbolic_vars = None
        sat = None
        branch_found = False
        
        for branch in possible_branches:
            if not StrategyService.is_symbolic_branch(branch):
                continue
            branch_found = True
            sat, sol = StrategyService.solve_branch(branch)

            if sat == SATResult.SAT:
                symbolic_vars = branch.inputs
                break

        if not branch_found or sat == SATResult.UNSAT:
            self.state.verdict = Verdict.SAFE
            logger.info(f'[HTTP DRIVER] No symbolic branch found or UNSAT')
            return Action.REPORTVERDICT

        if sat == SATResult.UNKNOWN:
            logger.info(f'[HTTP DRIVER] SAT result is UNKNOWN')
            self.state.verdict = Verdict.UNKNOWN
            return Action.REPORTVERDICT

        sol_viz = [f'{key}: {val["plain_value"]}' for key, val in sol.items()]
        logger.info(f'[HTTP DRIVER] Found new solution: {sol_viz}')
        # Don't re-register vars, just store the solution
        self.sym_storage.store_solution(sol)
        return Action.SYMBOLICNEXT

    def run(self):
        """Main execution method."""
        verdict = self.exec()
        logger.info(f'[HTTP DRIVER] Verdict: {verdict}')
        self.kill_current_process()

    def exec(self):
        """Runs the symbolic execution using HTTP requests."""
        logger.info(f'[HTTP DRIVER] Beginning HTTP symbolic execution')
        
        # Register symbolic variables from URL template
        self._register_symbolic_vars()
        
        # Main execution loop
        while True:
            # Send HTTP request
            status, response = self.send_http_request()
            
            # Determine the next step
            next_step = self.determine_next_step(status, response)
            
            # Select the endpoint (assuming single endpoint for HTTP targets)
            endpoints = Database.instance().get_endpoints()
            if len(endpoints) == 0:
                # Create endpoint if none exists
                Database.instance().add_endpoint(0)
                self.endpoint_id = 0
            else:
                assert len(endpoints) == 1
                self.endpoint_id = endpoints[0]
            
            if next_step == Action.REPORTVERDICT:
                break

            if next_step == Action.SYMBOLICNEXT:
                logger.info(f'[HTTP DRIVER] Next step: SYMBOLIC EXPLORATION')
                
                next_step = self.retrieve_solution()
                if next_step == Action.REPORTVERDICT:
                    break

        logger.info(f'[HTTP DRIVER] Symbolic execution terminated')
        violations = Database.instance().get_violations(self.endpoint_id)
        logger.info(f'[HTTP DRIVER] Found {len(violations)} violations')
        if len(violations) > 0:
            for v in violations:
                logger.info(f'[HTTP DRIVER] Violation: {[vv.__str__() for vv in v]}')

        return self.state.verdict

    def kill_current_process(self):
        """Terminate the current process."""
        pid = os.getpid()
        os.kill(pid, signal.SIGTERM)