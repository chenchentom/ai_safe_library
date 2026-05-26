#!/usr/bin/env python3
import json
import time
import sys

jsonl_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1.jsonl'

def should_skip_url(url):
    skip_keywords = [
        'mp.weixin.qq.com',
        'weixin.qq.com',
        'arxiv.org/pdf',
        '邮箱',
        'jdzol.com',
    ]
    for keyword in skip_keywords:
        if keyword in url:
            return True
    return False

def main():
    print("=" * 100)
    print("批量爬取剩余记录")
    print("=" * 100)
    
    jsonl_items = []
    with open(jsonl_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line:
                item = json.loads(line)
                jsonl_items.append(item)
    
    remaining_items = []
    for item in jsonl_items:
        content = item.get('content', '').strip()
        source_url = item.get('source_url', '').strip()
        if not content and source_url and not should_skip_url(source_url):
            remaining_items.append(item)
    
    print(f"  可尝试爬取: {len(remaining_items)} 条")
    print("=" * 100)
    
    results = {}
    
    for item in remaining_items:
        record_num = item.get('number')
        event_name = item.get('event_name', '')
        url = item.get('source_url', '')
        
        print(f"\n[{record_num}] {event_name[:60]}")
        print(f"    URL: {url[:70]}")
        
        try:
            # 这里只是列出，实际爬取用 WebFetch 工具
            results[record_num] = {
                'item': item,
                'url': url,
                'event_name': event_name
            }
        except Exception as e:
            print(f"    ❌ 错误: {str(e)[:60]}")
    
    print(f"\n" + "=" * 100)
    print(f"  列表完成: {len(results)} 条待处理")
    print("=" * 100)
    
    # 输出到临时文件供后续处理
    with open('/tmp/remaining_urls.json', 'w', encoding='utf-8') as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    
    print(f"\n  待处理URL列表已保存到 /tmp/remaining_urls.json")

if __name__ == "__main__":
    main()
