"""
SimpleDriver - A simple exploration driver for integration tests.

This driver runs a Java target iteratively with symbolic inputs until
all reachable paths have been explored. It's designed for:
- Integration tests
- Small example programs
- Learning/demonstrating symbolic execution

Unlike SVCompDriver (which checks for violations), this just explores all paths.
Unlike PassiveDriver (which only solves, doesn't run), this actively runs Java.
"""

import os
import sys
import subprocess
from typing import List, Tuple
from enum import Enum

from data.Database import Database
from strategy.StrategyService import StrategyService
from solver.SolverHandler import SATResult
from driver.SymbolicStorage import SymbolicStorage

import log
logger = log.get_logger()


class ExecutionStatus(Enum):
    SUCCESS = 1
    ERROR = 2
    TIMEOUT = 3


class SimpleDriver:
    """
    Simple driver that explores all paths in a Java target.

    The exploration loop:
    1. Run Java target (first time with concrete values)
    2. Constraints are sent to explorer via HTTP
    3. Query StrategyService for next unexplored branch
    4. Solve for inputs that will explore that branch
    5. Run Java again with new inputs
    6. Repeat until no more branches to explore
    """

    def __init__(self, args):
        self.args = args
        self.sym_storage = SymbolicStorage()
        self.endpoint_id = None  # Will be set after first execution
        self.iteration = 0

    def build_command(self) -> List[str]:
        """Builds the Java command with symbolic inputs as system properties."""
        cmd = [
            'java',
            f'-Xmx4g',  # Fixed 4GB for simplicity
            f'-Dconfig.path={self.args.config}',
            f'-Dexplorer.port={self.args.port}',
            f'-javaagent:{self.args.agent}',
            f'-Djava.library.path={self.args.z3dir}',
            '-Dsolver.mode=HTTP',
            '-ea',  # Enable assertions
        ]

        # Add symbolic value assignments (if we have them)
        for var in self.sym_storage.vars.values():
            val = var.newValue if var.newValue is not None else var.value
            # Use swat.assignment prefix (read by Intrinsics.retrieveAssignments())
            cmd.insert(1, f'-Dswat.assignment.{var.dType.value}_{var.idx}={val}')

        # Add target
        if self.args.target.endswith('.jar'):
            cmd.extend(['-jar', self.args.target])
        else:
            # Assume target is a class name with classpath provided
            if self.args.classpath:
                cmd.extend(['-cp', ':'.join(self.args.classpath), self.args.target])
            else:
                logger.error('[EXPLORE] No classpath provided for class target')
                cmd.extend([self.args.target])

        return cmd

    def run_target(self) -> Tuple[ExecutionStatus, List[str]]:
        """Executes the Java target and returns status and output."""
        cmd = self.build_command()

        logger.info(f'[EXPLORE] Running iteration {self.iteration}')
        logger.debug(f'[EXPLORE] Command: {" ".join(cmd)}')

        if self.sym_storage.vars:
            vals = {f'{v.dType.value}_{v.idx}': v.newValue or v.value
                    for v in self.sym_storage.vars.values()}
            logger.info(f'[EXPLORE] Symbolic inputs: {vals}')

        output = []
        try:
            with subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                bufsize=1,
                universal_newlines=True
            ) as proc:
                stdout, _ = proc.communicate(timeout=60)
                output = stdout.splitlines()

                # Log output
                logger.info('[JAVA] Output:')
                for line in output:
                    logger.info(f'[JAVA]   {line}')

                if proc.returncode == 0:
                    return ExecutionStatus.SUCCESS, output
                else:
                    logger.error(f'[EXPLORE] Java exited with code {proc.returncode}')
                    return ExecutionStatus.ERROR, output

        except subprocess.TimeoutExpired:
            logger.error('[EXPLORE] Execution timed out')
            proc.kill()
            return ExecutionStatus.TIMEOUT, output
        except Exception as e:
            logger.error(f'[EXPLORE] Execution failed: {e}')
            return ExecutionStatus.ERROR, output

    def get_next_solution(self) -> bool:
        """
        Queries the strategy service for the next solution.

        Returns:
            True if a new solution was found, False if exploration is complete
        """
        # Get branches from the strategy
        possible_branches = StrategyService.select_branch(endpoint_id=self.endpoint_id)

        logger.info(f'[EXPLORE] Found {len(possible_branches)} possible branches')

        if not possible_branches:
            logger.info('[EXPLORE] No more branches available')
            return False

        # Try to solve branches until we find a SAT one
        for idx, branch in enumerate(possible_branches):
            logger.info(f'[EXPLORE] Checking branch {idx+1}/{len(possible_branches)}: {branch.id}')

            # Skip non-symbolic branches
            logger.info(f'[EXPLORE] Calling is_symbolic_branch for {branch.id}...')
            is_symbolic = StrategyService.is_symbolic_branch(branch)
            logger.info(f'[EXPLORE] is_symbolic_branch returned: {is_symbolic}')

            if not is_symbolic:
                logger.debug(f'[EXPLORE] Skipping non-symbolic branch {branch.id}')
                continue

            logger.info(f'[EXPLORE] Branch {branch.id} is symbolic, attempting to solve...')
            # Try to solve this branch
            sat, sol = StrategyService.solve_branch(branch)
            logger.info(f'[EXPLORE] Branch {branch.id} solve result: {sat}')

            if sat == SATResult.SAT:
                # Found a solution! Store it and continue exploration
                sol_viz = {key: val.get('plain_value', val.get('encoded_value'))
                          for key, val in sol.items()}
                logger.info(f'[EXPLORE] Solved branch {branch.id}')
                logger.info(f'[EXPLORE] Solution: {sol_viz}')
                print(f'[SimpleDriver] Found new path to explore', flush=True)

                # Register the solution in symbolic storage
                # Extract type and index from Input objects (skip length variables)
                # Array variables come in pairs: [I_0 (the array) and [I_0_length (the length)
                # We only want to register the array itself, not the length
                non_length_inputs = [inp for inp in branch.inputs if not inp.name.endswith('_length')]
                var_types = [inp.name.rsplit('_', 1)[0] for inp in non_length_inputs]
                var_indices = [int(inp.name.rsplit('_', 1)[1]) for inp in non_length_inputs]
                self.sym_storage.register_vars(var_types, var_indices)
                self.sym_storage.store_solution(sol)

                return True
            elif sat == SATResult.UNSAT:
                logger.debug(f'[EXPLORE] Branch {branch.id} is UNSAT')
            else:
                logger.warning(f'[EXPLORE] Branch {branch.id} solver returned {sat}')

        # No SAT branches found
        logger.info('[EXPLORE] No satisfiable branches remaining')
        return False

    def run(self):
        """Main exploration loop."""
        logger.info('='*70)
        logger.info('SimpleDriver - Symbolic Path Exploration')
        logger.info('='*70)
        logger.info(f'Target: {self.args.target}')
        logger.info(f'Config: {self.args.config}')
        logger.info(f'Port: {self.args.port}')
        logger.info('='*70)

        # Also print to stdout so user sees progress
        print('='*70, flush=True)
        print('SimpleDriver - Symbolic Path Exploration', flush=True)
        print('='*70, flush=True)
        print(f'Target: {self.args.target}', flush=True)
        print('='*70, flush=True)

        # Main exploration loop
        max_iterations = 100  # Safety limit
        tree = None  # Initialize to avoid UnboundLocalError

        while self.iteration < max_iterations:
            self.iteration += 1

            logger.info('')
            logger.info('='*70)
            logger.info(f'ITERATION {self.iteration}')
            logger.info('='*70)

            # Print to stdout so user sees progress
            print(f'\n[SimpleDriver] Iteration {self.iteration}', flush=True)

            # Run the target
            status, output = self.run_target()

            if status != ExecutionStatus.SUCCESS:
                logger.error('[EXPLORE] Execution failed - aborting exploration')
                break

            # After first execution, get the actual endpoint ID
            if self.endpoint_id is None:
                # Give the HTTP constraint service time to process the request
                import time
                time.sleep(0.5)  # Wait 500ms for constraint service to process

                endpoints = Database.instance().get_endpoints()
                if not endpoints:
                    logger.error('[EXPLORE] No endpoints found after execution')
                    logger.error('[EXPLORE] This may indicate the constraint service is not receiving data')
                    break
                # Use the first (and typically only) endpoint
                self.endpoint_id = endpoints[0]
                logger.info(f'[EXPLORE] Using endpoint ID: {self.endpoint_id}')

            # Check if we found any symbolic variables
            tree = Database.instance().get_tree(self.endpoint_id)
            if len(tree.symbolic_vars) == 0:
                logger.info('[EXPLORE] No symbolic variables found - nothing to explore')
                break

            logger.info(f'[EXPLORE] Found {len(tree.symbolic_vars)} symbolic variables')

            # Try to get next solution
            has_next = self.get_next_solution()

            if not has_next:
                logger.info('[EXPLORE] All reachable paths explored!')
                print('[SimpleDriver] All reachable paths explored!', flush=True)
                break

        # Report final statistics
        logger.info('')
        logger.info('='*70)
        logger.info('EXPLORATION COMPLETE')
        logger.info('='*70)
        logger.info(f'Total iterations: {self.iteration}')
        logger.info(f'Symbolic variables: {len(tree.symbolic_vars) if tree else 0}')
        logger.info('='*70)

        # Print summary to stdout
        print('\n' + '='*70, flush=True)
        print('EXPLORATION COMPLETE', flush=True)
        print('='*70, flush=True)
        print(f'Total iterations: {self.iteration}', flush=True)
        if tree:
            print(f'Symbolic variables: {len(tree.symbolic_vars)}', flush=True)
        print('='*70, flush=True)

        # Exit the process to stop the uvicorn server
        # SimpleDriver is a one-shot exploration tool, not a long-running server
        # Use os._exit() to forcibly terminate the entire process (including uvicorn)
        # sys.exit() would only exit this thread, leaving uvicorn running
        logger.info('[EXPLORE] Terminating SimpleDriver process')
        os._exit(0)
