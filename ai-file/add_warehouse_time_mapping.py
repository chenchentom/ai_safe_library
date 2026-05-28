#!/usr/bin/env python3
"""
为已有 ES 索引追加 warehouse_time 字段映射（不重建索引、不丢数据）。

用法:
  python3 add_warehouse_time_mapping.py
"""
import urllib3
import requests

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

ES_URL = "https://localhost:9200"
ES_USERNAME = "elastic"
ES_PASSWORD = "V4h_Am00B-eNpeE5OSf*"

WAREHOUSE_TIME_MAPPING = {
    "properties": {
        "warehouse_time": {
            "type": "date",
            "format": "yyyy-MM-dd HH:mm:ss",
        }
    }
}

INDICES = ("biz_risk_clue", "biz_risk_review_record")


def put_mapping(index: str) -> None:
    url = f"{ES_URL}/{index}/_mapping"
    resp = requests.put(
        url,
        json=WAREHOUSE_TIME_MAPPING,
        auth=(ES_USERNAME, ES_PASSWORD),
        verify=False,
        timeout=30,
    )
    if resp.status_code in (200, 201):
        print(f"✅ {index}: warehouse_time 映射已添加")
    else:
        print(f"❌ {index}: {resp.status_code} {resp.text}")


if __name__ == "__main__":
    for name in INDICES:
        put_mapping(name)
