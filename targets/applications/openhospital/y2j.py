import sys
import os
import yaml
import json

with open('oh_build/api/oh.json', 'w') as out_f, open('oh_build/api/oh.yaml', 'r') as in_f:
    y_input = yaml.safe_load(in_f)
    json.dump(y_input, out_f, indent=2)