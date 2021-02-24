pragma solidity >0.4.16;
pragma experimental ABIEncoderV2;

import {Bits} from "./Bits.sol";

library Data {

    struct Label {
        bytes32 data;
        uint length;
    }

    struct Edge {
        bytes32 node;
        Label label;
    }

    struct Node {
        Edge[2] children;
    }

    struct Tree {
        bytes32 root;
        Data.Edge rootEdge;
        mapping(bytes32 => Data.Node) nodes;
    }

    function splitCommonPrefix(Label memory self, Label memory other) internal pure returns (
        Label memory prefix,
        Label memory labelSuffix
    ) {
        return splitAt(self, commonPrefix(self, other));
    }

    function splitAt(Label memory self, uint pos) internal pure returns (Label memory prefix, Label memory suffix) {
        assert(pos <= self.length && pos <= 256);
        prefix.length = pos;
        if (pos == 0) {
            prefix.data = bytes32(0);
        } else {
            prefix.data = bytes32(uint(self.data) & ~uint(1) << 255 - pos);
        }
        suffix.length = self.length - pos;
        suffix.data = self.data << pos;
        return (prefix, suffix);
    }

    function commonPrefix(Label memory self, Label memory other) internal pure returns (uint prefix) {
        uint length = self.length < other.length ? self.length : other.length;
        if (length == 0) {
            return 0;
        }
        uint diff = uint(self.data ^ other.data) & ~uint(0) << 256 - length; // TODO Mask should not be needed.
        if (diff == 0) {
            return length;
        }
        return 255 - Bits.highestBitSet(diff);
    }

    function removePrefix(Label memory self, uint prefix) internal pure returns (Label memory r) {
        require(prefix <= self.length);
        r.length = self.length - prefix;
        r.data = self.data << prefix;
        return r;
    }

    function chopFirstBit(Label memory self) internal pure returns (uint firstBit, Label memory tail) {
        require(self.length > 0);
        return (uint(self.data >> 255), Label(self.data << 1, self.length - 1));
    }

    function edgeHash(Data.Edge memory self) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(self.node, self.label.length, self.label.data));
    }

    function hash(Data.Node memory self) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(edgeHash(self.children[0]), edgeHash(self.children[1])));
    }

    function insertNode(Data.Tree storage tree, Data.Node memory n) internal returns (bytes32 newHash) {
        bytes32 h = hash(n);
        tree.nodes[h].children[0] = n.children[0];
        tree.nodes[h].children[1] = n.children[1];
        return h;
    }

    function replaceNode(Data.Tree storage self, bytes32 oldHash, Data.Node memory n) internal returns (bytes32 newHash) {
        delete self.nodes[oldHash];
        return insertNode(self, n);
    }

    function insertAtEdge(Tree storage self, Edge memory e, Label memory key, bytes32 value) internal returns (Edge memory) {
        assert(key.length >= e.label.length);
        Label memory prefix;
        Label memory suffix;
        (prefix, suffix) = splitCommonPrefix(key, e.label);
        bytes32 newNodeHash;
        if (suffix.length == 0) {
            // Full match with the key, update operation
            newNodeHash = value;
        } else if (prefix.length >= e.label.length) {
            // Partial match, just follow the path
            assert(suffix.length > 1);
            Node memory n = self.nodes[e.node];
            uint head;
            Label memory tail;
            (head, tail) = chopFirstBit(suffix);
            n.children[head] = insertAtEdge(self, n.children[head], tail, value);
            delete self.nodes[e.node];
            newNodeHash = insertNode(self, n);
        } else {
            // Mismatch, so let us create a new branch node.
            uint head;
            Label memory tail;
            (head, tail) = chopFirstBit(suffix);
            Node memory branchNode;
            branchNode.children[head] = Edge(value, tail);
            branchNode.children[1 - head] = Edge(e.node, removePrefix(e.label, prefix.length + 1));
            newNodeHash = insertNode(self, branchNode);
        }
        return Edge(newNodeHash, prefix);
    }

    function insert(Tree storage self, bytes memory key, bytes memory value) internal {
        Label memory k = Label(keccak256(key), 256);
        bytes32 valueHash = keccak256(value);
        Edge memory e;
        if (self.root == 0) {
            e.label = k;
            e.node = valueHash;
        } else {
            e = insertAtEdge(self, self.rootEdge, k, valueHash);
        }
        self.root = edgeHash(e);
        self.rootEdge = e;
    }
}