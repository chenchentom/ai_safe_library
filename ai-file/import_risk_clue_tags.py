#!/usr/bin/env python3
import pandas as pd
import pymysql
from datetime import datetime

# 数据库配置 - 根据你的实际配置修改
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'ai_safe_library',
    'charset': 'utf8mb4'
}

excel_path = '/Volumes/mac/code/java/ai_safe_library/标签体系.xlsx'
module = 'risk_clue'


def parse_tag_code(tag_name):
    """从标签名称中提取编码（字母+编号）"""
    if not tag_name:
        return ''
    # 分割 "-"，取前面的部分
    parts = tag_name.split('-', 1)
    if len(parts) > 1:
        return parts[0].strip()
    return ''


def parse_tag_name(tag_name):
    """从标签名称中提取纯名称（去掉编码前缀）"""
    if not tag_name:
        return ''
    # 分割 "-"，取后面的部分
    parts = tag_name.split('-', 1)
    if len(parts) > 1:
        return parts[1].strip()
    return tag_name.strip()


def import_tags():
    print("=" * 80)
    print("开始导入风险线索标签数据")
    print("=" * 80)

    # 1. 读取 Excel
    print("\n[1/4] 读取 Excel 文件...")
    df_tags = pd.read_excel(excel_path, sheet_name='Sheet1')
    df_desc = pd.read_excel(excel_path, sheet_name='Sheet2')

    print(f"  - 标签数据行数: {len(df_tags)}")
    print(f"  - 描述数据行数: {len(df_desc)}")

    # 2. 建立描述映射
    print("\n[2/4] 建立描述映射...")
    desc_map = {}
    for _, row in df_desc.iterrows():
        category_name = row['一级分类']
        description = row['描述']
        desc_map[category_name] = description
    print(f"  - 描述映射建立完成，共 {len(desc_map)} 条")

    # 3. 连接数据库
    print("\n[3/4] 连接数据库...")
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    try:
        # 先清空该模块的旧数据
        print("  - 清空旧数据...")
        cursor.execute(f"DELETE FROM biz_tag_category WHERE module = '{module}'")
        conn.commit()

        # 4. 处理并导入数据
        print("\n[4/4] 导入标签数据...")

        # 第一步：收集所有唯一的一级分类
        primary_categories = {}
        for _, row in df_tags.iterrows():
            primary_full = row['一级分类']
            primary_code = parse_tag_code(primary_full)
            primary_name = parse_tag_name(primary_full)

            if primary_name not in primary_categories:
                primary_categories[primary_name] = {
                    'code': primary_code,
                    'name': primary_name,
                    'description': desc_map.get(primary_name, '')
                }

        print(f"  - 一级分类数量: {len(primary_categories)}")

        # 第二步：插入一级分类
        tag_id = 1
        primary_id_map = {}

        for primary_name, primary_info in primary_categories.items():
            sql = """
            INSERT INTO biz_tag_category 
            (id, parent_id, module, tag_name, tag_code, tag_level, tag_path, 
             description, parent_name, icon, sort_order, status, create_time, update_time)
            VALUES 
            (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """

            tag_path = f"/{tag_id}"

            cursor.execute(sql, (
                tag_id,
                0,
                module,
                primary_info['name'],
                primary_info['code'],
                0,
                tag_path,
                primary_info['description'],
                '',
                'Folder',
                tag_id,
                '0',
                datetime.now(),
                datetime.now()
            ))

            primary_id_map[primary_name] = tag_id
            print(f"    ✓ 一级分类: {primary_info['code']}-{primary_info['name']} (ID: {tag_id})")
            tag_id += 1

        # 第三步：插入二级分类
        secondary_count = 0
        for _, row in df_tags.iterrows():
            primary_full = row['一级分类']
            secondary_full = row['二级分类']

            primary_name = parse_tag_name(primary_full)
            secondary_code = parse_tag_code(secondary_full)
            secondary_name = parse_tag_name(secondary_full)

            parent_id = primary_id_map.get(primary_name, 0)
            tag_path = f"/{parent_id}/{tag_id}"

            sql = """
            INSERT INTO biz_tag_category 
            (id, parent_id, module, tag_name, tag_code, tag_level, tag_path, 
             description, parent_name, icon, sort_order, status, create_time, update_time)
            VALUES 
            (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """

            cursor.execute(sql, (
                tag_id,
                parent_id,
                module,
                secondary_name,
                secondary_code,
                1,
                tag_path,
                '',
                primary_name,
                'PriceTag',
                secondary_count + 1,
                '0',
                datetime.now(),
                datetime.now()
            ))

            print(f"    ✓ 二级分类: {secondary_code}-{secondary_name} (父级: {primary_name}, ID: {tag_id})")
            tag_id += 1
            secondary_count += 1

        conn.commit()

        print("\n" + "=" * 80)
        print("✅ 导入完成！")
        print(f"  - 一级分类: {len(primary_categories)} 条")
        print(f"  - 二级分类: {secondary_count} 条")
        print(f"  - 总计: {len(primary_categories) + secondary_count} 条")
        print("=" * 80)

    except Exception as e:
        conn.rollback()
        print(f"\n❌ 导入失败: {e}")
        import traceback
        traceback.print_exc()
    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":
    import_tags()
