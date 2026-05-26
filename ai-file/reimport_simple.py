import json
import uuid
from datetime import datetime
import subprocess
import sys

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

# 删除旧索引
print("删除旧索引...")
run_curl("DELETE", f"/{index_name}")

# 创建新索引
print("创建新索引...")
mapping = '''{
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
            "createdTime": {"type": "date"},
            "updatedTime": {"type": "date"}
        }
    }
}'''
run_curl("PUT", f"/{index_name}", mapping)

# 导入数据
print("导入数据...")
count = 0

with open('/Users/tom/Downloads/test.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        
        data = json.loads(line)
        
        class_name_list = []
        if 'class_name' in data and data['class_name']:
            class_name_str = data['class_name']
            class_name_list = [tag.strip() for tag in class_name_str.split(';') if tag.strip()]
        
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
        
        doc_json = json.dumps(doc, ensure_ascii=False)
        run_curl("PUT", f"/{index_name}/_doc/{doc['id']}", doc_json)
        
        count += 1
        print(f"已导入 {count} 条数据，classNameModel: {class_name_list}")

print(f"\n导入完成！共导入 {count} 条数据")
