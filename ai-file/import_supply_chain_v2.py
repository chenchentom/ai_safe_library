#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
供应链标签 1.0 数据导入脚本
从 Excel 导入数据到 biz_tag_category 表
"""

import pandas as pd
import pymysql
import sys
from datetime import datetime

# 数据库配置（请根据实际情况修改）
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'ai_safe_library',
    'charset': 'utf8mb4'
}

MODULE = 'supply_chain_v2'

def get_connection():
    """获取数据库连接"""
    return pymysql.connect(**DB_CONFIG)

def clear_existing_data(conn):
    """清除该模块的现有数据"""
    cursor = conn.cursor()
    try:
        cursor.execute("DELETE FROM biz_tag_category WHERE module = %s", (MODULE,))
        conn.commit()
        print(f"已清除 {cursor.rowcount} 条现有记录")
    except Exception as e:
        print(f"清除数据失败: {e}")
        conn.rollback()
    finally:
        cursor.close()

def generate_tag_code(name):
    """生成标签编码"""
    import re
    # 移除特殊字符，转成拼音或英文标识
    # 这里简化处理：使用中文拼音首字母
    code = re.sub(r'[^\w]', '', name)
    # 简单的拼音首字母映射（常用字）
    pinyin_map = {
        '数': 'shu', '据': 'ju', '采': 'cai', '集': 'ji',
        '清': 'qing', '洗': 'xi', '与': 'yu', '处': 'chu', '理': 'li',
        '网': 'wang', '页': 'ye', '抓': 'zhua', '取': 'qu', '爬': 'pa', '虫': 'chong',
        '合': 'he', '成': 'cheng', '生': 'sheng', '平': 'ping', '台': 'tai',
        '文': 'wen', '本': 'ben', '工': 'gong', '具': 'ju',
        '去': 'qu', '重': 'chong', '质': 'zhi', '量': 'liang', '过': 'guo', '滤': 'lv'
    }
    result = []
    for char in name:
        if char in pinyin_map:
            result.append(pinyin_map[char])
        else:
            result.append(char.lower())
    code = '_'.join(result)
    # 限制长度
    return code[:50]

def insert_tag(conn, parent_id, tag_name, tag_code, tag_level, tag_path, sort_order):
    """插入标签"""
    cursor = conn.cursor()
    now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    sql = """
    INSERT INTO biz_tag_category 
    (parent_id, module, tag_name, tag_code, tag_level, tag_path, description, icon, sort_order, status, create_time, update_time, del_flag)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    try:
        cursor.execute(sql, (
            parent_id,
            MODULE,
            tag_name,
            tag_code,
            tag_level,
            tag_path,
            '',
            '',
            sort_order,
            '0',
            now,
            now,
            '0'
        ))
        conn.commit()
        return cursor.lastrowid
    except Exception as e:
        print(f"插入标签失败 [{tag_name}]: {e}")
        conn.rollback()
        return None
    finally:
        cursor.close()

def main():
    excel_file = '供应链标签1.xlsx'
    
    print(f"读取 Excel 文件: {excel_file}")
    try:
        df = pd.read_excel(excel_file)
        print(f"读取到 {len(df)} 行数据")
        print(f"列名: {df.columns.tolist()}")
    except Exception as e:
        print(f"读取 Excel 失败: {e}")
        return 1

    # 去重
    df = df.drop_duplicates()
    print(f"去重后剩余 {len(df)} 行")

    # 构建树形结构
    level1_map = {}  # 一级分类名称 -> id
    level2_map = {}  # (一级, 二级) -> id

    conn = get_connection()
    try:
        # 先清除现有数据
        clear_existing_data(conn)

        # 先插入所有一级分类
        level1_names = df['一级分类'].unique()
        print(f"\n一级分类数量: {len(level1_names)}")
        
        for idx, level1_name in enumerate(level1_names):
            tag_code = generate_tag_code(level1_name)
            tag_id = insert_tag(
                conn=conn,
                parent_id=0,
                tag_name=level1_name,
                tag_code=tag_code,
                tag_level=0,
                tag_path=f'/{idx + 1}',
                sort_order=idx
            )
            if tag_id:
                level1_map[level1_name] = tag_id
                print(f"  插入一级分类: {level1_name} (id={tag_id})")

        # 再插入二级分类
        print(f"\n开始插入二级分类...")
        level2_counter = {}
        
        for _, row in df.iterrows():
            level1_name = row['一级分类']
            level2_name = row['二级分类']
            
            if pd.isna(level1_name) or pd.isna(level2_name):
                continue
                
            key = (level1_name, level2_name)
            if key in level2_map:
                continue
                
            if level1_name not in level1_map:
                continue
                
            parent_id = level1_map[level1_name]
            
            if level1_name not in level2_counter:
                level2_counter[level1_name] = 0
            level2_counter[level1_name] += 1
            
            tag_code = generate_tag_code(f"{level1_name}_{level2_name}")
            parent_tag = None
            
            cursor = conn.cursor()
            try:
                cursor.execute("SELECT tag_path FROM biz_tag_category WHERE id = %s", (parent_id,))
                result = cursor.fetchone()
                if result:
                    parent_tag_path = result[0]
                else:
                    parent_tag_path = f'/{parent_id}'
            finally:
                cursor.close()
            
            tag_id = insert_tag(
                conn=conn,
                parent_id=parent_id,
                tag_name=level2_name,
                tag_code=tag_code,
                tag_level=1,
                tag_path=f'{parent_tag_path}/{level2_counter[level1_name]}',
                sort_order=level2_counter[level1_name] - 1
            )
            
            if tag_id:
                level2_map[key] = tag_id
                print(f"  插入二级分类: {level1_name} -> {level2_name} (id={tag_id})")

        print(f"\n导入完成!")
        print(f"  一级分类: {len(level1_map)} 个")
        print(f"  二级分类: {len(level2_map)} 个")
        print(f"  总计: {len(level1_map) + len(level2_map)} 个标签")
        
    except Exception as e:
        print(f"导入过程出错: {e}")
        import traceback
        traceback.print_exc()
        return 1
    finally:
        conn.close()
    
    return 0

if __name__ == '__main__':
    sys.exit(main())
