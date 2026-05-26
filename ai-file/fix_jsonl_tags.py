#!/usr/bin/env python3
import json
import pandas as pd

excel_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/标签体系.xlsx'
jsonl_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1.jsonl'
output_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1_fixed.jsonl'

def parse_tag_name(tag_name):
    if not tag_name:
        return ''
    parts = tag_name.split('-', 1)
    if len(parts) > 1:
        return parts[1].strip()
    return tag_name.strip()

def main():
    print("=" * 80)
    print("开始修复JSONL标签")
    print("=" * 80)
    
    df = pd.read_excel(excel_path, sheet_name='Sheet1')
    
    system_secondary_names = set()
    
    for _, row in df.iterrows():
        secondary_full = row['二级分类']
        secondary_name = parse_tag_name(secondary_full)
        system_secondary_names.add(secondary_name)
    
    print(f"  系统二级标签数量: {len(system_secondary_names)}")
    
    jsonl_items = []
    with open(jsonl_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line:
                item = json.loads(line)
                jsonl_items.append(item)
    
    print(f"  读取记录数: {len(jsonl_items)}")
    
    fixed_count = 0
    for idx, item in enumerate(jsonl_items, 1):
        original_c2 = item.get('class_2', '').strip()
        c2 = original_c2.replace('\r', '').replace('\n', '')
        
        new_c2 = c2
        
        if not c2:
            new_c2 = ''
        
        elif '、' in c2:
            tags = [t.strip() for t in c2.split('、')]
            if tags:
                first_tag = tags[0]
                first_tag = first_tag.replace('A2 ', '').replace('A4 ', '').replace('A5 ', '').replace('A1 ', '')
                first_tag = first_tag.replace('C2', '').replace('C3', '')
                first_tag = first_tag.strip()
                new_c2 = first_tag
                if new_c2 not in system_secondary_names:
                    new_c2 = ''
        
        elif c2 == '请求伪造':
            new_c2 = '服务端请求伪造'
        
        elif c2 == '违规内容生产':
            new_c2 = '违规内容生成'
        
        elif c2 not in system_secondary_names:
            new_c2 = ''
        
        if new_c2 != original_c2:
            fixed_count += 1
            print(f"  [记录 {idx}] {item.get('event_name', '')}")
            print(f"      原标签: '{original_c2}'")
            print(f"      新标签: '{new_c2}'")
        
        item['class_2'] = new_c2
    
    print(f"\n  修复记录数: {fixed_count}")
    
    with open(output_path, 'w', encoding='utf-8') as f:
        for item in jsonl_items:
            f.write(json.dumps(item, ensure_ascii=False) + '\n')
    
    print(f"\n" + "=" * 80)
    print(f"✅ 修复完成！")
    print(f"   输出文件: {output_path}")
    print("=" * 80)

if __name__ == "__main__":
    main()
