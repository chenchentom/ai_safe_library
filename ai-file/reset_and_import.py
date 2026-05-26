#!/usr/bin/env python3
import requests
from requests.packages.urllib3.exceptions import InsecureRequestWarning

requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

ES_URL = "https://localhost:9200"
ES_USER = "elastic"
ES_PASS = "V4h_Am00B-eNpeE5OSf*"
INDEX_NAME = "biz_risk_clue"

print("正在删除旧索引...")
try:
    response = requests.delete(
        f"{ES_URL}/{INDEX_NAME}",
        auth=(ES_USER, ES_PASS),
        verify=False
    )
    print(f"删除结果: {response.status_code}")
    print(response.text)
except Exception as e:
    print(f"删除索引时出错（可能不存在）: {e}")

print("\n现在运行导入脚本...")
import subprocess
subprocess.run(["python3", "import_data.py"])
