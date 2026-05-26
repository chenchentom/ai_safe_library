#!/usr/bin/env python3
import json
import uuid
import datetime
from elasticsearch import Elasticsearch

ES_URL = "https://localhost:9200"
ES_USERNAME = "elastic"
ES_PASSWORD = "V4h_Am00B-eNpeE5OSf*"
INDEX_NAME = "biz_risk_clue"
JSONL_PATH = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1.jsonl'

INDEX_SETTINGS = {
    "settings": {
        "number_of_shards": 3,
        "number_of_replicas": 1,
        "analysis": {
            "analyzer": {
                "default_analyzer": {
                    "type": "custom",
                    "tokenizer": "ik_max_word"
                }
            }
        }
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
                "analyzer": "ik_max_word",
                "search_analyzer": "ik_smart",
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
                "analyzer": "ik_max_word",
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
                "analyzer": "ik_max_word"
            },
            "riskDescriptionHuman": {
                "type": "text",
                "analyzer": "ik_max_word"
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
                "analyzer": "ik_max_word"
            },
            "researchTeam": {
                "type": "keyword"
            },
            "content": {
                "type": "text",
                "analyzer": "ik_max_word"
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
                "analyzer": "ik_max_word"
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

def main():
    print("=" * 100)
    print("重新创建 ES 索引并导入数据")
    print("=" * 100)
    
    print(f"\n1. 连接到 ES: {ES_URL}")
    es = Elasticsearch(
        ES_URL,
        basic_auth=(ES_USERNAME, ES_PASSWORD),
        verify_certs=False
    )
    
    if es.ping():
        print("   ✅ 连接成功")
    else:
        print("   ❌ 连接失败")
        return
    
    print(f"\n2. 删除旧索引: {INDEX_NAME}")
    if es.indices.exists(index=INDEX_NAME):
        es.indices.delete(index=INDEX_NAME)
        print("   ✅ 旧索引已删除")
    else:
        print("   ℹ️  旧索引不存在，跳过删除")
    
    print(f"\n3. 创建新索引: {INDEX_NAME}")
    es.indices.create(index=INDEX_NAME, body=INDEX_SETTINGS)
    print("   ✅ 新索引已创建")
    
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
            
            es.index(index=INDEX_NAME, id=doc_id, document=es_doc)
            success_count += 1
            if success_count % 10 == 0:
                print(f"   已导入 {success_count}/{len(jsonl_items)} 条...")
                
        except Exception as e:
            fail_count += 1
            print(f"   ❌ 导入失败 (记录 {item.get('number', '?')}): {str(e)[:100]}")
    
    print(f"\n" + "=" * 100)
    print(f"导入完成: {success_count} 条成功, {fail_count} 条失败")
    print("=" * 100)

if __name__ == "__main__":
    main()
