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
print("更新 JSONL 文件内容 (第3批)")
print("=" * 80)

updates = {
    43: """Title: Multi-Faceted Attack: Exposing Cross-Model Vulnerabilities in Defense-Equipped Vision-Language Models

Authors: Yijun Yang, Lichao Wang, Jianping Zhang, Chi Harold Liu, Lanqing Hong, Qiang Xu

Abstract: The growing misuse of Vision-Language Models (VLMs) has led providers to deploy multiple safeguards, including alignment tuning, system prompts, and content moderation. However, the real-world robustness of these defenses against adversarial attacks remains underexplored. We introduce Multi-Faceted Attack (MFA), a framework that systematically exposes general safety vulnerabilities in leading defense-equipped VLMs such as GPT-4o, Gemini-Pro, and Llama-4. The core component of MFA is the Attention-Transfer Attack (ATA), which hides harmful instructions inside a meta task with competing objectives. We provide a theoretical perspective based on reward hacking to explain why this attack succeeds. To improve cross-model transferability, we further introduce a lightweight transfer-enhancement algorithm combined with a simple repetition strategy that jointly bypasses both input-level and output-level filters without model-specific fine-tuning. Empirically, we show that adversarial images optimized for one vision encoder transfer broadly to unseen VLMs, indicating that shared visual representations create a cross-model safety vulnerability. Overall, MFA achieves a 58.5% success rate and consistently outperforms existing methods. On state-of-the-art commercial models, MFA reaches a 52.8% success rate, surpassing the second-best attack by 34%. These results challenge the perceived robustness of current defense mechanisms and highlight persistent safety weaknesses in modern VLMs.

Comments: AAAI 2026 Oral
Subjects: Cryptography and Security (cs.CR)
arXiv:2511.16110"""
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
