import json
import logging
import re

from data.BinaryExecutionTree.Node import Node

from data.Database import Database
from strategy.DFS import dfs
from solver.SolverHandler import SATResult, Z3Handler


class StrategyService:
    
    @staticmethod
    def select_branch(**kwargs):
        endpoint_id = kwargs.get('endpoint_id', None)
        db = Database.instance()
        tree = db.get_tree(endpoint_id)
        visited = set()
        possible_branches = dfs(visited, tree, tree.root, db.get_solutions(), db.get_unsat_branches())
        
        return possible_branches
        
        
    @staticmethod
    def collect_path_constrains(node: Node) -> list():
        path_constraints = list()
        trace_id = node.trace_id
        
        while node.parent is not None:
            node = node.parent
            if node.kind != "Special":
                if trace_id in node.constraint:
                    c = node.constraint[trace_id]
                    if "declare-fun" in c:
                        path_constraints.append(c)
                        
        return path_constraints
        
    @staticmethod
    def collect_input_constrains(node: Node) -> list():
        path_constraints = list()
        
        for input in node.inputs:
            path_constraints.append(input.lower_bound)
            path_constraints.append(input.upper_bound)
        
        return path_constraints

    @staticmethod
    def collect_uf_definitions(node: Node) -> list():
        uf_definitions = list()
        for uf in node.ufs:
            uf_definitions.append(uf.definition)
        return uf_definitions

    @staticmethod
    def solve_branch(possible_branch: Node, endpoint_id=None):
        db = Database.instance()
        
        path_constraints = StrategyService.collect_path_constrains(possible_branch)
        path_constraints.extend(StrategyService.collect_input_constrains(possible_branch))
        path_constraints.extend(StrategyService.collect_uf_definitions(possible_branch))
        inputs = possible_branch.inputs
        sat, sol = Z3Handler.solve_opt(possible_branch, path_constraints)
        
        if sat == SATResult.SAT:
            db.add_solution(branch_id=possible_branch.gid, sol=sol, inputs=inputs, endpoint_id=endpoint_id)
            
        elif sat == SATResult.UNSAT:
            db.add_unsat_branch(possible_branch.gid)

        return sat, sol

    @staticmethod
    def is_symbolic_branch(node: Node) -> bool:
        constraint = node.constraint[node.trace_id]
        # Match regular symbolic variables: I_21, Z_22, java/lang/String_23, etc.
        # Also match list element variables: List_20_I_0, List_20_java/lang/String_1, etc.
        regular_pattern = r'\(declare-fun ([ZBCSIFJD]|java\/lang\/String)_\d+'
        list_pattern = r'\(declare-fun List_\d+_([ZBCSIFJD]|java\/lang\/String)_\d+'
        res =  re.search(regular_pattern, constraint) or re.search(list_pattern, constraint)
        #print(f"Node {node.gid} is symbolic: {res is not None}")
        #print(f"Constraint: {constraint}")
        return res is not None
    
        
        
        
        