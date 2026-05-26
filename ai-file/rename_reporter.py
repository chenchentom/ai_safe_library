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
print("重命名 reporter 为 submit_user_name")
print("=" * 100)

updated_count = 0
for item in jsonl_items:
    if 'reporter' in item:
        item['submit_user_name'] = item.pop('reporter')
        updated_count += 1
        print(f"  [记录 {item.get('number')}] {item.get('event_name', '')[:50]} - ✅ 重命名成功")

print(f"\n" + "=" * 100)
print(f"  总计重命名: {updated_count} 条")
print("=" * 100)

with open(jsonl_path, 'w', encoding='utf-8') as f:
    for item in jsonl_items:
        f.write(json.dumps(item, ensure_ascii=False) + '\n')

print(f"\n✅ 文件已更新: {jsonl_path}")
