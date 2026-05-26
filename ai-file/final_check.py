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
print("最后剩余的3条记录")
print("=" * 100)

remaining = []
for item in jsonl_items:
    content = item.get('content', '').strip()
    if not content:
        remaining.append(item)
        print(f"\n[记录 {item.get('number')}] {item.get('event_name', '')}")
        print(f"    source_url: {item.get('source_url', '')}")
        print(f"    risk_description: {item.get('risk_description', '')[:200]}...")

print(f"\n" + "=" * 100)
print(f"剩余: {len(remaining)} 条")
print("=" * 100)
