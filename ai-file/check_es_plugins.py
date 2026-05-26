#!/usr/bin/env python3
import requests
import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

ES_URL = "https://localhost:9200"
ES_USERNAME = "elastic"
ES_PASSWORD = "V4h_Am00B-eNpeE5OSf*"

print("=" * 100)
print("检查 ES 插件")
print("=" * 100)

try:
    resp = requests.get(
        f"{ES_URL}/_cat/plugins?v",
        auth=(ES_USERNAME, ES_PASSWORD),
        verify=False
    )
    print(resp.text)
except Exception as e:
    print(f"错误: {str(e)}")

print("\n" + "=" * 100)
print("检查现有索引")
print("=" * 100)

try:
    resp = requests.get(
        f"{ES_URL}/_cat/indices?v",
        auth=(ES_USERNAME, ES_PASSWORD),
        verify=False
    )
    print(resp.text)
except Exception as e:
    print(f"错误: {str(e)}")
