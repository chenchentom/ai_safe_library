#!/usr/bin/env python3
import redis

# Redis 配置
REDIS_CONFIG = {
    'host': 'localhost',
    'port': 6379,
    'db': 0,
    'decode_responses': True
}

def clear_cache():
    print("=" * 80)
    print("清空 Redis 中的标签缓存")
    print("=" * 80)
    
    try:
        r = redis.Redis(**REDIS_CONFIG)
        
        # 查找所有标签相关的缓存键
        keys = r.keys("tag:tree:*")
        
        print(f"\n找到 {len(keys)} 个缓存键:")
        for key in keys:
            print(f"  - {key}")
        
        if keys:
            # 删除这些键
            r.delete(*keys)
            print(f"\n✓ 已删除 {len(keys)} 个缓存键")
        else:
            print("\n没有找到标签缓存")
        
        print("\n" + "=" * 80)
        
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    clear_cache()
