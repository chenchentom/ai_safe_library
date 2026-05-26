#!/usr/bin/env python3
import subprocess
import json
import time
import sys

def fetch_weixin_url(url, timeout=30):
    print(f"  正在处理: {url[:70]}...")
    
    try:
        # 打开 URL
        print("    打开页面...", end='', flush=True)
        result_open = subprocess.run(
            ['bb-browser', 'open', url, '--tab'],
            capture_output=True, text=True, timeout=20
        )
        if result_open.returncode != 0:
            print(f" ❌ 打开失败")
            return None
        print(" ✅")
        
        # 等待页面加载
        print("    等待加载...", end='', flush=True)
        time.sleep(5)
        print(" ✅")
        
        # 尝试多种方式获取内容
        print("    提取内容...", end='', flush=True)
        
        # 方式1: 尝试获取 innerText
        js_commands = [
            'document.body ? document.body.innerText.substring(0, 10000) : ""',
            'document.querySelector("#js_content") ? document.querySelector("#js_content").innerText : ""',
            'document.querySelector(".rich_media_content") ? document.querySelector(".rich_media_content").innerText : ""',
            'document.querySelector("article") ? document.querySelector("article").innerText : ""',
        ]
        
        content = ""
        for js in js_commands:
            try:
                result_eval = subprocess.run(
                    ['bb-browser', 'eval', js],
                    capture_output=True, text=True, timeout=10
                )
                if result_eval.returncode == 0 and len(result_eval.stdout.strip()) > 50:
                    content = result_eval.stdout.strip()
                    if content:
                        break
            except:
                continue
        
        if not content:
            # 尝试用 snapshot
            try:
                result_snap = subprocess.run(
                    ['bb-browser', 'snapshot', '-i'],
                    capture_output=True, text=True, timeout=15
                )
                if result_snap.returncode == 0:
                    print(f" (snapshot成功)")
            except:
                pass
        
        if len(content) > 100:
            print(f" ✅ 成功 ({len(content)} 字符)")
            return content
        else:
            print(f" ❌ 内容过短")
            return None
            
    except Exception as e:
        print(f" ❌ 异常: {str(e)[:80]}")
        return None

def main():
    print("=" * 80)
    print("尝试爬取微信公众号文章")
    print("=" * 80)
    
    # 测试几个链接
    test_urls = [
        ("记录12", "https://mp.weixin.qq.com/s/ltfeKyQxppOAkpePswGOog"),
        ("记录44", "https://mp.weixin.qq.com/s/RdDiw93k2tcr0YYqnc-V4Q"),
    ]
    
    results = {}
    for name, url in test_urls:
        print(f"\n[{name}]")
        content = fetch_weixin_url(url)
        if content:
            results[name] = content
    
    print(f"\n" + "=" * 80)
    print(f"成功爬取: {len(results)} 条")
    print("=" * 80)
    
    for name, content in results.items():
        print(f"\n[{name}] 内容预览:")
        print(content[:300] + "..." if len(content) > 300 else content)

if __name__ == "__main__":
    main()
