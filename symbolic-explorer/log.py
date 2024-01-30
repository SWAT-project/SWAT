import logging


def setup_logger(name, log_file, level=logging.INFO, useConsoleHandler=True):
    formatter = logging.Formatter("%(asctime)s [%(threadName)-12.12s] [%(levelname)-5.5s]  %(message)s")

    fileHandler = logging.FileHandler(log_file)
    fileHandler.setFormatter(formatter)
    logger = logging.getLogger(name)
    if useConsoleHandler:
        consoleHandler = logging.StreamHandler()
        consoleHandler.setFormatter(formatter)
        logger.addHandler(consoleHandler)
    logger.setLevel(level)
    logger.addHandler(fileHandler)

    return logger


logger = setup_logger("main_logger", './logs/explorer.log')
