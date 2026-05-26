#!/usr/bin/env python3
import json
import uuid
from datetime import datetime
import requests
from requests.packages.urllib3.exceptions import InsecureRequestWarning

# 禁用 SSL 警告
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

# ES 配置
ES_URL = "https://localhost:9200"
ES_USER = "elastic"
ES_PASS = "V4h_Am00B-eNpeE5OSf*"
INDEX_NAME = "biz_risk_clue"

# 统一的时间格式
FIXED_TIME = "2026-05-18 09:52:56"

def parse_class_name(class_name_str):
    """将 class_name 字符串转换为列表"""
    if not class_name_str:
        return []
    # 用分号分隔，去除空字符串
    tags = [tag.strip() for tag in class_name_str.split(';') if tag.strip()]
    return tags

def import_data():
    # 读取 JSONL 文件
    with open('/Users/tom/Downloads/test.jsonl', 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    print(f"读取到 {len(lines)} 条数据")
    
    success_count = 0
    for i, line in enumerate(lines, 1):
        try:
            line = line.strip()
            if not line:
                continue
            
            data = json.loads(line)
            
            # 构建 ES 文档
            doc = {
                "id": str(uuid.uuid4()),
                "title": data.get("title", ""),
                "content": data.get("content", ""),
                "summary": data.get("summary", ""),
                "url": data.get("url", ""),
                "siteName": data.get("site_name", ""),
                "sourceType": "crawl",  # 默认为开源网站
                "reportUnit": "系统自动导入",
                "riskLevel": "info",  # 默认信息级别
                "reviewStatus": 10,  # 待审核
                "tags": [],
                "classNameModel": [],
                "classNameHuman": parse_class_name(data.get("class_name", "")),
                "createdTime": FIXED_TIME,
                "updatedTime": FIXED_TIME
            }
            
            # 插入到 ES
            response = requests.post(
                f"{ES_URL}/{INDEX_NAME}/_doc/{doc['id']}",
                auth=(ES_USER, ES_PASS),
                json=doc,
                verify=False
            )
            
            if response.status_code in [200, 201]:
                success_count += 1
                print(f"[{i}/{len(lines)}] 成功导入: {doc['title'][:30]}... (站点: {doc['siteName']})")
            else:
                print(f"[{i}/{len(lines)}] 导入失败: {response.text}")
                
        except Exception as e:
            print(f"[{i}/{len(lines)}] 处理错误: {str(e)}")
    
    print(f"\n导入完成！成功: {success_count}/{len(lines)}")

if __name__ == "__main__":
    import_data()

