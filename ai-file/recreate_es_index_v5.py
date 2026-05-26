#!/usr/bin/env python3
import json
import uuid
import datetime
import requests

ES_URL = "https://localhost:9200"
ES_USERNAME = "elastic"
ES_PASSWORD = "V4h_Am00B-eNpeE5OSf*"
INDEX_NAME = "biz_risk_clue"
JSONL_PATH = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1.jsonl'

INDEX_SETTINGS = {
    "settings": {
        "number_of_shards": 3,
        "number_of_replicas": 1
    },
    "mappings": {
        "properties": {
            "id": {
                "type": "keyword"
            },
            "number": {
                "type": "integer"
            },
            "event_name": {
                "type": "text",
                "analyzer": "standard",
                "search_analyzer": "standard",
                "fields": {
                    "keyword": {
                        "type": "keyword",
                        "ignore_above": 256
                    }
                }
            },
            "class_report_1": {
                "type": "keyword"
            },
            "class_report_2": {
                "type": "keyword"
            },
            "class_report_list": {
                "type": "keyword"
            },
            "class_human_1": {
                "type": "keyword"
            },
            "class_human_2": {
                "type": "keyword"
            },
            "class_human_list": {
                "type": "keyword"
            },
            "products_components_services": {
                "type": "text",
                "analyzer": "standard",
                "fields": {
                    "keyword": {
                        "type": "keyword",
                        "ignore_above": 512
                    }
                }
            },
            "operating_entity": {
                "type": "keyword"
            },
            "operating_entity_human": {
                "type": "keyword"
            },
            "risk_description": {
                "type": "text",
                "analyzer": "standard"
            },
            "risk_description_human": {
                "type": "text",
                "analyzer": "standard"
            },
            "source_url": {
                "type": "keyword",
                "ignore_above": 2048
            },
            "source_website": {
                "type": "keyword"
            },
            "paper_title": {
                "type": "text",
                "analyzer": "standard"
            },
            "research_team": {
                "type": "keyword"
            },
            "content": {
                "type": "text",
                "analyzer": "standard"
            },
            "submit_user_name": {
                "type": "keyword"
            },
            "submission_channel": {
                "type": "keyword"
            },
            "submission_time": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss"
            },
            "submit_org_name": {
                "type": "keyword"
            },
            "is_submit": {
                "type": "integer"
            },
            "audit_status": {
                "type": "integer"
            },
            "is_warehouse": {
                "type": "integer"
            },
            "audit_reason": {
                "type": "text",
                "analyzer": "standard"
            },
            "audit_user_name": {
                "type": "keyword"
            },
            "audit_dept_name": {
                "type": "keyword"
            },
            "audit_time": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss"
            },
            "create_time": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss"
            },
            "update_time": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss"
            },
            "deleted": {
                "type": "integer"
            },
            "is_verify": {
                "type": "integer"
            }
        }
    }
}

def convert_yes_no_to_int(value):
    if value == "是":
        return 1
    elif value == "否":
        return 0
    else:
        return None

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

