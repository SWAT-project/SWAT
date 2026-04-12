from data.Database import Database

import log
logger = log.get_logger()


class BranchCoverageService:
    def add_coverage(**kwargs):
        ids = kwargs.get('ids', {})
        total_branch_instr = kwargs.get('total', 0)

        db = Database.instance()

        cov_old = db.get_branch_coverage()
        db.add_branch_coverage(ids, total_branch_instr)
        cov_new = db.get_branch_coverage()

        relative_coverage = 0
        if cov_new["total_branch_count"] > 0:
            relative_coverage = cov_new["executed_branches"] / cov_new["total_branch_count"]

        logger.info(f'{cov_new["executed_branches"] - cov_old["executed_branches"]} new instructions visited '
                    f'(Total: {relative_coverage}).')
