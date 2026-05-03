import subprocess
import re

def run_cmd(cmd):
    return subprocess.check_output(cmd, shell=True).decode('utf-8')

log = run_cmd('git log --since="midnight" --oneline')
commits = log.strip().split('\n')
for c in commits:
    parts = c.split(' ', 1)
    if len(parts) < 2: continue
    hash = parts[0]
    msg = parts[1]
    stat = run_cmd(f'git show {hash} --stat | tail -n 1')
    print(f"- {hash}: {msg} ({stat.strip()})")
