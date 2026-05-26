#!/usr/bin/env python3
import requests
import warnings
warnings.filterwarnings("ignore")

print("=" * 100)
print("测试 ES 连接")
print("=" * 100)

urls_to_test = [
    "https://localhost:9200",
    "http://localhost:9200",
]

username = "elastic"
password = "V4h_Am00B-eNpeE5OSf*"

for url in urls_to_test:
    print(f"\n尝试连接: {url}")
    try:
        response = requests.get(
            url,
            auth=(username, password),
            verify=False,
            timeout=5
        )
        print(f"  状态码: {response.status_code}")
        print(f"  响应: {response.text[:200]}")
        if response.status_code == 200:
            print(f"  ✅ 连接成功!")
    except Exception as e:
        print(f"  ❌ 连接失败: {str(e)}")

print("\n" + "=" * 100)
