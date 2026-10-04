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
    old = 'public void runActions(ActionSink sink, Map<String, Object> result) {'
    new = 'public static void runActions(ActionSink sink, Map<String, Object> result) {'
    assert s.count(old) == 1, p
    s = s.replace(old, new)
    io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
    print('static runActions:', p)
