import logging


def setup_logger(name, log_file, level=logging.INFO, useConsoleHandler=True):
    formatter = logging.Formatter("%(asctime)s [%(threadName)-12.12s] [%(levelname)-5.5s]  %(message)s")

    fileHandler = logging.FileHandler(log_file)
    fileHandler.setFormatter(formatter)
    if useConsoleHandler:
        consoleHandler = logging.StreamHandler()
        consoleHandler.setFormatter(formatter)
    logger = logging.getLogger(name)
    logger.setLevel(level)
    logger.addHandler(fileHandler)
    #logger.addHandler(consoleHandler)

    return logger


coverage_logger = setup_logger("coverage_logger", './logs/coverage.log')
request_logger = setup_logger("request_logger", './logs/request.log',useConsoleHandler=False)
strategy_logger = setup_logger("strategy_logger", './logs/strategy.log')
constraint_logger = setup_logger("constraint_logger", './logs/constraint.log')
solution_logger = setup_logger("solution_logger", './logs/solution.log')
verdict_logger = setup_logger("verdict_logger", './logs/verdict.log')
