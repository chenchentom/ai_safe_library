#!/usr/bin/env bash
# 从本机开发环境导出 MySQL + ES，用于正式环境迁移
# 用法: ./export-migrate.sh
# 可按需修改下方连接信息

set -euo pipefail

OUT_DIR="${1:-/Users/tom/Downloads/ai}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASS="${MYSQL_PASS:-123456}"
MYSQL_DB="${MYSQL_DB:-ai_safe_library}"

ES_URL="${ES_URL:-https://localhost:9200}"
ES_USER="${ES_USER:-elastic}"
ES_PASS="${ES_PASS:-V4h_Am00B-eNpeE5OSf*}"

mkdir -p "$OUT_DIR/mysql" "$OUT_DIR/elasticsearch"

echo "[1/3] MySQL 全量导出..."
mysqldump -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASS" \
  --single-transaction --routines --triggers --events \
  --set-gtid-purged=OFF \
  --default-character-set=utf8mb4 \
  "$MYSQL_DB" 2>/dev/null > "$OUT_DIR/mysql/ai_safe_library_full.sql"

mysqldump -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASS" \
  --no-data --set-gtid-purged=OFF \
  --default-character-set=utf8mb4 \
  "$MYSQL_DB" 2>/dev/null > "$OUT_DIR/mysql/ai_safe_library_schema_only.sql"

mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASS" -N -e \
  "SELECT table_name, table_rows FROM information_schema.tables WHERE table_schema='$MYSQL_DB' ORDER BY table_name;" \
  > "$OUT_DIR/mysql/table_summary.txt"

echo "[2/3] ES 映射与设置..."
for idx in biz_risk_clue biz_risk_review_record; do
  curl -sk -u "$ES_USER:$ES_PASS" "$ES_URL/$idx" -o "$OUT_DIR/elasticsearch/${idx}_index_settings.json"
  curl -sk -u "$ES_USER:$ES_PASS" "$ES_URL/$idx/_mapping" -o "$OUT_DIR/elasticsearch/${idx}_mapping.json"
done

echo "[3/3] ES 数据 scroll 导出..."
export OUT_DIR ES_URL ES_USER ES_PASS
python3 << 'PYEOF'
import json, os, ssl, base64, urllib.request

OUT = os.environ["OUT_DIR"]
ES_URL = os.environ["ES_URL"]
AUTH = base64.b64encode(f"{os.environ['ES_USER']}:{os.environ['ES_PASS']}".encode()).decode()
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def req(method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(ES_URL + path, data=data, method=method)
    r.add_header("Authorization", f"Basic {AUTH}")
    r.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(r, context=ctx, timeout=120) as resp:
        return json.loads(resp.read().decode())

def export_index(index):
    ndjson = os.path.join(OUT, "elasticsearch", f"{index}_data.ndjson")
    bulk = os.path.join(OUT, "elasticsearch", f"{index}_bulk.ndjson")
    count, scroll_id = 0, None
    try:
        res = req("POST", f"/{index}/_search?scroll=2m", {"size": 500, "query": {"match_all": {}}})
        scroll_id, hits = res.get("_scroll_id"), res["hits"]["hits"]
        with open(ndjson, "w", encoding="utf-8") as nf, open(bulk, "w", encoding="utf-8") as bf:
            while hits:
                for h in hits:
                    nf.write(json.dumps({"_id": h["_id"], "_source": h["_source"]}, ensure_ascii=False) + "\n")
                    bf.write(json.dumps({"index": {"_index": index, "_id": h["_id"]}}) + "\n")
                    bf.write(json.dumps(h["_source"], ensure_ascii=False) + "\n")
                    count += 1
                res = req("POST", "/_search/scroll", {"scroll": "2m", "scroll_id": scroll_id})
                scroll_id, hits = res.get("_scroll_id"), res["hits"]["hits"]
    finally:
        if scroll_id:
            try:
                req("DELETE", "/_search/scroll", {"scroll_id": scroll_id})
            except Exception:
                pass
    print(f"  {index}: {count} docs")

for idx in ["biz_risk_clue", "biz_risk_review_record"]:
    export_index(idx)
PYEOF

chmod +x "$OUT_DIR/export-migrate.sh" 2>/dev/null || true
echo "完成。输出目录: $OUT_DIR"
ls -lh "$OUT_DIR/mysql" "$OUT_DIR/elasticsearch"
