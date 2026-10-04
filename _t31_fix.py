# -*- coding: utf-8 -*-
import io

paths = [
    r'F:\Vesna-pkg\packages\vesna-mc\templates\VesnaBridge.java',
    r'F:\Vesna-pkg\_t31_fabric\src\main\java\com\example\vesnamc\VesnaBridge.java',
    r'F:\Vesna-pkg\_t31_forge\src\main\java\com\example\vesnamc\VesnaBridge.java',
    r'F:\Vesna-pkg\_t31_neoforge\src\main\java\com\example\vesnamc\VesnaBridge.java',
]
for p in paths:
    s = io.open(p, encoding='utf-8').read()
    n1 = s.count('private final ActionSink sink;')
    n2 = s.count('ResidentBridge(File scriptsDir, String vesnaPath, ActionSink sink)')
    s = s.replace('private final ActionSink sink;', 'private final VesnaBridge.ActionSink sink;')
    s = s.replace('ResidentBridge(File scriptsDir, String vesnaPath, ActionSink sink)',
                  'ResidentBridge(File scriptsDir, String vesnaPath, VesnaBridge.ActionSink sink)')
    io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
    print('fixed', p, n1, n2)
