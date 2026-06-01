import unittest
import subprocess
from generate_report import run_cmd

class TestGenerateReport(unittest.TestCase):
    def test_run_cmd_safe(self):
        # A simple, safe command
        output = run_cmd(['echo', 'hello'])
        self.assertEqual(output.strip(), 'hello')

    def test_run_cmd_injection_prevented(self):
        # An attempt to inject commands that should fail or be treated as literals
        # When shell=False, subprocess treats the list elements as arguments to the first element
        # It won't execute `; echo injected` as a separate command
        output = run_cmd(['echo', 'hello;', 'echo', 'injected'])
        self.assertEqual(output.strip(), 'hello; echo injected')

    def test_run_cmd_invalid_command(self):
        # When shell=False, checking that an invalid command raises FileNotFoundError
        with self.assertRaises(FileNotFoundError):
            run_cmd(['not_a_real_command', 'test'])

if __name__ == '__main__':
    unittest.main()
