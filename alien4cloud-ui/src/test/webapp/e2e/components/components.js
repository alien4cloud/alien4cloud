/* global by */
'use strict';

var common = require('../common/common');

var go = function() {
  common.click(by.id('menu.components'));
};
module.exports.go = go;

module.exports.tags = {
  goodTag: {
    key: 'my_good_tag',
    value: 'Whatever i want to add as value here...'
  },
  goodTag2: {
    key: 'my_good_tag2',
    value: 'Whatever i want to add as value here for ...'
  },
  badTag: {
    key: 'my_good*tag',
    value: 'This tag should not be added to tag list with *...'
  }
};

var search = function(text){
  common.sendKeys(by.id('component_query_input'), text);
  common.click(by.id('btn-search-component'));
}
module.exports.search = search;
