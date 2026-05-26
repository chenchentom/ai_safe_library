#!/usr/bin/env python3
import json
import time
import random
import requests
from bs4 import BeautifulSoup

jsonl_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1.jsonl'
backup_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1_before_fetch.jsonl'

def fetch_url(url):
    headers = {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36'
    }
    
    try:
        if 'mp.weixin.qq.com' in url:
            print(f"    ⏭️  跳过微信公众号链接 (需要登录)")
            return None
        
        if 'arxiv.org' in url and url.endswith('.pdf'):
            print(f"    ⏭️  跳过PDF链接")
            return None
        
        if url == '邮箱' or 'jdzol.com' in url:
            print(f"    ⏭️  跳过无效链接")
            return None
        
        if 'github.com' in url:
            print(f"    ⏭️  跳过GitHub链接")
            return None
        
        print(f"    正在请求...", end='', flush=True)
        response = requests.get(url, headers=headers, timeout=20)
        response.encoding = 'utf-8'
        
        if response.status_code == 200:
            print(f" OK", end='', flush=True)
            soup = BeautifulSoup(response.text, 'html.parser')
            
            for script in soup(['script', 'style', 'nav', 'footer', 'header']):
                script.decompose()
            
            text_parts = []
            
            title = soup.find('title')
            if title:
                text_parts.append(title.get_text().strip())
            
            paragraphs = soup.find_all('p')
            for p in paragraphs:
                text = p.get_text().strip()
                if text and len(text) > 20:
                    text_parts.append(text)
            
            content = '\n'.join(text_parts[:40])
            
            if len(content) > 100:
                return content
            else:
                print(f" (内容过短)", end='')
                return None
        else:
            print(f" HTTP {response.status_code}", end='')
            return None
    except Exception as e:
        print(f" 错误: {str(e)[:50]}", end='')
        return None

def main():
    print("=" * 80)
    print("网页内容爬取")
    print("=" * 80)
    
    jsonl_items = []
    with open(jsonl_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line:
                item = json.loads(line)
                jsonl_items.append(item)
    
    print(f"  读取记录数: {len(jsonl_items)}")
    
    with open(backup_path, 'w', encoding='utf-8') as f:
        for item in jsonl_items:
            f.write(json.dumps(item, ensure_ascii=False) + '\n')
    print(f"  备份文件: {backup_path}")
    
    fetched_count = 0
    for idx, item in enumerate(jsonl_items, 1):
        content = item.get('content', '').strip()
        source_url = item.get('source_url', '').strip()
        
        if not content and source_url:
            print(f"\n  [记录 {idx}] {item.get('event_name', '')}")
            print(f"    URL: {source_url[:70]}...")
            
            new_content = fetch_url(source_url)
            
            if new_content:
                item['content'] = new_content
                fetched_count += 1
                print(f" ✅ 成功 ({len(new_content)} 字符)")
            else:
                print(f" ❌ 跳过")
            
            time.sleep(random.uniform(0.5, 1.5))
    
    print(f"\n" + "=" * 80)
    print(f"  爬取完成: {fetched_count} 条")
    print("=" * 80)
    
    with open(jsonl_path, 'w', encoding='utf-8') as f:
        for item in jsonl_items:
            f.write(json.dumps(item, ensure_ascii=False) + '\n')
    
    print(f"\n✅ 文件已更新: {jsonl_path}")

if __name__ == "__main__":
    main()
