#!/bin/bash

ES_URL="https://localhost:9200"
ES_USER="elastic"
ES_PASS="V4h_Am00B-eNpeE5OSf*"
INDEX="biz_risk_clue"

echo "删除旧索引..."
curl -k -u "${ES_USER}:${ES_PASS}" -X DELETE "${ES_URL}/${INDEX}" 2>/dev/null

echo -e "\n创建新索引..."
curl -k -u "${ES_USER}:${ES_PASS}" -X PUT "${ES_URL}/${INDEX}" \
  -H "Content-Type: application/json" \
  -d '{
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
        "createdTime": {"type": "date", "format": "yyyy-MM-dd'\''T'\''HH:mm:ss"},
        "updatedTime": {"type": "date", "format": "yyyy-MM-dd'\''T'\''HH:mm:ss"}
      }
    }
  }' 2>/dev/null

echo -e "\n导入数据..."
count=0

python3 -c "
import json
import uuid
from datetime import datetime

def generate_uuid():
    return str(uuid.uuid4())

def format_time(time_str):
    if not time_str:
        return datetime.now().isoformat()
    try:
        dt = datetime.strptime(time_str, '%Y-%m-%d %H:%M:%S')
        return dt.isoformat()
    except:
        return datetime.now().isoformat()

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
        
        doc = {
            'id': generate_uuid(),
            'title': data.get('title', ''),
            'content': data.get('content', ''),
            'summary': data.get('summary', ''),
            'url': data.get('url', ''),
            'sourceType': 'crawl',
            'reportUnit': '系统自动导入',
            'riskLevel': 'info',
            'reviewStatus': 10,
            'tags': [],
            'classNameModel': class_name_list,
            'classNameHuman': [],
            'createdTime': format_time(data.get('pub_time')),
            'updatedTime': datetime.now().isoformat()
        }
        
        print(json.dumps(doc, ensure_ascii=False))
" | while read -r doc_json; do
    if [ -z "$doc_json" ]; then
        continue
    fi
    
    doc_id=$(echo "$doc_json" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
    
    curl -k -u "${ES_USER}:${ES_PASS}" -X PUT "${ES_URL}/${INDEX}/_doc/${doc_id}" \
      -H "Content-Type: application/json" \
      -d "$doc_json" -s > /dev/null
    
    count=$((count + 1))
    echo "已导入 ${count} 条数据"
done

echo -e "\n导入完成！共导入 ${count} 条数据"
