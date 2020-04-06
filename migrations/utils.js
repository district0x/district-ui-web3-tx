const fs = require('fs');

const utils = {

  smartContractsTemplate (map, env) {
    return `(ns tests.smart-contracts-test)

(def smart-contracts
  ${map})
`;
  }

};

module.exports = utils;
