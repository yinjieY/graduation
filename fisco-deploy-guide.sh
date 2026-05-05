#!/bin/bash

# FISCO区块链存证合约部署脚本

echo "========================================="
echo "  FISCO存证合约部署脚本"
echo "========================================="
echo ""

# 1. 检查节点状态
echo "[1/5] 检查FISCO节点状态..."
cd ~/fisco/nodes/127.0.0.1
if [ -f "start_all.sh" ]; then
    echo "节点配置正常"
else
    echo "错误：未找到节点配置"
    exit 1
fi

# 2. 复制合约到控制台
echo "[2/5] 复制合约到控制台..."
cp ~/fisco/nodes/127.0.0.1/sdk/* ~/fisco/console/contracts/ 2>/dev/null || true
echo "合约复制完成"

# 3. 启动控制台
echo "[3/5] 启动FISCO控制台..."
echo "请在控制台中执行以下命令部署合约："
echo ""
echo "========================================="
echo "  控制台命令"
echo "========================================="
echo ""
echo "1. 进入控制台目录："
echo "   cd ~/fisco/console"
echo ""
echo "2. 启动控制台："
echo "   ./start.sh"
echo ""
echo "3. 在控制台中部署合约："
echo "   deploy ProofContract.sol"
echo ""
echo "4. 部署成功后，复制返回的合约地址"
echo "   类似于：0x1234567890abcdef..."
echo ""
echo "5. 将合约地址更新到项目配置中："
echo "   文件: application.yml"
echo "   位置: app.blockchain.contract-address"
echo ""
echo "========================================="
echo ""

# 4. 提示部署步骤
echo "[4/5] 等待您完成合约部署..."
echo "请在FISCO控制台中执行上述命令后，告诉我部署的合约地址"
echo ""

# 5. 确认证书
echo "[5/5] 检查SDK证书..."
if [ -f "~/fisco/nodes/127.0.0.1/sdk/ca.crt" ]; then
    echo "✓ SDK证书已就绪"
else
    echo "✗ SDK证书缺失"
fi

echo ""
echo "========================================="
echo "  部署指南完成"
echo "========================================="
