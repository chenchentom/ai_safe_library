#!/usr/bin/env python3
"""
清空 biz_risk_clue / biz_risk_review_record 索引数据，并从 JSONL 重新导入 99 条线索。

字段映射与后端 BizRiskClue.java / RiskClueServiceImpl.buildClueDocument 对齐。

用法:
  cd /Volumes/mac/code/java/ai_safe_library
  python3 scripts/reimport_biz_risk_clue.py --dry-run          # 仅校验映射，不写 ES
  python3 scripts/reimport_biz_risk_clue.py                    # 清空并导入
  python3 scripts/reimport_biz_risk_clue.py --recreate-index   # 删索引重建 mapping 后导入

环境变量（可选，覆盖 application-dev.yml 默认值）:
  ES_URL, ES_USERNAME, ES_PASSWORD
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import uuid
from datetime import datetime
from pathlib import Path
from typing import Any

import requests
import urllib3

# ---------------------------------------------------------------------------
# 配置
# ---------------------------------------------------------------------------

REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_JSONL = (
    REPO_ROOT
    / "ai-file"
    / "人工智能安全风险事件列表v2（汇总版）-至0521_1.jsonl"
)
DEV_YML = REPO_ROOT / "ai-safe-library-admin/src/main/resources/application-dev.yml"

INDEX_NAME = "biz_risk_clue"
REVIEW_RECORD_INDEX = "biz_risk_review_record"
EXPECTED_COUNT = 99
DATETIME_FMT = "%Y-%m-%d %H:%M:%S"

# JSONL 源字段（已全部核对，共 17 个）
JSONL_FIELDS = frozenset(
    {
        "number",
        "event_name",
        "class_report_1",
        "class_report_2",
        "products_components_services",
        "operating_entity",
        "risk_description",
        "source_url",
        "source_website",
        "paper_title",
        "research_team",
        "content",
        "submit_user_name",
        "submission_channel",
        "submission_time",
        "is_submit",
        "is_verify",
    }
)

# 与 BizRiskClue @Field 名称一致
ES_FIELDS_FROM_JSONL = {
    "number": "number",
    "event_name": "event_name",
    "class_report_1": "class_report_1",
    "class_report_2": "class_report_2",
    "products_components_services": "products_components_services",
    "operating_entity": "operating_entity",
    "risk_description": "risk_description",
    "source_url": "source_url",
    "source_website": "source_website",
    "paper_title": "paper_title",
    "research_team": "research_team",
    "content": "content",
    "submit_user_name": "submit_user_name",
    "submission_channel": "submission_channel",
    "submission_time": "submission_time",
    "is_submit": "is_submit",
    "is_verify": "is_verify",
}

INDEX_SETTINGS = {
    "settings": {"number_of_shards": 3, "number_of_replicas": 1},
    "mappings": {
        "properties": {
            "id": {"type": "keyword"},
            "number": {"type": "integer"},
            "event_name": {
                "type": "text",
                "analyzer": "standard",
                "search_analyzer": "standard",
                "fields": {"keyword": {"type": "keyword", "ignore_above": 256}},
            },
            "class_report_1": {"type": "keyword"},
            "class_report_2": {"type": "keyword"},
            "class_report_list": {"type": "keyword"},
            "class_human_1": {"type": "keyword"},
            "class_human_2": {"type": "keyword"},
            "class_human_list": {"type": "keyword"},
            "products_components_services": {
                "type": "text",
                "analyzer": "standard",
                "fields": {"keyword": {"type": "keyword", "ignore_above": 512}},
            },
            "operating_entity": {"type": "keyword"},
            "operating_entity_human": {"type": "keyword"},
            "risk_description": {"type": "text", "analyzer": "standard"},
            "risk_description_human": {"type": "text", "analyzer": "standard"},
            "source_url": {"type": "keyword", "ignore_above": 2048},
            "source_website": {"type": "keyword"},
            "paper_title": {"type": "text", "analyzer": "standard"},
            "research_team": {"type": "keyword"},
            "content": {"type": "text", "analyzer": "standard"},
            "submit_user_name": {"type": "keyword"},
            "submission_channel": {"type": "keyword"},
            "submission_time": {"type": "date", "format": "yyyy-MM-dd HH:mm:ss"},
            "submit_org_name": {"type": "keyword"},
            "is_submit": {"type": "integer"},
            "audit_status": {"type": "integer"},
            "is_warehouse": {"type": "integer"},
            "warehouse_time": {"type": "date", "format": "yyyy-MM-dd HH:mm:ss"},
            "audit_reason": {"type": "text", "analyzer": "standard"},
            "audit_user_name": {"type": "keyword"},
            "audit_dept_name": {"type": "keyword"},
            "audit_time": {"type": "date", "format": "yyyy-MM-dd HH:mm:ss"},
            "create_time": {"type": "date", "format": "yyyy-MM-dd HH:mm:ss"},
            "update_time": {"type": "date", "format": "yyyy-MM-dd HH:mm:ss"},
            "deleted": {"type": "integer"},
            "is_verify": {"type": "integer"},
            "is_shared": {"type": "integer"},
            "share_time": {"type": "date", "format": "yyyy-MM-dd HH:mm:ss"},
        }
    },
}


def load_es_config() -> tuple[str, str, str]:
    url = "https://localhost:9200"
    username = "elastic"
    password = ""
    if DEV_YML.is_file():
        in_es = False
        for line in DEV_YML.read_text(encoding="utf-8").splitlines():
            stripped = line.strip()
            if stripped.startswith("elasticsearch:"):
                in_es = True
                continue
            if in_es and stripped and not line.startswith((" ", "\t")):
                break
            if not in_es:
                continue
            if stripped.startswith("uris:"):
                url = stripped.split(":", 1)[1].strip()
            elif stripped.startswith("username:"):
                username = stripped.split(":", 1)[1].strip()
            elif stripped.startswith("password:"):
                password = stripped.split(":", 1)[1].strip()
    import os

    return (
        os.environ.get("ES_URL", url),
        os.environ.get("ES_USERNAME", username),
        os.environ.get("ES_PASSWORD", password),
    )


def es_request(
    session: requests.Session,
    method: str,
    path: str,
    json_data: dict | None = None,
) -> requests.Response:
    return session.request(
        method, path, json=json_data, timeout=120, verify=session.verify
    )


def text_or_none(value: Any) -> str | None:
    if value is None:
        return None
    s = str(value).strip()
    return s if s else None


def yes_no_to_int(value: Any) -> int | None:
    s = text_or_none(value)
    if s == "是":
        return 1
    if s == "否":
        return 0
    return None


def parse_submission_time(item: dict) -> str | None:
    raw = item.get("submission_time") or item.get("Submission_time")
    s = text_or_none(raw)
    if not s:
        return None
    try:
        datetime.strptime(s, DATETIME_FMT)
    except ValueError as exc:
        raise ValueError(f"submission_time 格式应为 {DATETIME_FMT}，实际: {s!r}") from exc
    return s


def build_class_report_list(class_report_1: str | None, class_report_2: str | None) -> list[str]:
    if not class_report_1:
        return []
    if class_report_2:
        return [f"{class_report_1}/{class_report_2}"]
    return [class_report_1]


def transform_record(item: dict, import_time: str) -> tuple[str, dict]:
    """
    将 JSONL 一行转为 ES 文档。
    返回 (doc_id, source)，doc_id 与字段 id 相同。
    """
    unknown = set(item.keys()) - JSONL_FIELDS
    if unknown:
        raise ValueError(f"JSONL 含未知字段 {unknown}，请更新脚本 JSONL_FIELDS")

    doc_id = str(uuid.uuid4())
    class_report_1 = text_or_none(item.get("class_report_1"))
    class_report_2 = text_or_none(item.get("class_report_2"))

    doc: dict[str, Any] = {
        "id": doc_id,
        "number": int(item["number"]) if item.get("number") is not None else None,
        "class_report_list": build_class_report_list(class_report_1, class_report_2),
        "class_human_list": [],
        "audit_status": 10,
        "is_warehouse": 0,
        "deleted": 0,
        "is_shared": 0,
        "create_time": import_time,
        "update_time": import_time,
    }

    for jsonl_key, es_key in ES_FIELDS_FROM_JSONL.items():
        if jsonl_key in ("is_submit", "is_verify"):
            continue
        if jsonl_key == "submission_time":
            t = parse_submission_time(item)
            if t:
                doc[es_key] = t
            continue
        if jsonl_key == "number":
            continue
        val = text_or_none(item.get(jsonl_key))
        if val is not None:
            doc[es_key] = val

    for flag_key in ("is_submit", "is_verify"):
        flag_val = yes_no_to_int(item.get(flag_key))
        if flag_val is not None:
            doc[flag_key] = flag_val

    return doc_id, doc


def load_jsonl(path: Path) -> list[dict]:
    rows: list[dict] = []
    with path.open(encoding="utf-8") as f:
        for lineno, line in enumerate(f, 1):
            line = line.strip()
            if not line:
                continue
            try:
                rows.append(json.loads(line))
            except json.JSONDecodeError as exc:
                raise ValueError(f"JSONL 第 {lineno} 行解析失败: {exc}") from exc
    return rows


def validate_jsonl(rows: list[dict]) -> None:
    if len(rows) != EXPECTED_COUNT:
        raise ValueError(f"期望 {EXPECTED_COUNT} 条，实际 {len(rows)} 条")

    numbers: list[int] = []
    for i, row in enumerate(rows, 1):
        if "number" not in row:
            raise ValueError(f"第 {i} 条缺少 number")
        if "event_name" not in row or not text_or_none(row["event_name"]):
            raise ValueError(f"第 {i} 条缺少 event_name")
        unknown = set(row.keys()) - JSONL_FIELDS
        if unknown:
            raise ValueError(f"第 {i} 条含未知字段: {unknown}")
        transform_record(row, datetime.now().strftime(DATETIME_FMT))
        numbers.append(int(row["number"]))

    if sorted(numbers) != list(range(1, EXPECTED_COUNT + 1)):
        print(f"警告: number 序列非 1..{EXPECTED_COUNT}，实际范围 {min(numbers)}..{max(numbers)}")


def clear_index_data(session: requests.Session, base: str, index_name: str) -> int:
    resp = es_request(
        session,
        "POST",
        f"{base}/{index_name}/_delete_by_query?refresh=true",
        {"query": {"match_all": {}}},
    )
    if resp.status_code == 404:
        print(f"索引 {index_name} 不存在，跳过清空")
        return 0
    resp.raise_for_status()
    deleted = resp.json().get("deleted", 0)
    print(f"已清空 {index_name}: deleted={deleted}")
    return deleted


def verify_index_count(session: requests.Session, base: str, index_name: str, expected: int) -> None:
    resp = es_request(session, "GET", f"{base}/{index_name}/_count")
    if resp.status_code == 404:
        if expected == 0:
            print(f"验证通过: {index_name} 不存在（视为 0 条）")
            return
        raise RuntimeError(f"验证失败: 索引 {index_name} 不存在")
    resp.raise_for_status()
    count = resp.json()["count"]
    if count != expected:
        raise RuntimeError(f"验证失败: {index_name} 期望 {expected} 条，实际 {count} 条")
    print(f"验证通过: {index_name} 共 {count} 条")


def recreate_index(session: requests.Session, base: str) -> None:
    resp = es_request(session, "DELETE", f"{base}/{INDEX_NAME}")
    if resp.status_code not in (200, 404):
        resp.raise_for_status()
    resp = es_request(session, "PUT", f"{base}/{INDEX_NAME}", INDEX_SETTINGS)
    resp.raise_for_status()
    print(f"已重建索引 {INDEX_NAME}")


def bulk_index(session: requests.Session, base: str, docs: list[tuple[str, dict]]) -> None:
    lines: list[str] = []
    for doc_id, source in docs:
        lines.append(json.dumps({"index": {"_index": INDEX_NAME, "_id": doc_id}}, ensure_ascii=False))
        lines.append(json.dumps(source, ensure_ascii=False))
    body = "\n".join(lines) + "\n"
    resp = session.post(
        f"{base}/_bulk?refresh=true",
        data=body.encode("utf-8"),
        headers={"Content-Type": "application/x-ndjson"},
        timeout=300,
        verify=session.verify,
    )
    resp.raise_for_status()
    result = resp.json()
    if result.get("errors"):
        for item in result.get("items", []):
            err = item.get("index", {}).get("error")
            if err:
                raise RuntimeError(f"bulk 写入失败: {err}")
    print(f"bulk 写入完成: {len(docs)} 条")


def verify_index(session: requests.Session, base: str, sample: dict) -> None:
    resp = es_request(session, "GET", f"{base}/{INDEX_NAME}/_count")
    resp.raise_for_status()
    count = resp.json()["count"]
    if count != EXPECTED_COUNT:
        raise RuntimeError(f"验证失败: 期望 {EXPECTED_COUNT} 条，实际 {count} 条")

    resp = es_request(
        session,
        "GET",
        f"{base}/{INDEX_NAME}/_search",
        {
            "size": 1,
            "query": {"term": {"number": sample.get("number")}},
        },
    )
    resp.raise_for_status()
    hits = resp.json()["hits"]["hits"]
    if not hits:
        raise RuntimeError(f"验证失败: 未找到 number={sample.get('number')} 的文档")

    src = hits[0]["_source"]
    checks = [
        ("event_name", sample.get("event_name")),
        ("class_report_1", sample.get("class_report_1")),
        ("audit_status", 10),
        ("is_warehouse", 0),
        ("deleted", 0),
        ("is_shared", 0),
    ]
    for field, expected in checks:
        if src.get(field) != expected:
            raise RuntimeError(
                f"字段校验失败 number={sample.get('number')}: {field}="
                f"{src.get(field)!r}, 期望 {expected!r}"
            )

    expected_list = build_class_report_list(
        text_or_none(sample.get("class_report_1")),
        text_or_none(sample.get("class_report_2")),
    )
    if src.get("class_report_list") != expected_list:
        raise RuntimeError(
            f"class_report_list 不匹配: {src.get('class_report_list')!r} != {expected_list!r}"
        )

    print(f"验证通过: 共 {count} 条，抽样 number={sample.get('number')} 字段正确")


def print_mapping_help() -> None:
    print("\n字段映射（JSONL → ES → Java BizRiskClue）")
    print("-" * 72)
    for jk, ek in sorted(ES_FIELDS_FROM_JSONL.items()):
        java = "".join(p.capitalize() for p in ek.split("_"))
        if ek == "event_name":
            java = "eventName"
        print(f"  {jk:32} → {ek:28} → {java}")
    print(f"  (派生) class_report_1+2          → class_report_list          → classReportList")
    print(f"  (默认) —                         → audit_status=10            → auditStatus (未审核)")
    print(f"  (默认) —                         → is_warehouse=0             → isWarehouse")
    print(f"  (默认) —                         → deleted=0                  → deleted")
    print(f"  (默认) —                         → is_shared=0                → isShared")
    print(f"  (默认) —                         → create_time/update_time    → 导入时刻")
    print(f"  is_submit/is_verify 是/否        → 1/0，空串则省略该字段")
    print("-" * 72)


def main() -> int:
    parser = argparse.ArgumentParser(description="清空并重新导入 biz_risk_clue 线索数据")
    parser.add_argument("--jsonl", type=Path, default=DEFAULT_JSONL, help="JSONL 数据文件")
    parser.add_argument("--dry-run", action="store_true", help="仅校验，不写 ES")
    parser.add_argument(
        "--recreate-index",
        action="store_true",
        help="删除并重建索引 mapping（默认仅 delete_by_query 清空文档）",
    )
    args = parser.parse_args()

    if not args.jsonl.is_file():
        print(f"错误: 找不到 JSONL 文件 {args.jsonl}", file=sys.stderr)
        return 1

    print_mapping_help()

    rows = load_jsonl(args.jsonl)
    print(f"\n读取 JSONL: {args.jsonl} ({len(rows)} 条)")
    validate_jsonl(rows)
    print("JSONL 结构与字段映射校验通过")

    import_time = datetime.now().strftime(DATETIME_FMT)
    docs = [transform_record(row, import_time) for row in rows]

    if args.dry_run:
        print("\n[dry-run] 未连接 ES。示例文档 (number=1):")
        sample = next(d for _, d in docs if d.get("number") == 1)
        print(json.dumps(sample, ensure_ascii=False, indent=2)[:2000])
        return 0

    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    es_url, es_user, es_pass = load_es_config()
    session = requests.Session()
    session.auth = (es_user, es_pass)
    session.verify = False  # 本地 ES 常为自签名证书
    session.trust_env = False
    base = es_url.rstrip("/")

    print(f"\n连接 ES: {base}")
    resp = es_request(session, "GET", f"{base}/")
    resp.raise_for_status()
    print(f"ES 版本: {resp.json()['version']['number']}")

    print("\n清空索引数据...")
    clear_index_data(session, base, REVIEW_RECORD_INDEX)
    if args.recreate_index:
        recreate_index(session, base)
    else:
        clear_index_data(session, base, INDEX_NAME)
        verify_index_count(session, base, INDEX_NAME, 0)
    verify_index_count(session, base, REVIEW_RECORD_INDEX, 0)

    bulk_index(session, base, docs)
    verify_index(session, base, rows[0])
    verify_index_count(session, base, REVIEW_RECORD_INDEX, 0)
    print("\n全部完成。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
