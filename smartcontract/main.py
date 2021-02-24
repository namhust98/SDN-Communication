from web3 import Web3
from web3.middleware import geth_poa_middleware
from solcx import compile_files
from flask import Flask, request
import json

app = Flask(__name__)

# wallet_private_key = 'c8b4e1880d1536fdf1490d4143ff33d724105f811b4a5f14bc8dad2233dcdd2c'
# public_address = '0x259db7854803611c8f7937c464f927ab614845fa'
# contract_address = '0x26807668DdBD2E0399FefEFD79E9272e106C2fe1'

w3 = Web3(Web3.IPCProvider('/home/namhust98/data_admin/geth.ipc'))
w3.middleware_onion.inject(geth_poa_middleware, layer=0)


def deploy_new_contract(public_address):
    w3.eth.defaultAccount = Web3.toChecksumAddress(public_address)
    compile_sol = compile_files('MerkleTree.sol')
    bytecode = compile_sol['MerkleTree.sol:MerkleTree']['bin']
    abi = compile_sol['MerkleTree.sol:MerkleTree']['abi']
    greeter = w3.eth.contract(abi=abi, bytecode=bytecode)

    return greeter.constructor().transact()


def update_data_contract(contract_address, public_address, key, value):
    w3.eth.defaultAccount = Web3.toChecksumAddress(public_address)
    compile_sol = compile_files('MerkleTree.sol')
    abi = compile_sol['MerkleTree.sol:MerkleTree']['abi']
    contract = w3.eth.contract(address=contract_address, abi=abi)

    return contract.functions.insert(key.encode('utf-8'), value.encode('utf-8')).transact()


def get_proof_contract(contract_address, public_address, key):
    w3.eth.defaultAccount = Web3.toChecksumAddress(public_address)
    compile_sol = compile_files('MerkleTree.sol')
    abi = compile_sol['MerkleTree.sol:MerkleTree']['abi']
    contract = w3.eth.contract(address=contract_address, abi=abi)

    return str(contract.functions.getProof(key.encode('utf-8')).call())


def get_root_contract(contract_address, public_address):
    w3.eth.defaultAccount = Web3.toChecksumAddress(public_address)
    compile_sol = compile_files('MerkleTree.sol')
    abi = compile_sol['MerkleTree.sol:MerkleTree']['abi']
    contract = w3.eth.contract(address=contract_address, abi=abi)
    return str(contract.functions.getRootHash().call())


def verify_data_contract(contract_address, public_address, root, key, value, branch, proof):
    w3.eth.defaultAccount = Web3.toChecksumAddress(public_address)
    compile_sol = compile_files('MerkleTree.sol')
    abi = compile_sol['MerkleTree.sol:MerkleTree']['abi']
    contract = w3.eth.contract(address=contract_address, abi=abi)
    return str(contract.functions.verifyProof(root, key.encode('utf-8'), value.encode('utf-8'), branch, proof).call())


@app.route('/deploy_contract', methods=['POST'])
def deploy_contract():
    string_data = request.data
    json_data = json.loads(string_data)
    deploy_new_contract(public_address=json_data['public_address'])
    return 'Success! Check your blockchain service to see detail'


@app.route('/update_contract', methods=['POST'])
def update_contract():
    string_data = b"" + request.data
    string_data = string_data.decode()
    json_data = json.loads(string_data)
    contract_address = json_data['contract_address']
    public_address = json_data['public_address']
    ip_controller = json_data['ip_controller']
    data_sdn = json_data['data_sdn']
    update_data_contract(contract_address, public_address, ip_controller, data_sdn)
    return 'Success! Check your blockchain service to see detail'


@app.route('/get_root', methods=['POST'])
def get_root():
    string_data = request.data
    json_data = json.loads(string_data)
    contract_address = json_data['contract_address']
    public_address = json_data['public_address']
    return get_root_contract(contract_address, public_address)


@app.route('/get_proof', methods=['POST'])
def get_proof():
    string_data = request.data
    json_data = json.loads(string_data)
    contract_address = json_data['contract_address']
    public_address = json_data['public_address']
    key = json_data['key']
    return get_proof_contract(contract_address, public_address, key)


@app.route('/verify_data', methods=['POST'])
def verify_data():
    string_data = request.data
    json_data = json.loads(string_data)
    contract_address = json_data['contract_address']
    public_address = json_data['public_address']
    key = json_data['key']
    value = json_data['value']
    root = json_data['root']
    branch = json_data['branch']
    proof = json_data['proof']

    return verify_data_contract(contract_address, public_address, root, key, value, branch, proof)


app.run(debug=True, port=10000, threaded=False)
