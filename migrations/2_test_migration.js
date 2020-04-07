const fs = require('fs');
const edn = require("jsedn");

const { smartContractsTemplate } = require ("./utils.js");
const { smart_contracts_path } = require ('../truffle.js');

const MintableToken = artifacts.require("MintableToken");

/**
 * This migration deploys the test smart contract suite
 *
 * Usage:
 * npx truffle migrate --network ganache
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  deployer
    .then (() => {
      console.log ("@@@ using Web3 version:", web3.version.api);
      console.log ("@@@ using address", address);
    })
    .then (() => deployer.deploy (MintableToken, Object.assign(opts, {gas: gas})))
    .then ((instance) => {

      var smartContracts = edn.encode(
        new edn.Map([

          edn.kw(":mintable-token"), new edn.Map([edn.kw(":name"), "MintableToken",
                                                  edn.kw(":address"), instance.address])
        ]));

      console.log (smartContracts);
      fs.writeFileSync(smart_contracts_path, smartContractsTemplate (smartContracts, "test"));
    })
    .catch(console.error);

  deployer.then (function () {
    console.log ("Done");
  });

}
