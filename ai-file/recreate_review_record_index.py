#!/usr/bin/env python3
"""
仅重建 biz_risk_review_record 索引（不影响 biz_risk_clue）。
旧版 camelCase / isHarmful 字段将被清除，历史审核记录需重新审核产生。
"""
import json
import sys
import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

ES_URL = "https://localhost:9200"
ES_USERNAME = "elastic"
ES_PASSWORD = "V4h_Am00B-eNpeE5OSf*"
INDEX_NAME = "biz_risk_review_record"

INDEX_BODY = {
    "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 1,
        "refresh_interval": "1s",
    },
    "mappings": {
        "properties": {
            "clue_id": {"type": "keyword"},
            "is_warehouse": {"type": "integer"},
            "class_human_1": {"type": "keyword"},
            "class_human_2": {"type": "keyword"},
            "class_human_list": {"type": "keyword"},
            "risk_category": {"type": "keyword"},
            "risk_description_human": {
                "type": "text",
                "analyzer": "standard",
            },
            "operating_entity_human": {"type": "keyword"},
            "review_result": {"type": "keyword"},
            "review_comment": {
                "type": "text",
                "analyzer": "standard",
            },
            "reviewer": {"type": "keyword"},
            "reviewer_name": {"type": "keyword"},
            "review_dept": {"type": "keyword"},
            "review_time": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd'T'HH:mm:ss||epoch_millis",
            },
            "warehouse_time": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd'T'HH:mm:ss||epoch_millis",
            },
        }
    },
}


def es_request(method, path, json_data=None):
    return requests.request(
        method,
        f"{ES_URL}{path}",
        auth=(ES_USERNAME, ES_PASSWORD),
        json=json_data,
        verify=False,
        timeout=30,
    )


def main():
    print("=" * 80)
    print(f"重建索引: {INDEX_NAME}（不触碰 biz_risk_clue）")
    print("=" * 80)

    # 统计旧数据
    try:
        resp = es_request("GET", f"/{INDEX_NAME}/_count")
        if resp.status_code == 200:
            old_count = resp.json().get("count", 0)
            print(f"当前索引文档数: {old_count}")
    except Exception as exc:
        print(f"读取旧索引计数失败: {exc}")

    # 删除旧索引
    resp = es_request("DELETE", f"/{INDEX_NAME}")
    if resp.status_code in (200, 404):
        print(f"删除旧索引: {resp.status_code}")
    else:
        print(f"删除失败: {resp.status_code} {resp.text}")
        sys.exit(1)

    # 创建新索引
    resp = es_request("PUT", f"/{INDEX_NAME}", INDEX_BODY)
    if resp.status_code not in (200, 201):
        print(f"创建失败: {resp.status_code} {resp.text}")
        sys.exit(1)

    print("创建新索引成功")
    print(json.dumps(resp.json(), indent=2, ensure_ascii=False))

    # 验证 mapping
    resp = es_request("GET", f"/{INDEX_NAME}/_mapping")
    print("\n最终 mapping:")
    print(json.dumps(resp.json(), indent=2, ensure_ascii=False))
    print("\n完成。请重启后端后重新提交审核以写入新格式记录。")


if __name__ == "__main__":
    main()
