#!/usr/bin/env python3
import json

jsonl_path = '/Volumes/mac/code/java/ai_safe_library/ai-file/人工智能安全风险事件列表v2（汇总版）-至0521_1.jsonl'

jsonl_items = []
with open(jsonl_path, 'r', encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if line:
            item = json.loads(line)
            jsonl_items.append(item)

print("=" * 80)
print("更新 JSONL 文件内容 (第2批)")
print("=" * 80)

updates = {
    49: """Heartbeat context inheritance bypasses sandbox via senderIsOwner escalation

Package: npm openclaw
Affected versions: <=2026.3.28
Patched versions: >= 2026.3.31

Summary: Heartbeat context inheritance bypasses sandbox via senderIsOwner escalation

Severity: Critical (9.5)

CVSS v4 base metrics:
- Attack Vector: Network
- Attack Complexity: Low
- Attack Requirements: Present
- Privileges Required: None
- User interaction: None

Vulnerable System Impact Metrics:
- Confidentiality: High
- Integrity: High
- Availability: High

Subsequent System Impact Metrics:
- Confidentiality: High
- Integrity: High
- Availability: High

First stable tag containing the fix: v2026.3.31

Credit: AntAISecurityLab""",
    
    62: """SQL injection in Proxy API key verification

Package: pip litellm
Affected versions: >=1.81.16, <1.83.7
Patched versions: >=1.83.7

Impact: A database query used during proxy API key checks mixed the caller-supplied key value into the query text instead of passing it as a separate parameter. An unauthenticated attacker could send a specially crafted Authorization header to any LLM API route (for example POST /chat/completions) and reach this query through the proxy's error-handling path.

An attacker could read data from the proxy's database and may be able to modify it, leading to unauthorised access to the proxy and the credentials it manages.

Patches: Fixed in 1.83.7. The caller-supplied value is now always passed to the database as a separate parameter. Upgrade to 1.83.7 or later.

Workarounds: If upgrading is not immediately possible, set disable_error_logs: true under general_settings. This removes the path through which unauthenticated input reaches the vulnerable query.

Severity: Critical (9.3)

CVSS v4 base metrics:
- Attack Vector: Network
- Attack Complexity: Low
- Attack Requirements: None
- Privileges Required: None
- User interaction: None

Vulnerable System Impact Metrics:
- Confidentiality: High
- Integrity: High
- Availability: High

Subsequent System Impact Metrics:
- Confidentiality: None
- Integrity: None
- Availability: None

CVE ID: CVE-2026-42208
Weakness: CWE-89 - Improper Neutralization of Special Elements used in an SQL Command ('SQL Injection')

Discovery Credit: Tencent YunDing Security Lab"""
}

updated_count = 0
for item in jsonl_items:
    record_num = item.get('number')
    if record_num in updates:
        if not item.get('content', '').strip():
            item['content'] = updates[record_num]
            updated_count += 1
            print(f"  [记录 {record_num}] {item.get('event_name', '')} - ✅ 更新成功")

print(f"\n" + "=" * 80)
print(f"  总计更新: {updated_count} 条")
print("=" * 80)

with open(jsonl_path, 'w', encoding='utf-8') as f:
    for item in jsonl_items:
        f.write(json.dumps(item, ensure_ascii=False) + '\n')

print(f"\n✅ 文件已更新: {jsonl_path}")
