// SPDX-License-Identifier: MIT
pragma solidity ^0.4.25;

contract ProofContract {
    // 存证结构
    struct Proof {
        string businessKey;
        string proofType;
        string hash;
        string payload;
        uint256 timestamp;
        address submitter;
    }

    // 存证映射：businessKey -> Proof
    mapping(string => Proof) public proofs;

    // 验证记录是否存在
    mapping(string => bool) public exists;

    // 事件：存证成功
    event ProofSaved(
        string indexed businessKey,
        string proofType,
        string hash,
        uint256 timestamp
    );

    // 事件：验证查询
    event ProofVerified(
        string indexed businessKey,
        bool result,
        uint256 timestamp
    );

    // 保存存证
    function save(
        string memory businessKey,
        string memory proofType,
        string memory hash,
        string memory payload
    ) public {
        require(bytes(businessKey).length > 0, "businessKey cannot be empty");
        require(bytes(hash).length > 0, "hash cannot be empty");
        require(!exists[businessKey], "Proof already exists for this businessKey");

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

    // 验证存证
    function verify(
        string memory businessKey,
        string memory hash
    ) public view returns (bool) {
        if (!exists[businessKey]) {
            return false;
        }

        bool result = keccak256(abi.encodePacked(proofs[businessKey].hash)) ==
                      keccak256(abi.encodePacked(hash));

        return result;
    }

    // 获取存证详情
    function getProof(string memory businessKey) public view returns (
        string memory proofType,
        string memory hash,
        string memory payload,
        uint256 timestamp,
        address submitter
    ) {
        require(exists[businessKey], "Proof does not exist");

        Proof storage p = proofs[businessKey];
        return (
            p.proofType,
            p.hash,
            p.payload,
            p.timestamp,
            p.submitter
        );
    }

    // 检查存证是否存在
    function hasProof(string memory businessKey) public view returns (bool) {
        return exists[businessKey];
    }
}
