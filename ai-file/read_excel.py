#!/usr/bin/env python3
import pandas as pd
import sys

excel_path = '/Volumes/mac/code/java/ai_safe_library/标签体系.xlsx'

print(f"正在读取 Excel 文件: {excel_path}")
print("=" * 80)

try:
    xl = pd.ExcelFile(excel_path)
    print(f"工作表列表: {xl.sheet_names}")
    print("=" * 80)
    
    for sheet_name in xl.sheet_names:
        print(f"\n工作表: {sheet_name}")
        print("-" * 80)
        
        df = pd.read_excel(excel_path, sheet_name=sheet_name)
        
        print(f"列名: {list(df.columns)}")
        print(f"行数: {len(df)}")
        print("\n前 10 行数据:")
        print(df.head(10).to_string())
        print("=" * 80)
        
except Exception as e:
    print(f"错误: {e}")
    import traceback
    traceback.print_exc()
