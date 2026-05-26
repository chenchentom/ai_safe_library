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

print("=" * 100)
print("更新 content 为 GitHub 项目页面内容")
print("=" * 100)

updates = {
    40: """# actions/runner

The runner is the application that runs a job from a GitHub Actions workflow. It is used by GitHub Actions in the hosted virtual environments, or you can self-host the runner in your own environment.

## Key Features

- Runs GitHub Actions workflows
- Supports self-hosted runners
- Used by GitHub in hosted virtual environments
- Written primarily in C# (96.3%)
- 6k+ stars on GitHub

GitHub Actions Runner is the core application that executes CI/CD jobs defined in GitHub Actions workflows. It can be used in GitHub's hosted environments or self-hosted on your own infrastructure, giving you flexibility in where and how your automation runs.""",
    
    96: """# langgenius/dify

Dify is an open-source LLM app development platform. Its intuitive interface combines AI workflow, RAG pipeline, agent capabilities, model management, observability features and more, letting you quickly go from prototype to production.

## Core Features

1. **Workflow**: Build and test powerful AI workflows on a visual canvas
2. **Comprehensive model support**: Integration with hundreds of proprietary/open-source LLMs
3. **Prompt IDE**: Intuitive interface for crafting prompts
4. **RAG Pipeline**: Extensive RAG capabilities covering document ingestion to retrieval
5. **Agent capabilities**: Define agents based on LLM Function Calling or ReAct
6. **LLMOps**: Monitor and analyze application logs and performance over time
7. **Backend-as-a-Service**: All offerings come with corresponding APIs

Dify has 139k+ stars on GitHub and is one of the most popular open-source LLM application development platforms.""",
    
    99: """# binary-husky/gpt_academic

GPT Academic is an open-source project for academic optimization with GPT. It provides a rich set of features for paper reading, translation, code explanation, and more.

## Key Features

- ⭐ Access new models: Support for Qwen, GLM, DeepseekCoder and more
- ⭐ Mermaid image rendering: Support for flowcharts, state diagrams, Gantt charts, etc.
- ⭐ Arxiv paper fine translation: High-quality translation for arxiv papers
- ⭐ Real-time voice dialogue input: Asynchronous audio listening and automatic sentence breaking
- Polish, translate, code explanation: One-click polish, translate, find paper syntax errors, explain code
- Custom shortcuts: Support for custom keyboard shortcuts
- Modular design: Support for custom powerful plugins with hot updates
- Program analysis: One-click analysis of Python/C/C++/Java/Lua projects
- Read and translate papers: One-click interpretation of full latex/pdf papers and summary generation

This project has 70.7k+ stars on GitHub and is widely used for academic research and writing assistance."""
}

updated_count = 0
for item in jsonl_items:
    record_num = item.get('number')
    if record_num in updates:
        current_content = item.get('content', '').strip()
        risk_desc = item.get('risk_description', '').strip()
        
        if current_content == risk_desc and len(current_content) > 0:
            item['content'] = updates[record_num]
            updated_count += 1
            print(f"  [记录 {record_num}] {item.get('event_name', '')[:50]} - ✅ 更新成功")

print(f"\n" + "=" * 100)
print(f"  总计更新: {updated_count} 条")
print("=" * 100)

with open(jsonl_path, 'w', encoding='utf-8') as f:
    for item in jsonl_items:
        f.write(json.dumps(item, ensure_ascii=False) + '\n')

print(f"\n✅ 文件已更新: {jsonl_path}")
