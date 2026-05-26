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
print("剩余需要处理的58条记录详情")
print("=" * 100)

remaining_count = 0
for idx, item in enumerate(jsonl_items, 1):
    content = item.get('content', '').strip()
    source_url = item.get('source_url', '').strip()
    
    if not content and source_url:
        remaining_count += 1
        print(f"\n[{remaining_count}] 记录 {idx}: {item.get('event_name', '')}")
        print(f"    URL: {source_url}")
        print(f"    网站: {item.get('source_website', '')}")

print(f"\n" + "=" * 100)
print(f"总计剩余: {remaining_count} 条")
print("=" * 100)
