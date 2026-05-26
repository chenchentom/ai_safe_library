#!/usr/bin/env python3
import json
import pandas as pd

excel_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/标签体系.xlsx'
jsonl_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1.jsonl'

def parse_tag_name(tag_name):
    if not tag_name:
        return ''
    parts = tag_name.split('-', 1)
    if len(parts) > 1:
        return parts[1].strip()
    return tag_name.strip()

def main():
    print("=" * 100)
    print("JSONL标签与系统标签体系核对报告")
    print("=" * 100)
    
    df = pd.read_excel(excel_path, sheet_name='Sheet1')
    
    system_primary_with_code = {}
    system_secondary_with_code = {}
    primary_to_secondary = {}
    
    for _, row in df.iterrows():
        primary_full = row['一级分类']
        secondary_full = row['二级分类']
        
        primary_name = parse_tag_name(primary_full)
        secondary_name = parse_tag_name(secondary_full)
        
        system_primary_with_code[primary_name] = primary_full
        system_secondary_with_code[secondary_name] = secondary_full
        
        if primary_name not in primary_to_secondary:
            primary_to_secondary[primary_name] = set()
        primary_to_secondary[primary_name].add(secondary_name)
    
    system_primary_names = set(system_primary_with_code.keys())
    system_secondary_names = set(system_secondary_with_code.keys())
    
    print(f"\n系统一级标签 (共{len(system_primary_names)}个):")
    for name in sorted(system_primary_names):
        print(f"  - {system_primary_with_code[name]}")
    
    print(f"\n系统二级标签 (共{len(system_secondary_names)}个):")
    
    jsonl_items = []
    with open(jsonl_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line:
                item = json.loads(line)
                jsonl_items.append(item)
    
    print(f"\n\nJSONL文件记录数: {len(jsonl_items)}")
    
    jsonl_primary = set()
    jsonl_secondary = set()
    mismatches = []
    
    for idx, item in enumerate(jsonl_items, 1):
        c1 = item.get('class_1', '').strip()
        c2 = item.get('class_2', '').strip()
        c2 = c2.replace('\r', '').replace('\n', '')
        
        jsonl_primary.add(c1)
        jsonl_secondary.add(c2)
        
        issues = []
        
        if c1 not in system_primary_names:
            issues.append(f'一级标签「{c1}」不在系统中')
        
        if c2 not in system_secondary_names:
            issues.append(f'二级标签「{c2}」不在系统中')
        elif c1 in system_primary_names and c2 not in primary_to_secondary.get(c1, set()):
            pass
        
        if issues:
            mismatches.append({
                'number': item.get('number', idx),
                'event_name': item.get('event_name', ''),
                'class_1': c1,
                'class_2': c2,
                'issues': issues
            })
    
    print(f"\n\nJSONL中使用的一级标签:")
    for tag in sorted(jsonl_primary):
        status = "✅" if tag in system_primary_names else "❌"
        print(f"  {status} {tag}")
    
    print(f"\n\nJSONL中使用的二级标签:")
    for tag in sorted(jsonl_secondary):
        status = "✅" if tag in system_secondary_names else "❌"
        print(f"  {status} {tag}")
    
    print(f"\n\n" + "=" * 100)
    print("核对结果汇总")
    print("=" * 100)
    print(f"  总记录数: {len(jsonl_items)}")
    print(f"  正常匹配: {len(jsonl_items) - len(mismatches)}")
    print(f"  不匹配: {len(mismatches)}")
    
    if mismatches:
        print(f"\n\n" + "=" * 100)
        print(f"不匹配的记录 (共{len(mismatches)}条):")
        print("=" * 100)
        for m in mismatches:
            print(f"\n  [记录 {m['number']}] {m['event_name']}")
            print(f"    class_1: {m['class_1']}")
            print(f"    class_2: {m['class_2']}")
            print(f"    问题:")
            for issue in m['issues']:
                print(f"      - {issue}")
    
    print(f"\n\n" + "=" * 100)
    print("问题标签统计")
    print("=" * 100)
    
    missing_primary = jsonl_primary - system_primary_names
    if missing_primary:
        print(f"\n  ❌ 不在系统中的一级标签:")
        for tag in sorted(missing_primary):
            count = sum(1 for item in jsonl_items if item.get('class_1', '').strip() == tag)
            print(f"      - {tag} (出现 {count} 次)")
    
    missing_secondary = jsonl_secondary - system_secondary_names
    if missing_secondary:
        print(f"\n  ❌ 不在系统中的二级标签:")
        for tag in sorted(missing_secondary):
            count = sum(1 for item in jsonl_items 
                       if item.get('class_2', '').strip().replace('\r', '').replace('\n', '') == tag)
            print(f"      - {tag} (出现 {count} 次)")

if __name__ == "__main__":
    main()
