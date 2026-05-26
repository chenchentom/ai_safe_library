from elasticsearch import Elasticsearch
from datetime import datetime
import uuid
import json

es = Elasticsearch(
    ['https://localhost:9200'],
    basic_auth=('elastic', 'V4h_Am00B-eNpeE5OSf*'),
    verify_certs=False
)

index_name = 'biz_risk_clue'

print("删除旧索引...")
if es.indices.exists(index=index_name):
    es.indices.delete(index=index_name)
    print(f"索引 {index_name} 已删除")

print("创建新索引...")
mapping = {
    "mappings": {
        "properties": {
            "id": {"type": "keyword"},
            "title": {"type": "text", "analyzer": "standard"},
            "content": {"type": "text", "analyzer": "standard"},
            "summary": {"type": "text", "analyzer": "standard"},
            "url": {"type": "keyword"},
            "sourceType": {"type": "keyword"},
            "reportUnit": {"type": "keyword"},
            "riskLevel": {"type": "keyword"},
            "reviewStatus": {"type": "integer"},
            "tags": {"type": "keyword"},
            "classNameModel": {"type": "keyword"},
            "classNameHuman": {"type": "keyword"},
            "createdTime": {"type": "date", "format": "yyyy-MM-dd'T'HH:mm:ss"},
            "updatedTime": {"type": "date", "format": "yyyy-MM-dd'T'HH:mm:ss"}
        }
    }
}

es.indices.create(index=index_name, body=mapping)
print(f"索引 {index_name} 已创建")

print("导入数据...")
count = 0

with open('/Users/tom/Downloads/test.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        
        data = json.loads(line)
        
        # 处理 class_name 字段，按分号分割
        class_name_list = []
        if 'class_name' in data and data['class_name']:
            class_name_str = data['class_name']
            class_name_list = [tag.strip() for tag in class_name_str.split(';') if tag.strip()]
        
        # 处理 pub_time
        created_time = datetime.now()
        if 'pub_time' in data and data['pub_time']:
            try:
                created_time = datetime.strptime(data['pub_time'], '%Y-%m-%d %H:%M:%S')
            except:
                pass
        
        doc = {
            "id": str(uuid.uuid4()),
            "title": data.get('title', ''),
            "content": data.get('content', ''),
            "summary": data.get('summary', ''),
            "url": data.get('url', ''),
            "sourceType": "crawl",
            "reportUnit": "系统自动导入",
            "riskLevel": "info",
            "reviewStatus": 10,
            "tags": [],
            "classNameModel": class_name_list,
            "classNameHuman": [],
            "createdTime": created_time.isoformat(),
            "updatedTime": datetime.now().isoformat()
        }
        
        es.index(index=index_name, id=doc['id'], document=doc)
        count += 1
        print(f"已导入 {count} 条数据，classNameModel: {class_name_list}")

print(f"\n导入完成！共导入 {count} 条数据")