def main():
    import urllib3
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    
    print("=" * 100)
    print("重新创建 ES 索引并导入数据 (下划线格式)")
    print("=" * 100)
    
    print(f"\n1. 检查 ES 连接: {ES_URL}")
    try:
        resp = es_request("GET", "/")
        resp.raise_for_status()
        print(f"   ✅ 连接成功 - ES 版本: {resp.json()['version']['number']}")
    except Exception as e:
        print(f"   ❌ 连接失败: {str(e)}")
        return
    
    print(f"\n2. 删除旧索引: {INDEX_NAME}")
    try:
        resp = es_request("DELETE", f"/{INDEX_NAME}")
        if resp.status_code == 200:
            print("   ✅ 旧索引已删除")
        elif resp.status_code == 404:
            print("   ℹ️  旧索引不存在，跳过删除")
        else:
            print(f"   ⚠️  删除响应: {resp.status_code}")
    except Exception as e:
        print(f"   ❌ 删除失败: {str(e)}")
        return
    
    print(f"\n3. 创建新索引: {INDEX_NAME}")
    try:
        resp = es_request("PUT", f"/{INDEX_NAME}", INDEX_SETTINGS)
        resp.raise_for_status()
        print("   ✅ 新索引已创建")
    except Exception as e:
        print(f"   ❌ 创建失败: {str(e)}")
        try:
            print(f"   响应: {resp.text}")
        except:
            pass
        return
    
    print(f"\n4. 读取 JSONL 文件: {JSONL_PATH}")
    jsonl_items = []
    with open(JSONL_PATH, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line:
                item = json.loads(line)
                jsonl_items.append(item)
    print(f"   ✅ 读取 {len(jsonl_items)} 条记录")
    
    print(f"\n5. 转换并导入数据")
    current_time = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    success_count = 0
    fail_count = 0
    
    for item in jsonl_items:
        try:
            doc_id = str(uuid.uuid4())
            
            class_report_1 = item.get('class_report_1', '')
            class_report_2 = item.get('class_report_2', '')
            class_report_list = []
            if class_report_1:
                class_list_str = class_report_1
                if class_report_2:
                    class_list_str = f"{class_report_1}/{class_report_2}"
                class_report_list = [class_list_str]
            
            submission_time = item.get('Submission_time', '') or item.get('submission_time', '')
            
            es_doc = {
                "id": doc_id,
                "number": item.get('number'),
                "event_name": item.get('event_name', ''),
                "class_report_1": class_report_1,
                "class_report_2": class_report_2,
                "class_report_list": class_report_list,
                "class_human_1": '',
                "class_human_2": '',
                "class_human_list": [],
                "products_components_services": item.get('products_components_services', ''),
                "operating_entity": item.get('operating_entity', ''),
                "operating_entity_human": '',
                "risk_description": item.get('risk_description', ''),
                "risk_description_human": '',
                "source_url": item.get('source_url', ''),
                "source_website": item.get('source_website', ''),
                "paper_title": item.get('paper_title', ''),
                "research_team": item.get('research_team', ''),
                "content": item.get('content', ''),
                "submit_user_name": item.get('submit_user_name', ''),
                "submission_channel": item.get('submission_channel', ''),
                "submission_time": submission_time if submission_time else None,
                "submit_org_name": '',
                "is_submit": convert_yes_no_to_int(item.get('is_submit', '')),
                "audit_status": 10,
                "is_warehouse": 0,
                "audit_reason": '',
                "audit_user_name": '',
                "audit_dept_name": '',
                "audit_time": None,
                "create_time": current_time,
                "update_time": current_time,
                "deleted": 0,
                "is_verify": convert_yes_no_to_int(item.get('is_verify', ''))
            }
            
            es_doc_clean = {k: v for k, v in es_doc.items() if v is not None}
            
            resp = es_request("PUT", f"/{INDEX_NAME}/_doc/{doc_id}", es_doc_clean)
            resp.raise_for_status()
            success_count += 1
            if success_count % 10 == 0:
                print(f"   已导入 {success_count}/{len(jsonl_items)} 条...")
                
        except Exception as e:
            fail_count += 1
            print(f"   ❌ 导入失败 (记录 {item.get('number', '?')}): {str(e)[:100]}")
    
    print(f"\n" + "=" * 100)
    print(f"导入完成: {success_count} 条成功, {fail_count} 条失败")
    print("=" * 100)
    
    print(f"\n6. 验证索引")
    try:
        resp = es_request("GET", f"/{INDEX_NAME}/_count")
        resp.raise_for_status()
        count = resp.json()['count']
        print(f"   ✅ 索引中共有 {count} 条文档")
        
        print(f"\n7. 查看第一条文档")
        resp = es_request("GET", f"/{INDEX_NAME}/_search?size=1")
        resp.raise_for_status()
        hits = resp.json()['hits']['hits']
        if hits:
            print("   字段名（下划线格式）:")
            for key in hits[0]['_source'].keys():
                print(f"   - {key}")
            if 'class_report_list' in hits[0]['_source']:
                print(f"\n   class_report_list（数组格式）: {hits[0]['_source']['class_report_list']}")
    except Exception as e:
        print(f"   ❌ 验证失败: {str(e)}")

if __name__ == "__main__":
    main()
