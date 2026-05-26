#!/usr/bin/env python3
import subprocess
import json
import time

def test_bb_browser():
    print("=" * 80)
    print("测试 bb-browser")
    print("=" * 80)
    
    # 测试列出 adapter
    print("\n1. 测试列出可用 adapters...")
    try:
        result = subprocess.run(
            ['bb-browser', 'site', 'list'],
            capture_output=True, text=True, timeout=10
        )
        if result.returncode == 0:
            print("   ✅ bb-browser 可用")
            print(f"   输出前200字符: {result.stdout[:200]}...")
        else:
            print(f"   ❌ 错误: {result.stderr}")
    except Exception as e:
        print(f"   ❌ 异常: {str(e)}")
    
    # 测试打开一个网页
    print("\n2. 尝试打开一个网页...")
    test_url = "https://example.com"
    try:
        result = subprocess.run(
            ['bb-browser', 'open', test_url, '--tab'],
            capture_output=True, text=True, timeout=15
        )
        if result.returncode == 0:
            print(f"   ✅ 成功打开: {test_url}")
            time.sleep(2)
            
            # 尝试获取页面内容
            print("\n3. 尝试获取页面内容...")
            result_eval = subprocess.run(
                ['bb-browser', 'eval', 'document.body.innerText'],
                capture_output=True, text=True, timeout=10
            )
            if result_eval.returncode == 0:
                print(f"   ✅ 获取内容成功: {len(result_eval.stdout)} 字符")
                print(f"   内容预览: {result_eval.stdout[:100]}...")
            else:
                print(f"   ❌ 获取内容失败: {result_eval.stderr}")
        else:
            print(f"   ❌ 打开失败: {result.stderr}")
    except Exception as e:
        print(f"   ❌ 异常: {str(e)}")

if __name__ == "__main__":
    test_bb_browser()
