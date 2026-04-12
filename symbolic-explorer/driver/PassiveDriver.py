import log
logger = log.get_logger()
from data.Database import Database
from strategy.StrategyService import StrategyService

import time


class PassiveDriver:

    def __init__(self, args):
        self.args = args

    def run_dse(self, auto_run=False):
        db = Database.instance()
        no_open_branches = True

        while True and auto_run:

            for ep_id in db.get_endpoints():
                # sat_flag, sol, inputs = StrategyService.select_branch(endpoint_id=ep_id)
                possible_branches = StrategyService.select_branch(endpoint_id=ep_id)
                solutions = []
                for idx, possible_branch in enumerate(possible_branches):
                    if "declare-fun" in possible_branch.constraint[possible_branch.trace_id]:
                        sat, sol = StrategyService.solve_branch(possible_branch=possible_branch,
                                                                endpoint_id=ep_id)
                        if sol is not None:
                            solutions.append((sat, sol))
                            sol_viz = [f'{key}: {val["plain_value"]}' for key, val in sol.items()]
                            logger.info(f'[SYMBOLIC EXPLORATION] SAT solution: {sol}')
                            logger.info(f'[SYMBOLIC EXPLORATION] Found new solution: {sol_viz}')

                if len(solutions) == 0:
                    logger.info(f'No more new branches found for endpoint_id: ' + str(ep_id))
                else:
                    no_open_branches = False
                    logger.info('sat_flags: ' + str([item[0] for item in solutions]))
                    for idx, solution in enumerate(solutions):
                        logger.info("Solution " + str(idx))
                        for s in solution[1]:
                            logger.info('sol:' + str(type(sol[s])))
                    # for i in inputs:
                    #     log.info('inputs: ' + str(i.name) + ',' + str(i.value) + ',' + str(i.type) + ','
                    #           + str(i.lower_bound) + ',' + str(i.upper_bound))

            time.sleep(5)

            if no_open_branches:
                logger.info("No new open branches found :/ Waiting impatiently!")