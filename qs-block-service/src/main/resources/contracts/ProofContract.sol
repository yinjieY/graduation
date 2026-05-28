// SPDX-License-Identifier: MIT
pragma solidity ^0.4.25;

contract ProofContract {

    struct Proof {
        string businessKey;
        string proofType;
        string hash;
        string payload;
        uint256 timestamp;
        address submitter;
    }

    mapping(bytes32 => Proof) public proofs;
    mapping(bytes32 => bool) public exists;

    event ProofSaved(string indexed businessKey, string proofType, string hash, uint256 timestamp);
    event ProofVerified(string indexed businessKey, bool result, uint256 timestamp);

    function save(
        string memory businessKey,
        string memory proofType,
        string memory hash,
        string memory payload
    ) public {
        require(bytes(businessKey).length > 0, "businessKey cannot be empty");
        require(bytes(hash).length > 0, "hash cannot be empty");

        bytes32 key = keccak256(abi.encodePacked(businessKey, proofType));
        require(!exists[key], "Proof already exists");

        proofs[key] = Proof({
            businessKey: businessKey,
            proofType: proofType,
            hash: hash,
            payload: payload,
            timestamp: block.timestamp,
            submitter: msg.sender
        });

        exists[key] = true;
        emit ProofSaved(businessKey, proofType, hash, block.timestamp);
    }

    function verify(
        string memory businessKey,
        string memory proofType,
        string memory hash
    ) public view returns (bool) {
        bytes32 key = keccak256(abi.encodePacked(businessKey, proofType));
        if (!exists[key]) {
            return false;
        }
        return keccak256(abi.encodePacked(proofs[key].hash)) ==
               keccak256(abi.encodePacked(hash));
    }

    function getProof(string memory businessKey, string memory proofType) public view returns (
        string memory _businessKey,
        string memory _proofType,
        string memory _hash,
        uint256 _timestamp,
        address _submitter
    ) {
        bytes32 key = keccak256(abi.encodePacked(businessKey, proofType));
        require(exists[key], "Proof does not exist");
        Proof storage p = proofs[key];
        return (p.businessKey, p.proofType, p.hash, p.timestamp, p.submitter);
    }

    function hasProof(string memory businessKey, string memory proofType) public view returns (bool) {
        return exists[keccak256(abi.encodePacked(businessKey, proofType))];
    }
}
