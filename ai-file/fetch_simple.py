#!/usr/bin/env python3
import json
import time
import random
import requests
from bs4 import BeautifulSoup

jsonl_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1.jsonl'
backup_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1_before_fetch.jsonl'

def should_skip(url):
    skip_keywords = [
        'mp.weixin.qq.com',
        'arxiv.org',
        'github.com',
        '邮箱',
        'jdzol.com',
        '.pdf',
        'nvd.nist.gov',
        'red.anthropic.com',
        'www-cdn.anthropic.com',
    ]
    for keyword in skip_keywords:
        if keyword in url:
            return True
    return False

def fetch_url(url):
    headers = {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36'
    }
    
    try:
        if should_skip(url):
            return None
        
        response = requests.get(url, headers=headers, timeout=15)
        response.encoding = 'utf-8'
        
        if response.status_code == 200:
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
                return None
        else:
            return None
    except Exception as e:
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
            print(f"  [记录 {idx}] {item.get('event_name', '')[:40]}...", end=' ', flush=True)
            
            if should_skip(source_url):
                print("⏭️  跳过")
            else:
                new_content = fetch_url(source_url)
                if new_content:
                    item['content'] = new_content
                    fetched_count += 1
                    print(f"✅ 成功 ({len(new_content)} 字符)")
                else:
                    print("❌ 失败")
            
            time.sleep(random.uniform(0.3, 0.8))
    
    print(f"\n" + "=" * 80)
    print(f"  爬取完成: {fetched_count} 条")
    print("=" * 80)
    
    with open(jsonl_path, 'w', encoding='utf-8') as f:
        for item in jsonl_items:
            f.write(json.dumps(item, ensure_ascii=False) + '\n')
    
    print(f"\n✅ 文件已更新: {jsonl_path}")

if __name__ == "__main__":
    main()
