"""
Unit tests for svcomp/SymbolicStorage: the inputs passed to the executor as -Dswat.input.

Run from symbolic-explorer/ with the harness venv (needs z3):
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_svcomp_storage -v
"""
import os
import struct
import sys
import unittest
from types import SimpleNamespace

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from svcomp.SymbolicStorage import SymbolicStorage


def bits(d: float) -> int:
    return struct.unpack('>Q', struct.pack('>d', d))[0]


class SymbolicStorageTest(unittest.TestCase):

    def test_floating_point_trace_values_are_passed_as_decimal_bits(self):
        # The trace has the hex digits of the bits; the Verifier parses unsigned decimal
        storage = SymbolicStorage()
        storage.register_vars([SimpleNamespace(name='D_710933219792', value=format(bits(-1.5), 'x')),
                               SimpleNamespace(name='F_7', value='3fc00000'),
                               SimpleNamespace(name='I_5', value='42'),
                               SimpleNamespace(name='D_8', value='0')])
        self.assertEqual(storage.vars['D_710933219792'].value, str(bits(-1.5)))
        self.assertEqual(storage.vars['F_7'].value, str(0x3fc00000))
        self.assertEqual(storage.vars['I_5'].value, '42')
        self.assertEqual(storage.vars['D_8'].value, '0')

    def test_a_solution_overrides_the_trace_value(self):
        storage = SymbolicStorage()
        storage.register_vars([SimpleNamespace(name='D_1', value='3ff8000000000000')])
        storage.store_solution({'D_1': {'encoded_value': '0', 'plain_value': 0.0}})
        self.assertEqual(storage.vars['D_1'].newValue, '0')


if __name__ == '__main__':
    unittest.main()
