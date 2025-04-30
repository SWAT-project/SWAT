import logging
import os


# Internal logger
_interal_logger = logging.getLogger(__name__)

# Global logger instances
logger = logging.getLogger('explorer.log')
verdict_logger = logging.getLogger('verdict.log')


class LoggerNotInitializedError(Exception):
    """Exception raised when loggers are not initialized."""
    pass


def get_logger():
    global logger
    if logger is None:
        raise LoggerNotInitializedError("Logger has not been initialized.")
    return logger

def get_verdict_logger():
    global logger
    if logger is None:
        raise LoggerNotInitializedError("Logger has not been initialized.")
    return logger


def setup_logger(_logger, log_file, level=logging.INFO, use_console_handler=True):
    """Sets up a logger with both file and optional console handlers."""
    global _interal_logger 

    formatter = logging.Formatter(
        "%(asctime)s [%(threadName)-12.12s] [%(levelname)-5.5s]  %(message)s"
    )
    _interal_logger.debug(f"Setting up logger {_logger} with file {log_file}")
    
    # File handler
    file_handler = logging.FileHandler(log_file)
    file_handler.setLevel(level)  # Ensure the handler's level matches the logger's level
    file_handler.setFormatter(formatter)

    # Create logger
    
    _logger.setLevel(level)
    _logger.addHandler(file_handler)

    # Optional console handler
    if use_console_handler:
        console_handler = logging.StreamHandler()
        console_handler.setLevel(level)  # Ensure consistent levels
        console_handler.setFormatter(formatter)
        _logger.addHandler(console_handler)

    # Prevent propagation to root logger
    _logger.propagate = False
    return _logger


def initialize_loggers(log_dir, level=logging.DEBUG):
    """Initializes global loggers based on the provided log directory."""
    global logger, verdict_logger
    os.makedirs(log_dir, exist_ok=True)
    
    logger = setup_logger(logger, os.path.join(log_dir, 'explorer.log'), level=level)
    verdict_logger = setup_logger(verdict_logger, os.path.join(log_dir, 'verdict.log'),level=level)

