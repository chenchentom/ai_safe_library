#!/usr/bin/env python3
import pandas as pd

excel_path = '/Volumes/mac/code/java/ai_safe_library/标签体系.xlsx'

df = pd.read_excel(excel_path, sheet_name='Sheet1')
print("=" * 100)
print("系统标签体系")
print("=" * 100)
print()
print(f"总行数: {len(df)}")
print()
print("前20行:")
print(df.head(20).to_string())
print()
print("=" * 100)
print("一级标签统计:")
print(df['一级分类'].value_counts().to_string())
print()
print("=" * 100)
print("唯一一级标签:")
primary_tags = df['一级分类'].unique()
for tag in sorted(primary_tags):
    print(f"  - {tag}")
