from enum import Enum


class ConfigKeys(Enum):
    DEBUG = "logging.debug",
    MODE = "explorer.mode",
    TARGET = "explorer.target"
    SYMBOLIC_VARS = "explorer.symbolicvars"
    LIBRARY_PATH = "executor.librarypath"
    AGENT = "executor.agent"
    BASE_DIR = "root.dir"


class ConfigManager:

    def __init__(self, path, config):
        self.path = path
        self.config = config

    def get(self, key: ConfigKeys, default=None):
        return self.config.get(key.value, default)
