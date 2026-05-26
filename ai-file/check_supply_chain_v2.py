#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import pymysql

DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'ai_safe_library',
    'charset': 'utf8mb4'
}

conn = pymysql.connect(**DB_CONFIG)
cursor = conn.cursor()

print('表: biz_supply_chain_tag_v2')
print('=' * 80)

cursor.execute('SELECT COUNT(*) FROM biz_supply_chain_tag_v2')
count = cursor.fetchone()[0]
print(f'数据总量: {count} 条')

if count > 0:
    print('\n前 20 条数据:')
    cursor.execute('SELECT id, parent_id, module, tag_name, tag_code, tag_level, status FROM biz_supply_chain_tag_v2 LIMIT 20')
    for row in cursor.fetchall():
        print(f'  id={row[0]}, parent_id={row[1]}, module={row[2]}, name={row[3]}, code={row[4]}, level={row[5]}, status={row[6]}')

    print('\n按 module 分组统计:')
    cursor.execute('SELECT module, COUNT(*) as cnt FROM biz_supply_chain_tag_v2 GROUP BY module')
    for row in cursor.fetchall():
        print(f'  {row[0]:<20} → {row[1]} 条')

    print('\n一级分类 (parent_id=0):')
    cursor.execute('SELECT tag_name, tag_code FROM biz_supply_chain_tag_v2 WHERE parent_id = 0 ORDER BY sort_order')
    for row in cursor.fetchall():
        print(f'  - {row[0]} ({row[1]})')

cursor.close()
conn.close()
