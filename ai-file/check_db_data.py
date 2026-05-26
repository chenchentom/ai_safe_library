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

def check_data():
    print("=" * 80)
    print("检查数据库中的风险线索标签数据")
    print("=" * 80)
    
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor(pymysql.cursors.DictCursor)
    
    try:
        # 查询所有 risk_clue 模块的标签
        sql = "SELECT id, parent_id, parent_name, module, tag_name, tag_code, tag_level, description FROM biz_tag_category WHERE module = 'risk_clue' ORDER BY id"
        cursor.execute(sql)
        results = cursor.fetchall()
        
        print(f"\n共找到 {len(results)} 条数据:\n")
        print(f"{'ID':<5} {'父级ID':<8} {'父级名称':<15} {'标签名称':<20} {'编码':<10} {'层级':<5} {'描述':<20}")
        print("-" * 100)
        
        for row in results:
            parent_name = row['parent_name'] or '-'
            description = (row['description'] or '')[:18]
            print(f"{row['id']:<5} {str(row['parent_id']):<8} {parent_name:<15} {row['tag_name']:<20} {row['tag_code']:<10} {row['tag_level']:<5} {description:<20}")
        
        print("\n" + "=" * 80)
        
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()
    finally:
        cursor.close()
        conn.close()

if __name__ == "__main__":
    check_data()
