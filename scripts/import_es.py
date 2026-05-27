#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从导出目录恢复 Elasticsearch 索引与数据（本地无密码 ES 默认可用）。

导出目录需包含（每个索引一组）：
  - {index}_mapping.json
  - {index}_bulk.ndjson

用法示例：
  python3 import_es.py
  python3 import_es.py --data-dir /Users/tom/Downloads/ai/elasticsearch
  python3 import_es.py --es-url http://127.0.0.1:9200 --recreate
"""

from __future__ import annotations

import argparse
import json
import ssl
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="导入 ES mapping + bulk 数据")
    parser.add_argument(
        "--es-url",
        default="http://localhost:9200",
        help="Elasticsearch 地址，默认 http://localhost:9200",
    )
    parser.add_argument(
        "--data-dir",
        default="",
        help="导出文件目录，默认使用脚本同目录下的 elasticsearch 子目录",
    )
    parser.add_argument(
        "--user",
        default="",
        help="ES 用户名（可选）",
    )
    parser.add_argument(
        "--password",
        default="",
        help="ES 密码（可选）",
    )
    parser.add_argument(
        "--recreate",
        action="store_true",
        help="若索引已存在则先删除再重建",
    )
    parser.add_argument(
        "--insecure",
        action="store_true",
        help="HTTPS 时跳过证书校验",
    )
    parser.add_argument(
        "--chunk-size",
        type=int,
        default=500,
        help="bulk 每批文档数（每文档占 2 行 ndjson）",
    )
    return parser.parse_args()


class EsClient:
    def __init__(
        self,
        base_url: str,
        user: str = "",
        password: str = "",
        insecure: bool = False,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self._auth_header = self._build_auth(user, password)
        self._ssl_ctx = None
        if self.base_url.startswith("https://"):
            self._ssl_ctx = ssl.create_default_context()
            if insecure:
                self._ssl_ctx.check_hostname = False
                self._ssl_ctx.verify_mode = ssl.CERT_NONE

    @staticmethod
    def _build_auth(user: str, password: str) -> str | None:
        if not user:
            return None
        import base64

        token = base64.b64encode(f"{user}:{password}".encode()).decode()
        return f"Basic {token}"

    def request(
        self,
        method: str,
        path: str,
        body: bytes | None = None,
        content_type: str = "application/json",
    ) -> Any:
        url = f"{self.base_url}{path}"
        req = urllib.request.Request(url, data=body, method=method)
        if body is not None:
            req.add_header("Content-Type", content_type)
        if self._auth_header:
            req.add_header("Authorization", self._auth_header)
        try:
            with urllib.request.urlopen(req, context=self._ssl_ctx, timeout=120) as resp:
                raw = resp.read().decode("utf-8")
                return json.loads(raw) if raw else {}
        except urllib.error.HTTPError as e:
            detail = e.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"HTTP {e.code} {method} {path}\n{detail}") from e

    def ping(self) -> dict[str, Any]:
        return self.request("GET", "/")

    def index_exists(self, index: str) -> bool:
        try:
            self.request("HEAD", f"/{index}")
            return True
        except RuntimeError as e:
            if "HTTP 404" in str(e):
                return False
            raise

    def delete_index(self, index: str) -> None:
        if self.index_exists(index):
            self.request("DELETE", f"/{index}")
            print(f"  已删除索引: {index}")

    def create_index(self, index: str, mappings: dict[str, Any]) -> None:
        payload = json.dumps({"mappings": mappings}, ensure_ascii=False).encode("utf-8")
        self.request("PUT", f"/{index}", body=payload)
        print(f"  已创建索引: {index}")

    def bulk(self, ndjson: str) -> dict[str, Any]:
        data = ndjson.encode("utf-8")
        return self.request(
            "POST",
            "/_bulk",
            body=data,
            content_type="application/x-ndjson",
        )

    def refresh(self, index: str) -> None:
        self.request("POST", f"/{index}/_refresh")

    def count(self, index: str) -> int:
        res = self.request("GET", f"/{index}/_count")
        return int(res.get("count", 0))


def load_mappings(mapping_file: Path) -> tuple[str, dict[str, Any]]:
    with mapping_file.open(encoding="utf-8") as f:
        raw = json.load(f)
    if len(raw) != 1:
        raise ValueError(f"{mapping_file} 格式异常，期望单个索引键")
    index_name = next(iter(raw.keys()))
    mappings = raw[index_name].get("mappings")
    if not mappings:
        raise ValueError(f"{mapping_file} 缺少 mappings")
    return index_name, mappings


def iter_bulk_batches(bulk_file: Path, chunk_size: int):
    """每 chunk_size 条文档 yield 一段 ndjson（文档数 = 行数/2）。"""
    lines: list[str] = []
    doc_count = 0
    with bulk_file.open(encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            lines.append(line)
            if len(lines) % 2 == 0:
                doc_count += 1
                if doc_count >= chunk_size:
                    yield "\n".join(lines) + "\n", doc_count
                    lines = []
                    doc_count = 0
        if lines:
            remaining = len(lines) // 2
            if remaining:
                yield "\n".join(lines) + "\n", remaining


def import_index(
    client: EsClient,
    index: str,
    mappings: dict[str, Any],
    bulk_file: Path,
    recreate: bool,
    chunk_size: int,
) -> None:
    print(f"\n>>> 处理索引: {index}")

    if recreate:
        client.delete_index(index)

    if client.index_exists(index):
        print("  索引已存在，跳过 mapping 创建（使用 --recreate 可重建）")
    else:
        client.create_index(index, mappings)

    if not bulk_file.exists():
        raise FileNotFoundError(f"缺少 bulk 文件: {bulk_file}")

    total_docs = 0
    batch_no = 0
    for ndjson, batch_docs in iter_bulk_batches(bulk_file, chunk_size):
        batch_no += 1
        res = client.bulk(ndjson)
        if res.get("errors"):
            first_err = next(
                (item for item in res.get("items", []) if "error" in item.get("index", {})),
                None,
            )
            raise RuntimeError(f"bulk 导入失败: {first_err}")
        total_docs += batch_docs
        print(f"  bulk 批次 {batch_no}: +{batch_docs} 条")

    client.refresh(index)
    actual = client.count(index)
    print(f"  导入完成: bulk 共 {total_docs} 条，当前索引文档数 {actual}")


def discover_indices(data_dir: Path) -> list[tuple[str, Path, Path]]:
    pairs: list[tuple[str, Path, Path]] = []
    for mapping_file in sorted(data_dir.glob("*_mapping.json")):
        index = mapping_file.name[: -len("_mapping.json")]
        bulk_file = data_dir / f"{index}_bulk.ndjson"
        pairs.append((index, mapping_file, bulk_file))
    return pairs


def main() -> int:
    args = parse_args()
    script_dir = Path(__file__).resolve().parent
    data_dir = Path(args.data_dir) if args.data_dir else script_dir / "elasticsearch"
    if not data_dir.is_dir():
        print(f"错误: 数据目录不存在: {data_dir}", file=sys.stderr)
        print("请使用 --data-dir 指定包含 *_mapping.json 与 *_bulk.ndjson 的目录", file=sys.stderr)
        return 1

    indices = discover_indices(data_dir)
    if not indices:
        print(f"错误: 在 {data_dir} 未找到 *_mapping.json", file=sys.stderr)
        return 1

    client = EsClient(
        base_url=args.es_url,
        user=args.user,
        password=args.password,
        insecure=args.insecure,
    )

    print(f"ES: {args.es_url}")
    print(f"数据目录: {data_dir}")
    info = client.ping()
    print(f"集群: {info.get('cluster_name', '?')}  version: {info.get('version', {}).get('number', '?')}")

    for index, mapping_file, bulk_file in indices:
        index_name, mappings = load_mappings(mapping_file)
        if index_name != index:
            print(f"警告: 文件名索引 {index} 与 mapping 内 {index_name} 不一致，以 mapping 为准")
            index = index_name
        import_index(
            client=client,
            index=index,
            mappings=mappings,
            bulk_file=bulk_file,
            recreate=args.recreate,
            chunk_size=max(1, args.chunk_size),
        )

    print("\n全部完成。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
