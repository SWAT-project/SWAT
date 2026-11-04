import logging
import os


# Internal logger
_interal_logger = logging.getLogger(__name__)

# Global logger instances
logger = logging.getLogger('explorer.log')
verdict_logger = logging.getLogger('verdict.log')


# Pass as `extra` for lines relayed from another program (e.g. the executor's output): they carry
# their own timestamp and level, so they are logged as they are.
RELAYED = {'relayed': True}


class _Formatter(logging.Formatter):
    """The explorer's format, except for relayed lines (see RELAYED), which are left as they are."""

    def format(self, record):
        if getattr(record, 'relayed', False):
            return record.getMessage()
        return super().format(record)


class LoggerNotInitializedError(Exception):
    """Exception raised when loggers are not initialized."""
    pass


def get_logger():
    global logger
    if logger is None:
        raise LoggerNotInitializedError("Logger has not been initialized.")
    return logger

def get_verdict_logger():
    global verdict_logger
    if verdict_logger is None:
        raise LoggerNotInitializedError("Logger has not been initialized.")
    return verdict_logger


def setup_logger(_logger, log_file, level=logging.INFO, use_console_handler=True, console_level=None):
    """Sets up a logger with both file and optional console handlers. The console handler logs
    from console_level on (default: level)."""
    global _interal_logger 

    formatter = _Formatter(
        "%(asctime)s [%(threadName)-20.20s] [%(levelname)-5.5s]  %(message)s"
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
        console_handler.setLevel(level if console_level is None else console_level)
        console_handler.setFormatter(formatter)
        _logger.addHandler(console_handler)

    # Prevent propagation to root logger
    _logger.propagate = False
    return _logger


def initialize_loggers(log_dir, level=logging.DEBUG, console_level=logging.INFO):
    """Initializes global loggers based on the provided log directory: the log files get
    everything from level on, the console from console_level on."""
    global logger, verdict_logger
    os.makedirs(log_dir, exist_ok=True)

    logger = setup_logger(logger, os.path.join(log_dir, 'explorer.log'), level=level, console_level=console_level)
    verdict_logger = setup_logger(verdict_logger, os.path.join(log_dir, 'verdict.log'), level=level,
                                  console_level=console_level)
    # The verdict also goes to explorer.log, so that it reads as the whole run
    for handler in logger.handlers:
        if isinstance(handler, logging.FileHandler):
            verdict_logger.addHandler(handler)

