from data.Database import Database

import log
logger = log.get_logger()


class SimpleInstructionCoverageService:
    def add_coverage(**kwargs):
        ids = kwargs.get('ids', {})
        endpoint_id = kwargs.get('endpoint', None)
        total_instr = kwargs.get('total', 0)

        db = Database.instance()

        cov_old = db.get_total_instr_coverage()
        db.add_instr_coverage(endpoint_id, ids, total_instr)
        cov_new = db.get_total_instr_coverage()

        relative_coverage = 0
        if cov_new["total_instr_count"] > 0:
            relative_coverage = cov_new["executed_instr"] / cov_new["total_instr_count"]

        logger.info(f'{cov_new["executed_instr"] - cov_old["executed_instr"]} new instructions visited '
                    f'(Total: {relative_coverage}).')
