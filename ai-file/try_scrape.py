#!/usr/bin/env python3
import json
import time
import random
from scrapling import Scrapling

jsonl_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1.jsonl'

def scrape_with_scrapling(url):
    try:
        scraper = Scrapling()
        response = scraper.get(url)
        
        if response.success:
            return response.clean_text
        else:
            return None
    except Exception as e:
        print(f"    错误: {str(e)[:100]}")
        return None

def main():
    print("=" * 80)
    print("尝试使用 Scrapling 爬取")
    print("=" * 80)
    
    jsonl_items = []
    with open(jsonl_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line:
                item = json.loads(line)
                jsonl_items.append(item)
    
    print(f"  读取记录数: {len(jsonl_items)}")
    
    test_urls = [
        (18, "Vercel平台遭第三方AI工具供应链攻击事件", "https://vercel.com/kb/bulletin/vercel-april-2026-security-incident"),
        (75, "Google Ads 仿冒广告借 Claude.ai 共享对话向 macOS 用户投递恶意软件", "https://www.bleepingcomputer.com/news/security/hackers-abuse-google-ads-claudeai-chats-to-push-mac-malware/"),
        (76, "Claude Code 等 AI 编程助手面临 MCP 配置投毒攻击风险", "https://www.helpnetsecurity.com/2026/05/07/trustfall-ai-coding-cli-vulnerability-research/"),
        (64, "Mythos自主构建全链路攻击链实战能力披露事件", "https://nvd.nist.gov/vuln/detail/CVE-2026-4747"),
    ]
    
    fetched_count = 0
    for record_num, event_name, url in test_urls:
        print(f"\n  [记录 {record_num}] {event_name}")
        print(f"    URL: {url}")
        print(f"    正在爬取...", end='', flush=True)
        
        content = scrape_with_scrapling(url)
        
        if content and len(content) > 100:
            for item in jsonl_items:
                if item.get('number') == record_num:
                    item['content'] = content
                    fetched_count += 1
                    print(f" ✅ 成功 ({len(content)} 字符)")
                    break
        
        time.sleep(random.uniform(1, 2))
    
    print(f"\n" + "=" * 80)
    print(f"  爬取完成: {fetched_count} 条")
    print("=" * 80)
    
    with open(jsonl_path, 'w', encoding='utf-8') as f:
        for item in jsonl_items:
            f.write(json.dumps(item, ensure_ascii=False) + '\n')
    
    print(f"\n✅ 文件已更新")

if __name__ == "__main__":
    main()
