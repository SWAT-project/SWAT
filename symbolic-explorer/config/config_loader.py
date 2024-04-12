import javaproperties


def load_config_from_file(config_path):
    with open(config_path, "rb") as config_file:
        return javaproperties.load(config_file)
