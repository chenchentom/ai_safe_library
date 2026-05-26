
from elasticsearch import Elasticsearch
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# ES连接配置
es = Elasticsearch(
    "https://localhost:9200",
    basic_auth=("elastic", "V4h_Am00B-eNpeE5OSf*"),
    verify_certs=False
)

print("=== ES索引检查 ===")
print("索引是否存在:", es.indices.exists(index="biz_risk_clue"))
if es.indices.exists(index="biz_risk_clue"):
    print("文档总数:", es.count(index="biz_risk_clue")["count"])
    print("\n=== 查看前3条文档的字段 ===")
    resp = es.search(index="biz_risk_clue", size=3, query={"match_all": {}})
    for hit in resp["hits"]["hits"]:
        print("\nDoc ID:", hit["_id"])
        print("Fields:")
        for k, v in hit["_source"].items():
            val = v
            if isinstance(v, str) and len(v) > 50:
                val = v[:50] + "..."
            print("  %s: %s" % (k, val))
