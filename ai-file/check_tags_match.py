#!/usr/bin/env python3
import json
import pandas as pd

# 读取标签体系Excel
excel_path = '/Volumes/mac/code/java/ai_safe_library/标签体系.xlsx'
jsonl_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1.jsonl'

def parse_tag_name(tag_name):
    """从标签名称中提取纯名称（去掉编码前缀）"""
    if not tag_name:
        return ''
    parts = tag_name.split('-', 1)
    if len(parts) > 1:
        return parts[1].strip()
    return tag_name.strip()

def main():
    print("=" * 80)
    print("标签核对结果")
    print("=" * 80)
    
    # 读取标签体系
    print("\n[1/3] 读取标签体系...")
    df_tags = pd.read_excel(excel_path, sheet_name='Sheet1')
    
    # 构建系统标签集合
    system_primary = set()
    system_secondary = set()
    primary_to_secondary = {}
    
    for _, row in df_tags.iterrows():
        primary_full = row['一级分类']
        secondary_full = row['二级分类']
        
        primary_name = parse_tag_name(primary_full)
        secondary_name = parse_tag_name(secondary_full)
        
        system_primary.add(primary_name)
        system_secondary.add(secondary_name)
        
        if primary_name not in primary_to_secondary:
            primary_to_secondary[primary_name] = set()
        primary_to_secondary[primary_name].add(secondary_name)
    
    print(f"  - 系统一级标签: {len(system_primary)} 个")
    print(f"  - 系统二级标签: {len(system_secondary)} 个")
    print(f"\n  系统一级标签列表: {sorted(list(system_primary))}")
    
    # 读取JSONL文件
    print("\n[2/3] 读取JSONL文件...")
    jsonl_items = []
    with open(jsonl_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line:
                try:
                    item = json.loads(line)
                    jsonl_items.append(item)
                except Exception as e:
                    print(f"    警告: 解析错误 - {e}")
    
    print(f"  - JSONL记录数: {len(jsonl_items)}")
    
    # 收集JSONL中的标签
    jsonl_primary = set()
    jsonl_secondary = set()
    mismatches = []
    
    for idx, item in enumerate(jsonl_items, 1):
        c1 = item.get('class_1', '').strip()
        c2 = item.get('class_2', '').strip()
        
        # 移除可能的换行符
        c2 = c2.replace('\r', '').replace('\n', '')
        
        jsonl_primary.add(c1)
        jsonl_secondary.add(c2)
        
        # 检查是否匹配
        mismatch_info = {
            'number': item.get('number', idx),
            'event_name': item.get('event_name', ''),
            'class_1': c1,
            'class_2': c2,
            'issues': []
        }
        
        # 检查一级标签
        if c1 not in system_primary:
            mismatch_info['issues'].append(f'一级标签「{c1}」不在系统标签中')
        
        # 检查二级标签
        if c2 not in system_secondary:
            mismatch_info['issues'].append(f'二级标签「{c2}」不在系统标签中')
        elif c1 in system_primary and c2 not in primary_to_secondary.get(c1, set()):
            pass
        elif c1 in system_primary and c2 not in primary_to_secondary.get(c1, set()):
            pass
        elif c1 in system_primary:
            mismatch_info['issues'].append(f'二级标签「{c2}」不属于一级标签「{c1}」的下属分类')
        
        if mismatch_info['issues']:
            mismatches.append(mismatch_info)
    
    print(f"\n  JSONL一级标签: {sorted(list(jsonl_primary))}")
    print(f"  JSONL二级标签: {sorted(list(jsonl_secondary))}")
    
    # 输出核对结果
    print("\n[3/3] 核对结果...")
    print(f"\n  ✅ 正常匹配记录数: {len(jsonl_items) - len(mismatches)}")
    print(f"  ❌ 不匹配记录数: {len(mismatches)}")
    
    if mismatches:
        print(f"\n" + "=" * 80)
        print("不匹配记录详情:")
        print("=" * 80)
        for m in mismatches:
            print(f"\n  [记录 {m['number']}] {m['event_name']}")
            print(f"    class_1: {m['class_1']}")
            print(f"    class_2: {m['class_2']}")
            print(f"    问题:")
            for issue in m['issues']:
                print(f"      - {issue}")
        
        # 统计各类不匹配原因
        print(f"\n" + "=" * 80)
        print("不匹配标签统计:")
        print("=" * 80)
        
        # 统计不在系统中的一级标签
        missing_primary = jsonl_primary - system_primary
        if missing_primary:
            print(f"\n  不在系统中的一级标签:")
            for tag in sorted(missing_primary):
                count = sum(1 for item in jsonl_items if item.get('class_1', '').strip() == tag)
                print(f"    - {tag} (出现 {count} 次)")
        
        # 统计不在系统中的二级标签
        missing_secondary = jsonl_secondary - system_secondary
        if missing_secondary:
            print(f"\n  不在系统中的二级标签:")
            for tag in sorted(missing_secondary):
                count = sum(1 for item in jsonl_items if item.get('class_2', '').strip().replace('\r', '').replace('\n', '') == tag)
                print(f"    - {tag} (出现 {count} 次)")

if __name__ == "__main__":
    main()
