#!/bin/bash

# AI安全事件库 - 一键停止脚本
# 使用方法: ./stop.sh

echo "========================================="
echo "  AI安全事件库 - 停止服务"
echo "========================================="
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "[1/3] 停止后端服务..."
if [ -f "$SCRIPT_DIR/logs/backend.pid" ]; then
    BACKEND_PID=$(cat "$SCRIPT_DIR/logs/backend.pid" 2>/dev/null)
    if [ -n "$BACKEND_PID" ]; then
        kill -9 $BACKEND_PID 2>/dev/null
        echo "✅ 后端已停止 (PID: $BACKEND_PID)"
    fi
    rm -f "$SCRIPT_DIR/logs/backend.pid"
fi
# 兜底：通过端口查找
lsof -ti :8080 2>/dev/null | xargs -r kill -9 2>/dev/null
echo ""

echo "[2/3] 停止前端服务..."
if [ -f "$SCRIPT_DIR/logs/frontend.pid" ]; then
    FRONTEND_PID=$(cat "$SCRIPT_DIR/logs/frontend.pid" 2>/dev/null)
    if [ -n "$FRONTEND_PID" ]; then
        kill -9 $FRONTEND_PID 2>/dev/null
        echo "✅ 前端已停止 (PID: $FRONTEND_PID)"
    fi
    rm -f "$SCRIPT_DIR/logs/frontend.pid"
fi
# 兜底：通过端口查找
lsof -ti :5173 2>/dev/null | xargs -r kill -9 2>/dev/null
lsof -ti :5174 2>/dev/null | xargs -r kill -9 2>/dev/null
lsof -ti :5175 2>/dev/null | xargs -r kill -9 2>/dev/null
echo ""

echo "[3/3] 清理..."
# 清理可能残留的Java和Node进程
pkill -f "mvn spring-boot:run" 2>/dev/null
pkill -f "ai-safe-library-admin-1.0.0-SNAPSHOT.jar" 2>/dev/null
pkill -f "vite" 2>/dev/null
sleep 1

echo ""
echo "✅ 所有服务已停止！"
echo "========================================="
echo ""
