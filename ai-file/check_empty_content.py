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

print("=" * 80)
print("检查content为空的记录")
print("=" * 80)

empty_count = 0
empty_with_url_count = 0

for idx, item in enumerate(jsonl_items, 1):
    content = item.get('content', '').strip()
    source_url = item.get('source_url', '').strip()
    
    if not content:
        empty_count += 1
        if source_url:
            empty_with_url_count += 1
            print(f"  [记录 {idx}] {item.get('event_name', '')}")
            print(f"    URL: {source_url}")

print(f"\n" + "=" * 80)
print(f"  总记录数: {len(jsonl_items)}")
print(f"  content为空: {empty_count}")
print(f"  content为空且有source_url: {empty_with_url_count}")
print("=" * 80)
