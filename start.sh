#!/bin/bash

# AI安全事件库 - 一键启动脚本
# 使用方法: ./start.sh

echo "========================================="
echo "  AI安全事件库 - 一键启动"
echo "========================================="
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR"
FRONTEND_DIR="$SCRIPT_DIR/../ai_safe_library_frontend"

echo "后端目录: $BACKEND_DIR"
echo "前端目录: $FRONTEND_DIR"
echo ""

# 停止可能在运行的进程
echo "[1/5] 停止旧进程..."
lsof -ti :8080 2>/dev/null | xargs -r kill -9 2>/dev/null
lsof -ti :5173 2>/dev/null | xargs -r kill -9 2>/dev/null
lsof -ti :5174 2>/dev/null | xargs -r kill -9 2>/dev/null
lsof -ti :5175 2>/dev/null | xargs -r kill -9 2>/dev/null
sleep 1
echo "✅ 旧进程已停止"
echo ""

# 编译并安装后端，确保 admin 启动时拿到最新 business/system 依赖
echo "[2/5] 编译并安装后端..."
cd "$BACKEND_DIR"
mvn clean install -DskipTests -q
if [ $? -ne 0 ]; then
    echo "❌ 后端编译/安装失败"
    exit 1
fi
echo "✅ 后端编译并安装成功"
echo ""

# 创建日志目录
mkdir -p "$BACKEND_DIR/logs"

# 启动后端（后台运行）
echo "[3/5] 启动后端服务..."
cd "$BACKEND_DIR"
nohup java -jar "$BACKEND_DIR/ai-safe-library-admin/target/ai-safe-library-admin-1.0.0-SNAPSHOT.jar" > "$BACKEND_DIR/logs/backend.log" 2>&1 &
BACKEND_PID=$!
echo "后端PID: $BACKEND_PID"
echo $BACKEND_PID > "$BACKEND_DIR/logs/backend.pid"

# 等待后端启动
echo "等待后端启动..."
sleep 15

# 检查后端是否启动成功
if curl -s http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
    echo "✅ 后端服务启动成功"
else
    echo "⚠️  后端可能正在启动中，请稍后检查日志"
fi
echo ""

# 启动前端（后台运行）
echo "[4/5] 启动前端服务..."
cd "$FRONTEND_DIR"
nohup npm run dev > "$BACKEND_DIR/logs/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "前端PID: $FRONTEND_PID"
echo $FRONTEND_PID > "$BACKEND_DIR/logs/frontend.pid"

# 等待前端启动
echo "等待前端启动..."
sleep 5

echo ""
echo "[5/5] 启动完成！"
echo "========================================="
echo "  后端: http://localhost:8080/api"
echo "  前端: http://localhost:5173"
echo ""
echo "  查看日志:"
echo "    后端: tail -f $BACKEND_DIR/logs/backend.log"
echo "    前端: tail -f $BACKEND_DIR/logs/frontend.log"
echo ""
echo "  停止服务:"
echo "    ./stop.sh"
echo "========================================="
echo ""
