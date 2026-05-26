#!/usr/bin/env python3
import pymysql

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'ai_safe_library',
    'charset': 'utf8mb4'
}

def add_parent_name_field():
    print("正在添加 parent_name 字段...")
    
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    try:
        # 先检查字段是否已存在
        cursor.execute("SHOW COLUMNS FROM biz_tag_category LIKE 'parent_name'")
        result = cursor.fetchone()
        
        if result:
            print("✓ parent_name 字段已存在")
        else:
            # 添加字段
            sql = """
            ALTER TABLE biz_tag_category 
            ADD COLUMN parent_name VARCHAR(255) COMMENT '父级节点名称' 
            AFTER description
            """
            cursor.execute(sql)
            conn.commit()
            print("✓ 成功添加 parent_name 字段")
            
    except Exception as e:
        print(f"✗ 执行失败: {e}")
        import traceback
        traceback.print_exc()
    finally:
        cursor.close()
        conn.close()

if __name__ == "__main__":
    add_parent_name_field()
