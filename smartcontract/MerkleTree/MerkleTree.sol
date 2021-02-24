pragma solidity >=0.4.16;
pragma experimental ABIEncoderV2;

import {Data} from "./Data.sol";
import {Bits} from "./Bits.sol";

contract MerkleTree {

    using Data for Data.Tree;
    using Data for Data.Node;
    using Data for Data.Edge;
    using Data for Data.Label;
    using Bits for uint;

    Data.Tree internal tree;

    function getRootHash() public view returns (bytes32) {
        return tree.root;
    }

    function getRootEdge() public view returns (Data.Edge memory e) {
        e = tree.rootEdge;
    }

    function getNode(bytes32 hash) public view returns (Data.Node memory n) {
        n = tree.nodes[hash];
    }

    function getProof(bytes memory key) public view returns (uint branchMask, bytes32[] memory _siblings) {
        require(tree.root != 0);
        Data.Label memory k = Data.Label(keccak256(key), 256);
        Data.Edge memory e = tree.rootEdge;
        bytes32[256] memory siblings;
        uint length;
        uint numSiblings;
        while (true) {
            Data.Label memory prefix;
            Data.Label memory suffix;
            (prefix, suffix) = k.splitCommonPrefix(e.label);
            assert(prefix.length == e.label.length);
            if (suffix.length == 0) {
                break;
            }
            length += prefix.length;
            branchMask |= uint(1) << 255 - length;
            length += 1;
            uint head;
            Data.Label memory tail;
            (head, tail) = suffix.chopFirstBit();
            siblings[numSiblings++] = tree.nodes[e.node].children[1 - head].edgeHash();
            e = tree.nodes[e.node].children[head];
            k = tail;
        }
        if (numSiblings > 0) {
            _siblings = new bytes32[](numSiblings);
            for (uint i = 0; i < numSiblings; i++) {
                _siblings[i] = siblings[i];
            }
        }
        return (branchMask, _siblings);
    }

    function verifyProof(bytes32 rootHash, bytes memory key, bytes memory value, uint branchMask, bytes32[] memory siblings) public view returns (bool) {
        Data.Label memory k = Data.Label(keccak256(key), 256);
        Data.Edge memory e;
        e.node = keccak256(value);
        for (uint i = 0; branchMask != 0; i++) {
            uint bitSet = branchMask.lowestBitSet();
            branchMask &= ~(uint(1) << bitSet);
            (k, e.label) = k.splitAt(255 - bitSet);
            uint bit;
            (bit, e.label) = e.label.chopFirstBit();
            bytes32[2] memory edgeHashes;
            edgeHashes[bit] = e.edgeHash();
            edgeHashes[1 - bit] = siblings[siblings.length - i - 1];
            e.node = keccak256(abi.encodePacked(edgeHashes));
        }
        e.label = k;
        return rootHash == e.edgeHash();
    }

    function insert(bytes memory key, bytes memory value) public {
        tree.insert(key, value);
    }

}