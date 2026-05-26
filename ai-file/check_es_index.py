#!/usr/bin/env python3
import requests
import urllib3
import time
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

ES_URL = "https://localhost:9200"
ES_USERNAME = "elastic"
ES_PASSWORD = "V4h_Am00B-eNpeE5OSf*"
INDEX_NAME = "biz_risk_clue"

def es_request(method, path, json_data=None):
    url = f"{ES_URL}{path}"
    response = requests.request(
        method,
        url,
        auth=(ES_USERNAME, ES_PASSWORD),
        json=json_data,
        verify=False,
        timeout=30
    )
    return response

print("=" * 100)
print("验证 ES 索引")
print("=" * 100)

print("\n等待 5 秒...")
time.sleep(5)

print("\n1. 获取索引计数")
try:
    resp = es_request("GET", f"/{INDEX_NAME}/_count")
    resp.raise_for_status()
    count = resp.json()['count']
    print(f"   ✅ 索引中共有 {count} 条文档")
except Exception as e:
    print(f"   ❌ 失败: {str(e)}")

print("\n2. 查看索引设置")
try:
    resp = es_request("GET", f"/{INDEX_NAME}")
    resp.raise_for_status()
    data = resp.json()
    print(f"   Shards: {data[INDEX_NAME]['settings']['index']['number_of_shards']}")
    print(f"   Replicas: {data[INDEX_NAME]['settings']['index']['number_of_replicas']}")
except Exception as e:
    print(f"   ❌ 失败: {str(e)}")

print("\n3. 查看一条样例文档")
try:
    resp = es_request("GET", f"/{INDEX_NAME}/_search?size=1")
    resp.raise_for_status()
    hits = resp.json()['hits']['hits']
    if hits:
        doc = hits[0]['_source']
        print(f"   event_name: {doc.get('event_name')}")
        print(f"   class_report_1: {doc.get('class_report_1')}")
        print(f"   class_report_2: {doc.get('class_report_2')}")
        print(f"   class_report_list: {doc.get('class_report_list')}")
        print(f"   audit_status: {doc.get('audit_status')}")
        print(f"   is_warehouse: {doc.get('is_warehouse')}")
        print(f"   deleted: {doc.get('deleted')}")
except Exception as e:
    print(f"   ❌ 失败: {str(e)}")

print("\n" + "=" * 100)
