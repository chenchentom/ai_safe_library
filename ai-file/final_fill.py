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
print("填充最后3条记录")
print("=" * 100)

updated_count = 0
for item in jsonl_items:
    record_num = item.get('number')
    content = item.get('content', '').strip()
    risk_desc = item.get('risk_description', '').strip()
    
    if not content and risk_desc:
        item['content'] = risk_desc
        updated_count += 1
        print(f"  [记录 {record_num}] {item.get('event_name', '')[:60]} - ✅ 填充成功")

print(f"\n" + "=" * 100)
print(f"  总计填充: {updated_count} 条")
print("=" * 100)

with open(jsonl_path, 'w', encoding='utf-8') as f:
    for item in jsonl_items:
        f.write(json.dumps(item, ensure_ascii=False) + '\n')

print(f"\n✅ 文件已更新: {jsonl_path}")

# 最终检查
print("\n" + "=" * 100)
print("最终检查")
print("=" * 100)

empty_count = 0
for item in jsonl_items:
    content = item.get('content', '').strip()
    if not content:
        empty_count += 1
        print(f"  [记录 {item.get('number')}] {item.get('event_name', '')}")

if empty_count == 0:
    print("\n  ✅ 所有记录都有 content 了！")
else:
    print(f"\n  剩余: {empty_count} 条")
print("=" * 100)
