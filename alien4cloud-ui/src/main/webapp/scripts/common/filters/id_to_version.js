define(function (require) {
  'use strict';

  var _ = require('lodash');
  var modules = require('modules');

  modules.get('a4c-common').filter('idToVersion', function() {
    return function(id) {
      if(_.defined(id)) {
        var splitted = id.split(':');
        return splitted[splitted.length-1];
      }
      return id;
    };
  }); // filter
}); // define
