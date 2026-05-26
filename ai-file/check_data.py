import subprocess
import json

es_url = "https://localhost:9200"
es_user = "elastic"
es_pass = "V4h_Am00B-eNpeE5OSf*"
index_name = "biz_risk_clue"

def run_curl(method, path, data=None):
    cmd = [
        "curl", "-k", "-u", f"{es_user}:{es_pass}",
        "-X", method,
        f"{es_url}{path}"
    ]
    if data:
        cmd.extend(["-H", "Content-Type: application/json", "-d", data])
    result = subprocess.run(cmd, capture_output=True, text=True)
    return result.stdout

# 获取一条数据看看
print("检查索引映射...")
mapping = run_curl("GET", f"/{index_name}/_mapping")
print(mapping)

print("\n\n获取前2条数据...")
data = run_curl("GET", f"/{index_name}/_search?size=2")
print(data)
