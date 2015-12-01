/* global by */
'use strict';

var common = require('../common/common');

var go = function() {
  common.click(by.id('menu.components'));
};
module.exports.go = go;
