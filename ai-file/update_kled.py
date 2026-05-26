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
print("更新记录 42")
print("=" * 100)

updates = {
    42: """# Kled.ai

Kled Raises $10M in Funding. Sourcing the largest licensable datasets. Powering leading AI labs, governments, and research institutions with verified human data.

## Key Features

- Get paid for your data
- Become a contributor
- Explore datasets
- Backed by researchers from top institutions

## Investors

Backed by top investors including:
- DIPLO (Grammy-winning DJ)
- 24KGOLDN (Platinum recording artist)
- ADAM COHEN (Founder of Stic)
- SEAN LI (CEO of Magic Labs)
- DANIEL GREENBERG (Founder of MSCHF)
- LAZARBEAM (Content creator, 23M Subs)
- And many more angel investors

## Blog Updates (May 2026)

- Kled Raises $10M In Funding
- LazarBeam joins Kled's Cap Table
- Kled Expands Enterprise Compliance Infrastructure
- Solana Spotlights Kled's Data Economy
- Kled Expands Into Voice Data
- Kled Is Expanding Beyond Traditional Gig Work

Kled operates a human data marketplace where users can upload their photos, videos, and other data to earn income, which is then licensed to AI labs and research institutions."""
}

updated_count = 0
for item in jsonl_items:
    record_num = item.get('number')
    if record_num in updates:
        current_content = item.get('content', '').strip()
        risk_desc = item.get('risk_description', '').strip()
        
        if current_content == risk_desc and len(current_content) > 0:
            item['content'] = updates[record_num]
            updated_count += 1
            print(f"  [记录 {record_num}] {item.get('event_name', '')[:50]} - ✅ 更新成功")

print(f"\n" + "=" * 100)
print(f"  总计更新: {updated_count} 条")
print("=" * 100)

with open(jsonl_path, 'w', encoding='utf-8') as f:
    for item in jsonl_items:
        f.write(json.dumps(item, ensure_ascii=False) + '\n')

print(f"\n✅ 文件已更新: {jsonl_path}")
