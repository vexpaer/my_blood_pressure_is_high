import re
import subprocess
import sys

"""在当前 Android 界面上找到指定文本并点击其中心（截图工作流辅助）。"""

text = sys.argv[1]
subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True)
subprocess.run(["adb", "pull", "/sdcard/ui.xml", "/tmp/ui.xml"], capture_output=True)
xml = open("/tmp/ui.xml", encoding="utf-8").read()
m = re.search(
    r'text="' + re.escape(text) + r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',
    xml,
)
if not m:
    print(f"NOT FOUND: {text}")
    sys.exit(1)
x = (int(m.group(1)) + int(m.group(3))) // 2
y = (int(m.group(2)) + int(m.group(4))) // 2
subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)])
print(f"tapped {text} at {x},{y}")
