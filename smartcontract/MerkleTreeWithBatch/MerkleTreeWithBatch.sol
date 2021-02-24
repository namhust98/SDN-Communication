pragma solidity >=0.4.22;
pragma experimental ABIEncoderV2;

contract MerkleTreeWithBatch {

    struct Merkle {
        bytes32[9] frontier;
        uint leafCount;
    }

    mapping(uint => Merkle) merkleTree;
    uint treeCount;

    function insertNode(string memory data) public {
        bytes32 dataHash = keccak256(abi.encodePacked(data));
        uint _leafCount = merkleTree[treeCount].leafCount;
        bytes32[9] memory _frontier = merkleTree[treeCount].frontier;
        uint frontierSlot = getFrontierSlot(_leafCount);

        if (frontierSlot == 0) {
            merkleTree[treeCount].frontier[0] = dataHash;
        } else {
            for (uint i = 0; i < frontierSlot; i ++) {
                dataHash = keccak256(abi.encodePacked(_frontier[i], dataHash));
            }
            merkleTree[treeCount].frontier[frontierSlot] = dataHash;
        }
        merkleTree[treeCount].leafCount ++;

        if (merkleTree[treeCount].leafCount == 256) {
            treeCount ++;
        }
    }

    function insertNodes(string[] memory multiData){
        for (int i = 0; i < multiData.length; i++) {
            insertNode(multiData[i]);
        }
    }

    function getFrontierSlot(uint leafIndex) private pure returns (uint slot){
        slot = 0;
        if (leafIndex % 2 == 1) {
            uint exp1 = 1;
            uint pow1 = 2;
            uint pow2 = pow1 << 1;
            while (slot == 0) {
                if ((leafIndex + 1 - pow1) % pow2 == 0) {
                    slot = exp1;
                } else {
                    pow1 = pow2;
                    pow2 = pow2 << 1;
                    exp1 ++;
                }
            }
        }
    }
}
