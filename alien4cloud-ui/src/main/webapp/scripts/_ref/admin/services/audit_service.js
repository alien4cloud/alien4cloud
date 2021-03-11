define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('alien4cloud-admin', ['ngResource']).factory('auditService', ['$resource', function($resource) {

    // faceted search for audit traces
    var auditSearch = $resource('rest/latest/audit/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var auditConfiguration = $resource('rest/latest/audit/configuration/:field');

    return {
      'search': auditSearch.search,
      'getConfiguration': auditConfiguration.get,
      'saveConfiguration': auditConfiguration.save
    };

  }]);

});
