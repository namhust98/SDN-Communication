pragma solidity >=0.4.22;
pragma experimental ABIEncoderV2;

contract ContractWithoutMerkle {

    struct Sdn {
        string dataSdn;
    }

    mapping (string => Sdn) sdnNode;

    function setSdn(string memory _ip, string memory _dataSdn) public {
        sdnNode[_ip].dataSdn = _dataSdn;
    }

    function getSdn(string memory _ipAddr) view public returns (string memory) {
        return (sdnNode[_ipAddr].dataSdn);
    }
}
