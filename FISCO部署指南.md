# FISCO存证合约部署指南

## 第一步：打开WSL终端

在Windows上打开一个新的PowerShell或CMD窗口，执行：

```bash
wsl --distribution Ubuntu-20.04
```

进入WSL后，你会看到命令行提示符变为 `yin@DESKTOP-XXXXXX:~$`

---

## 第二步：创建合约文件

在WSL中执行：

```bash
cd ~/fisco/console/contracts/solidity
```

然后创建合约文件：

```bash
cat > ProofContract.sol << 'CONTRACT_EOF'
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract ProofContract {
    struct Proof {
        string businessKey;
        string proofType;
        string hash;
        string payload;
        uint256 timestamp;
        address submitter;
    }

    mapping(string => Proof) public proofs;
    mapping(string => bool) public exists;

    event ProofSaved(string indexed businessKey, string proofType, string hash, uint256 timestamp);

    function save(
        string calldata businessKey,
        string calldata proofType,
        string calldata hash,
        string calldata payload
    ) external {
        require(bytes(businessKey).length > 0, "businessKey cannot be empty");
        require(bytes(hash).length > 0, "hash cannot be empty");
        require(!exists[businessKey], "Proof already exists");

        proofs[businessKey] = Proof({
            businessKey: businessKey,
            proofType: proofType,
            hash: hash,
            payload: payload,
            timestamp: block.timestamp,
            submitter: msg.sender
        });

        exists[businessKey] = true;
        emit ProofSaved(businessKey, proofType, hash, block.timestamp);
    }

    function verify(string calldata businessKey, string calldata hash) external view returns (bool) {
        if (!exists[businessKey]) {
            return false;
        }
        return keccak256(abi.encodePacked(proofs[businessKey].hash)) ==
               keccak256(abi.encodePacked(hash));
    }

    function getProof(string calldata businessKey) external view returns (
        string memory proofType,
        string memory hash,
        string memory payload,
        uint256 timestamp,
        address submitter
    ) {
        require(exists[businessKey], "Proof does not exist");
        Proof storage p = proofs[businessKey];
        return (p.proofType, p.hash, p.payload, p.timestamp, p.submitter);
    }

    function hasProof(string calldata businessKey) external view returns (bool) {
        return exists[businessKey];
    }
}
CONTRACT_EOF
```

确认文件创建成功：
```bash
ls -la ProofContract.sol
```

---

## 第三步：启动FISCO控制台

```bash
cd ~/fisco/console
./start.sh
```

等待几秒钟，控制台启动后会显示：

```
=========================================
Welcome to FISCO BCOS console!
Type 'quit' or 'exit' to quit console
=========================================
[group:0]>
```

---

## 第四步：部署合约

在控制台中执行：

```bash
deploy ProofContract.sol
```

等待部署完成，成功后会显示类似：

```
Deploy successfully, contract address: 0x1234567890abcdef1234567890abcdef12345678
transaction hash: 0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890
```

**重要：请复制这个合约地址！**

---

## 第五步：更新项目配置

部署成功后：

1. 打开文件：`E:\Code\project\graduation\qs-block-service\src\main\resources\application.yml`

2. 找到这一行：
   ```yaml
   contract-address: "0xXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
   ```

3. 将 `0xXXXXXXXX...` 替换为实际部署的合约地址，例如：
   ```yaml
   contract-address: "0x1234567890abcdef1234567890abcdef12345678"
   ```

---

## 第六步：启动服务

配置完成后，启动qs-block-service：

```bash
# 在Windows的PowerShell中执行
cd E:\Code\project\graduation\qs-block-service
mvn spring-boot:run
```

---

## 测试存证功能

服务启动后，测试存证：

```bash
curl -X POST http://localhost:9095/block/proof/qr \
  -H "Content-Type: application/json" \
  -d '{
    "qsId": "QS-TEST-001",
    "batchId": "BATCH-001",
    "companyId": "COMPANY-001"
  }'
```

如果成功，会返回包含 `chainStatus: SUCCESS` 的响应！

---

## 常见问题

### 问题1：控制台启动失败
```
# 检查节点是否运行
cd ~/fisco/nodes/127.0.0.1
./start_all.sh
```

### 问题2：部署失败
```
# 检查节点状态
cd ~/fisco/nodes/127.0.0.1
ps aux | grep node
```

### 问题3：连接超时
检查项目配置中的 `peers` 是否为 `127.0.0.1:20200`

---

**恭喜！完成以上步骤后，您的项目就成功连接到FISCO-BCOS真实区块链了！**
