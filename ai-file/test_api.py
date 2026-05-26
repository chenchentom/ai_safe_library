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

# 直接从ES获取数据看看
print("从ES直接查询数据...")
query = '''{
    "query": {
        "match_all": {}
    },
    "size": 1
}'''
result = run_curl("POST", f"/{index_name}/_search", query)
print(result)
