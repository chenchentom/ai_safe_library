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

print('数据库中的所有表:')
print('=' * 80)
cursor.execute('SHOW TABLES')
tables = cursor.fetchall()
for table in tables:
    print(f'  - {table[0]}')

print('\n' + '=' * 80)
print('检查 biz_supply_chain_tag_v2 表是否存在:')
cursor.execute("SHOW TABLES LIKE 'biz_supply_chain_tag_v2'")
result = cursor.fetchone()
if result:
    print(f'  ✓ 表存在: {result[0]}')
    print('\n表结构:')
    cursor.execute('DESCRIBE biz_supply_chain_tag_v2')
    for row in cursor.fetchall():
        print(f'  {row[0]:<20} {row[1]}')
else:
    print('  ✗ 表不存在')

cursor.close()
conn.close()
