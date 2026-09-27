# -*- coding: utf-8 -*-
import subprocess, shutil, os, sys, time

DOCS = r'F:\Vesna-docs'
DIST = os.path.join(DOCS, 'docs', '.vitepress', 'dist')
TMP = r'F:\Vesna-pkg\_gh_dist'

def run(args, cwd=DOCS):
    p = subprocess.run(args, cwd=cwd, capture_output=True, text=True, encoding='utf-8', errors='replace')
    return p.returncode, (p.stdout or '') + (p.stderr or '')

def retry_push(args, tries=4):
    for i in range(1, tries + 1):
        rc, out = run(args)
        if rc == 0:
            return True, out
        print(f'attempt {i} failed: {out.strip()[:200]}')
        if i < tries:
            time.sleep(20)
    return False, out

# 1) stage dist outside the repo
if os.path.exists(TMP): shutil.rmtree(TMP)
shutil.copytree(DIST, TMP)
print('dist staged:', len(os.listdir(TMP)), 'items')

# 2) checkout gh-pages temp branch
rc, out = run(['git', 'checkout', '-b', '_gh_sync', 'origin/gh-pages'])
print('checkout:', rc, out.strip()[:100])
if rc != 0: sys.exit(1)

# 3) clear branch root, keep .git and node_modules
KEEP = {'.git', 'node_modules'}
for name in os.listdir(DOCS):
    p = os.path.join(DOCS, name)
    if name in KEEP: continue
    if os.path.isdir(p): shutil.rmtree(p, ignore_errors=True)
    else: os.remove(p)

# 4) copy staged dist in
for name in os.listdir(TMP):
    s = os.path.join(TMP, name)
    d = os.path.join(DOCS, name)
    if os.path.isdir(s): shutil.copytree(s, d)
    else: shutil.copy2(s, d)

rc, out = run(['git', 'add', '-A'])
print('add:', rc)
# 保险：gh-pages 分支无 .gitignore，node_modules 可能被 add；从索引移除
run(['git', 'rm', '-r', '--cached', 'node_modules', '-q'])
rc, out = run(['git', 'commit', '-m', '喵~'])
print('commit:', rc, out.strip()[:100])
ok, out = retry_push(['git', 'push', 'origin', '_gh_sync:gh-pages'])
print('push gh-pages ok:', ok)
print(out.strip()[-200:] if ok else 'FAILED')

# 5) back to main + cleanup
run(['git', 'checkout', 'main'])
run(['git', 'branch', '-D', '_gh_sync'])
shutil.rmtree(TMP, ignore_errors=True)
print('done')
