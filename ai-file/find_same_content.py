#!/usr/bin/env python3
import json

jsonl_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1.jsonl'

jsonl_items = []
with open(jsonl_path, 'r', encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if line:
            item = json.loads(line)
            jsonl_items.append(item)

print("=" * 100)
print("查找 content == risk_description 且 source_url 不为空的记录")
print("=" * 100)

matching = []
for item in jsonl_items:
    content = item.get('content', '').strip()
    risk_desc = item.get('risk_description', '').strip()
    source_url = item.get('source_url', '').strip()
    
    if content and risk_desc and source_url and content == risk_desc:
        matching.append(item)
        print(f"\n[记录 {item.get('number')}] {item.get('event_name', '')}")
        print(f"    URL: {source_url[:70]}")

print(f"\n" + "=" * 100)
print(f"符合条件: {len(matching)} 条")
print("=" * 100)

# 保存到临时文件
with open('/tmp/matching_records.json', 'w', encoding='utf-8') as f:
    json.dump([{
        'number': item.get('number'),
        'event_name': item.get('event_name'),
        'source_url': item.get('source_url'),
        'content': item.get('content')
    } for item in matching], f, ensure_ascii=False, indent=2)

print(f"\n详情已保存到 /tmp/matching_records.json")
