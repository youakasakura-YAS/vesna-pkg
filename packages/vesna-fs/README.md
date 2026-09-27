# vesna-fs

Filesystem utilities for Vesna scripts: recursive search, human-readable sizes, batch rename and directory trees.

用 Vesna 编写的文件系统工具包：递归查找、人类可读大小、批量重命名与目录树。

## Install / 安装

```sh
vpm install vesna-fs
```

Then in your script / 然后在脚本中：

```ves
import vesna-fs,
```

## API

### fs_find(dir; pattern)

Recursively list files under `dir` whose file name matches the regex `pattern`, returning full paths.

递归列出 `dir` 下文件名匹配正则 `pattern` 的文件，返回完整路径列表。

```ves
ves_files = fs_find("src"; "\.ves$"),
```

### fs_size_human(n)

Format a byte count as `500 B`, `1.5 KB`, `3.2 MB`, etc.

把字节数格式化为 `500 B`、`1.5 KB`、`3.2 MB` 等。

### fs_batch_rename(dir; from; to)

Replace `from` with `to` in every file name directly under `dir` (non-recursive, files only). Returns the number of files renamed.

将 `dir` 直接子目录中的文件名中的 `from` 替换为 `to`（不递归、仅文件）。返回重命名文件数。

### fs_tree(dir)

Return a text directory tree like:

返回文本目录树，形如：

```text
a.ves
b.txt
sub/
  c.ves
```

## License / 许可协议
MIT
