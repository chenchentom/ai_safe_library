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
print("更新 JSONL 文件内容")
print("=" * 80)

updates = {
    18: """Vercel April 2026 security incident

We’ve identified a security incident that involved unauthorized access to certain internal Vercel systems.

In this bulletin: 
- Updates
- Who is impacted
- What we know
- Recommendations
- Indicators of compromise (IOCs)
- Product enhancements

The incident originated with a compromise of Context.ai, a third-party AI tool used by a Vercel employee. The attacker used that access to take over the employee's Vercel Google Workspace account, which enabled them to gain access to some Vercel environments and environment variables that were not marked as “sensitive.” 

Environment variables marked as "sensitive" in Vercel are stored in a manner that prevents them from being read, and we currently do not have evidence that those values were accessed.

We assess the attacker as highly sophisticated based on their operational velocity and detailed understanding of Vercel's systems. We are working with Mandiant, additional cybersecurity firms, industry peers, and law enforcement. We have also engaged Context.ai directly to understand the full scope of the underlying compromise.

In collaboration with GitHub, Microsoft, npm, and Socket, our security team has confirmed that no npm packages published by Vercel have been compromised. There is no evidence of tampering, and we believe the supply chain remains safe.

While we continue to take actions to protect Vercel systems and customers, here are best practices you should follow:
- Enable multi-factor authentication
- Review and rotate environment variables
- Take advantage of the sensitive environment variables feature""",
    
    75: """Hackers abuse Google ads, Claude.ai chats to push Mac malware

Attackers are abusing Google Ads and legitimate Claude.ai shared chats in an active malvertising campaign.

Users searching for "Claude mac download" may come across sponsored search results that list claude.ai as the target website, but lead to instructions that install malware on their Mac.

The campaign was spotted by Berk Albayrak, a security engineer at Trendyol Group, who shared his findings on LinkedIn.

Albayrak identified a Claude.ai shared chat that presents itself as an official "Claude Code on Mac" installation guide, attributed to "Apple Support."

The chat walks users through opening Terminal and pasting a command, which silently downloads and runs malware on their Mac.

While attempting to verify Albayrak's findings, BleepingComputer landed on a second shared Claude chat carrying out the same attack through entirely separate infrastructure.

The two chats follow an identical structure and social engineering approach but use different domains and payloads.

The base64 instructions shown in the shared Claude chat download an encoded shell script from domains such as customroofingcontractors.com/curl/...

This compressed shell script runs entirely in memory, leaving little obvious trace on disk.

BleepingComputer observed the server serving a uniquely obfuscated version of the payload on each request, making it harder for security tools to flag.

The variant BleepingComputer identified starts by checking whether the machine has Russian or CIS-region keyboard input sources configured. If it does, the script exits without doing anything.

The script then pulls down a second-stage payload and runs it through osascript, macOS's built-in scripting engine. This gives the attacker remote code execution.

It harvests browser credentials, cookies, and macOS Keychain contents, packages them up, and exfiltrates them to the attacker's server.""",
    
    76: """One keypress is all it takes to compromise four AI coding tools

Developers clone unfamiliar repositories all the time. Open-source projects, work from teammates, sample code from a tutorial, a library someone recommended on a forum. The convention is old and reasonable: you look at what’s inside before you run it. AI coding assistants that work from the command line have inherited that convention, and a new piece of research from Adversa AI shows where the convention breaks.

The research, called TrustFall, covers four agentic coding tools: Claude Code from Anthropic, Gemini CLI from Google, Cursor CLI, and GitHub’s Copilot CLI. Each one reads configuration files that ship inside a project. Each one will start helper programs that those files point to. And each one asks for permission with a single dialog box that, in most cases, defaults to “yes.”

The result is that a malicious repository can compromise a developer’s machine the moment they open it in one of these tools and press Enter on the trust prompt. No tool call. No suspicious behavior from the AI. Just a config file, a default-yes dialog, and a process running with the developer’s permissions.

The mechanism the researchers exploited is a feature called MCP, short for Model Context Protocol. It lets an AI assistant talk to outside helper programs: a database connector, a linter, a custom search tool. Useful by design. The catch is that those helpers are defined inside the project itself, in a file the repository ships. When the assistant starts up in that folder, it starts the helpers too.

A helper program runs the same way any program on your computer runs. It can read your SSH keys, your cloud credentials, your shell history, source code from other projects on the same machine, and it can open a connection to a server the attacker controls. It is doing all of this before the AI has done any reasoning at all.

The attack fits in two small JSON files. One defines a helper named something ordinary like “linter” with a one-line script that fetches a payload from the internet and runs it. The other tells the assistant to auto-approve that helper. The repository can look almost empty.""",
    
    64: """CVE-2026-4747 Detail

Description: Each RPCSEC_GSS data packet is validated by a routine which checks a signature in the packet. This routine copies a portion of the packet into a stack buffer, but fails to ensure that the buffer is sufficiently large, and a malicious client can trigger a stack overflow. Notably, this does not require the client to authenticate itself first. As kgssapi.ko's RPCSEC_GSS implementation is vulnerable, remote code execution in the kernel is possible by an authenticated user that is able to send packets to the kernel's NFS server while kgssapi.ko is loaded into the kernel. In userspace, applications which have librpcgss_sec loaded and run an RPC server are vulnerable to remote code execution from any client able to send it packets. We are not aware of any such applications in the FreeBSD base system.

References:
- https://github.com/califio/publications/blob/main/MADBugs/CVE-2026-4747/exploit.py
- https://github.com/califio/publications/tree/main/MADBugs/CVE-2026-4747
- https://security.freebsd.org/advisories/FreeBSD-SA-26:08.rpcsec_gss.asc

Weakness: Stack-based Buffer Overflow

Affected Software: FreeBSD 13.5, 14.3, 14.4, 15.0""",
    
    66: """Assessing Claude Mythos Preview’s cybersecurity capabilities

Earlier today we announced Claude Mythos Preview, a new general-purpose language model. This model performs strongly across the board, but it is strikingly capable at computer security tasks. In response, we have launched Project Glasswing, an effort to use Mythos Preview to help secure the world’s most critical software, and to prepare the industry for the practices we all will need to adopt to keep ahead of cyberattackers.

During our testing, we found that Mythos Preview is capable of identifying and then exploiting zero-day vulnerabilities in every major operating system and every major web browser when directed by a user to do so. The vulnerabilities it finds are often subtle or difficult to detect. Many of them are ten or twenty years old, with the oldest we have found so far being a now-patched 27-year-old bug in OpenBSD—an operating system known primarily for its security.

The exploits it constructs are not just run-of-the-mill stack-smashing exploits. In one case, Mythos Preview wrote a web browser exploit that chained together four vulnerabilities, writing a complex JIT heap spray that escaped both renderer and OS sandboxes. It autonomously obtained local privilege escalation exploits on Linux and other operating systems by exploiting subtle race conditions and KASLR-bypasses. And it autonomously wrote a remote code execution exploit on FreeBSD’s NFS server that granted full root access to unauthenticated users by splitting a 20-gadget ROP chain over multiple packets.

Non-experts can also leverage Mythos Preview to find and exploit sophisticated vulnerabilities. Engineers at Anthropic with no formal security training have asked Mythos Preview to find remote code execution vulnerabilities overnight, and woken up the following morning to a complete, working exploit. In other cases, we’ve had researchers develop scaffolds that allow Mythos Preview to turn vulnerabilities into exploits without any human intervention."""
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
