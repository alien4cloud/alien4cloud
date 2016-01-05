/* global by */
'use strict';

var common = require('../common/common');
var components = require('./components');

var go = function() {
  components.go();
  common.click(by.id('cm.components.csars.list'));
};
module.exports.go = go;

var search = function(text){
  common.sendKeys(by.id('csar-query-input'), text);
  common.click(by.id('btn-search-csar'));
}
module.exports.search = search;

module.exports.open = function(name, version){
  search(name);
  common.click(by.id('csar_'+name+':'+version));
}
