from copy import deepcopy
import traceback

from data.Database import Database
from data.Solution.Solution import Solution


class SolutionService:

    def __init__(self):
        self.db = Database.instance()

    def get_next_solution(self):

        if len(self.db.get_new_solutions()) > 0:
            branch_id_next = self.db.get_new_solutions()[0]
            next_sol = self.db.consume_new_solution(branch_id_next)

            return self._convert_to_solution_dto(next_sol)
        else:
            return None

    def get_all_solutions(self):
        all_new_solutions = []
        for branch_id in self.db.get_new_solutions():
            all_new_solutions.append(self._convert_to_solution_dto(self.db.consume_new_solution(branch_id)))

        return all_new_solutions

    # https://z3prover.github.io/api/html/namespacez3py.html
    # https://z3prover.github.io/api/html/classz3py_1_1_int_num_ref.html
    # https://z3prover.github.io/api/html/classz3py_1_1_solver.html
    # https://z3prover.github.io/api/html/classz3py_1_1_model_ref.html
    # https://stackoverflow.com/questions/12598408/z3-python-getting-python-values-from-model
    @staticmethod
    def _convert_to_solution_dto(sol: Solution):
        z3_model = sol.solution
        sol_str_dict = {}
        for s in z3_model:
            try:
                # sol_str_dict[str(s)] = z3_model[s].as_string()
                sol_str_dict[str(s)] = z3_model[s]['encoded_value']
            except BaseException as e:
                print("Could not serialize solution.")
                print("Stack trace: ")
                traceback.print_stack()
                print("Traceback: ")
                traceback.print_tb(e.__traceback__)

                sol_str_dict = {}
                break

        target_inputs = deepcopy(sol.inputs)
        inputs_list = []
        for i in target_inputs:
            input_fields = vars(i)
            del input_fields['lower_bound']
            del input_fields['upper_bound']
            inputs_list.append(input_fields)

        return {'inputs': inputs_list, 'solution': sol_str_dict, 'endpoint_id': sol.endpoint_id}


