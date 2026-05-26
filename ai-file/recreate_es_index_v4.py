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
            "eventName": {
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
            "classReport1": {
                "type": "keyword"
            },
            "classReport2": {
                "type": "keyword"
            },
            "classReportList": {
                "type": "keyword"
            },
            "classHuman1": {
                "type": "keyword"
            },
            "classHuman2": {
                "type": "keyword"
            },
            "classHumanList": {
                "type": "keyword"
            },
            "productsComponentsServices": {
                "type": "text",
                "analyzer": "standard",
                "fields": {
                    "keyword": {
                        "type": "keyword",
                        "ignore_above": 512
                    }
                }
            },
            "operatingEntity": {
                "type": "keyword"
            },
            "operatingEntityHuman": {
                "type": "keyword"
            },
            "riskDescription": {
                "type": "text",
                "analyzer": "standard"
            },
            "riskDescriptionHuman": {
                "type": "text",
                "analyzer": "standard"
            },
            "sourceUrl": {
                "type": "keyword",
                "ignore_above": 2048
            },
            "sourceWebsite": {
                "type": "keyword"
            },
            "paperTitle": {
                "type": "text",
                "analyzer": "standard"
            },
            "researchTeam": {
                "type": "keyword"
            },
            "content": {
                "type": "text",
                "analyzer": "standard"
            },
            "submitUserName": {
                "type": "keyword"
            },
            "submissionChannel": {
                "type": "keyword"
            },
            "submissionTime": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss"
            },
            "submitOrgName": {
                "type": "keyword"
            },
            "isSubmit": {
                "type": "integer"
            },
            "auditStatus": {
                "type": "integer"
            },
            "isWarehouse": {
                "type": "integer"
            },
            "auditReason": {
                "type": "text",
                "analyzer": "standard"
            },
            "auditUserName": {
                "type": "keyword"
            },
            "auditDeptName": {
                "type": "keyword"
            },
            "auditTime": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss"
            },
            "createTime": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss"
            },
            "updateTime": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss"
            },
            "deleted": {
                "type": "integer"
            },
            "isVerify": {
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
    print("重新创建 ES 索引并导入数据")
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
            class_report_list = ''
            if class_report_1:
                class_report_list = class_report_1
                if class_report_2:
                    class_report_list = f"{class_report_1}/{class_report_2}"
            
            submission_time = item.get('Submission_time', '') or item.get('submission_time', '')
            
            es_doc = {
                "id": doc_id,
                "number": item.get('number'),
                "eventName": item.get('event_name', ''),
                "classReport1": class_report_1,
                "classReport2": class_report_2,
                "classReportList": class_report_list,
                "classHuman1": '',
                "classHuman2": '',
                "classHumanList": '',
                "productsComponentsServices": item.get('products_components_services', ''),
                "operatingEntity": item.get('operating_entity', ''),
                "operatingEntityHuman": '',
                "riskDescription": item.get('risk_description', ''),
                "riskDescriptionHuman": '',
                "sourceUrl": item.get('source_url', ''),
                "sourceWebsite": item.get('source_website', ''),
                "paperTitle": item.get('paper_title', ''),
                "researchTeam": item.get('research_team', ''),
                "content": item.get('content', ''),
                "submitUserName": item.get('submit_user_name', ''),
                "submissionChannel": item.get('submission_channel', ''),
                "submissionTime": submission_time if submission_time else None,
                "submitOrgName": '',
                "isSubmit": convert_yes_no_to_int(item.get('is_submit', '')),
                "auditStatus": 10,
                "isWarehouse": 0,
                "auditReason": '',
                "auditUserName": '',
                "auditDeptName": '',
                "auditTime": None,
                "createTime": current_time,
                "updateTime": current_time,
                "deleted": 0,
                "isVerify": convert_yes_no_to_int(item.get('is_verify', ''))
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
    except Exception as e:
        print(f"   ❌ 验证失败: {str(e)}")

if __name__ == "__main__":
    main()
