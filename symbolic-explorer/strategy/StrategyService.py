import json
import logging
import re

from data.BinaryExecutionTree.Node import Node

from data.Database import Database
from strategy.DFS import dfs
from solver.SolverHandler import SATResult, Z3Handler
from log import strategy_logger as logger


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
    def solve_branch(possible_branch: Node):
        db = Database.instance()
        
        path_constraints = StrategyService.collect_path_constrains(possible_branch)
        path_constraints.extend(StrategyService.collect_input_constrains(possible_branch))
        inputs = possible_branch.inputs
        sat, sol = Z3Handler.solve_opt(possible_branch, path_constraints)
        
        if sat == SATResult.SAT:
            db.add_solution(branch_id=possible_branch.gid, sol=sol, inputs=inputs)
            
        elif sat == SATResult.UNSAT:
            db.add_unsat_branch(possible_branch.gid)

        return sat, sol

    @staticmethod
    def is_symbolic_branch(node: Node) -> bool:
        constraint = node.constraint[node.trace_id]
        return re.search(r'\(declare-fun ([ZCSIFJD]|Ljava\/lang\/String)_\d+', constraint)
    
        
        
        
        