# vesna-json

JSON utilities for Vesna scripts: pretty printing, deep get/has/set along dotted paths and deep merge.

用 Vesna 编写的 JSON 工具包：美化输出、点路径深取值/判断/设值与深合并。

## Install / 安装

```sh
vpm install vesna-json
```

Then in your script / 然后在脚本中：

```ves
import vesna-json,
```

## API

### json_pretty(v)

Return a multi-line, indented JSON string for any value.

返回任意值的多行缩进 JSON 字符串。

```ves
print(json_pretty({"a": '1'; "b": {"c": ["x"; "y"]}})),
# {
#   "a": 1,
#   "b": {
#     "c": [
#       "x",
#       "y"
#     ]
#   }
# }
```

### json_get(d; path)

Deep get along a dotted path like `"a.b.c"`; a numeric segment indexes a list (1-based). Returns `none` when missing.

沿点路径（如 `"a.b.c"`）深取值；数字段按列表索引（从 1 起）。缺失时返回 `none`。

```ves
json_get({"a": ["x"; "y"]}; "a.2"),   # "y"
```

### json_has(d; path)

Return `true` when the path exists, otherwise `false`.

路径存在返回 `true`，否则 `false`。

### json_set(d; path; v)

Return a deep copy of `d` with the value at `path` replaced by `v` (intermediate dicts/lists are created as needed). The input `d` is never modified.

返回 `d` 的深拷贝，并将 `path` 处的值替换为 `v`（必要时自动创建中间字典/列表）。原 `d` 不会被修改。

```ves
d2 = json_set({"a": ["x"; "y"]}; "a.1"; "Z"),
# d2["a"][1] == "Z", d["a"][1] == "x"
```

### json_merge(a; b)

Return a deep merge of `a` and `b`: dicts merge recursively, other values from `b` win. Returns a new copy.

返回 `a` 与 `b` 的深合并：字典递归合并，其余值以 `b` 为准。返回新拷贝。

```ves
m = json_merge({"n": {"x": '1'}}; {"n": {"y": '2'}; "z": '3'}),
# m == {"n": {"x": '1'; "y": '2'}; "z": '3'}
```

## License / 许可协议
MIT
