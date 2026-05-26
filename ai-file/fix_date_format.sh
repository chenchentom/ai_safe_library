#!/bin/bash

ES_URL="https://localhost:9200"
ES_USER="elastic"
ES_PASS="V4h_Am00B-eNpeE5OSf*"
INDEX="biz_risk_clue"

echo "获取所有文档..."
response=$(curl -k -u "${ES_USER}:${ES_PASS}" "${ES_URL}/${INDEX}/_search?size=100" -s)

echo "$response" | python3 -c "
import sys, json, re
from datetime import datetime

data = json.load(sys.stdin)

for hit in data.get('hits', {}).get('hits', []):
    doc_id = hit['_id']
    doc = hit['_source']
    
    update = {}
    
    if 'createdTime' in doc and doc['createdTime']:
        try:
            dt = datetime.strptime(doc['createdTime'], '%Y-%m-%d %H:%M:%S')
            update['createdTime'] = dt.isoformat()
        except:
            pass
    
    if 'updatedTime' in doc and doc['updatedTime']:
        try:
            dt = datetime.strptime(doc['updatedTime'], '%Y-%m-%d %H:%M:%S')
            update['updatedTime'] = dt.isoformat()
        except:
            pass
    
    if update:
        print(f'Updating {doc_id}: {update}')
        import subprocess
        update_json = json.dumps({'doc': update})
        cmd = [
            'curl', '-k', '-u', f'{sys.argv[1]}:{sys.argv[2]}',
            f'{sys.argv[3]}/{sys.argv[4]}/_update/{doc_id}',
            '-H', 'Content-Type: application/json',
            '-d', update_json, '-s'
        ]
        subprocess.run(cmd)
" "$ES_USER" "$ES_PASS" "$ES_URL" "$INDEX"

echo "完成！"
