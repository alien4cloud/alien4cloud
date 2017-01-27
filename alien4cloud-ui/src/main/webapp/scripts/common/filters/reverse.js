define(function (require) {
  'use strict';

  var _ = require('lodash');
  var modules = require('modules');

  modules.get('a4c-common').filter('reverse', function() {
    return function(items) {
      if(_.defined(items)) {
        return items.slice().reverse();
      }
      return items;
    };
  }); // filter
}); // define
