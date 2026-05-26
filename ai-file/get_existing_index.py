#!/usr/bin/env python3
import requests
import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
import json

ES_URL = "https://localhost:9200"
ES_USERNAME = "elastic"
ES_PASSWORD = "V4h_Am00B-eNpeE5OSf*"

print("=" * 100)
print("查看 biz_risk_review_record 索引定义")
print("=" * 100)

try:
    resp = requests.get(
        f"{ES_URL}/biz_risk_review_record",
        auth=(ES_USERNAME, ES_PASSWORD),
        verify=False
    )
    print(json.dumps(resp.json(), indent=2, ensure_ascii=False))
except Exception as e:
    print(f"错误: {str(e)}")
