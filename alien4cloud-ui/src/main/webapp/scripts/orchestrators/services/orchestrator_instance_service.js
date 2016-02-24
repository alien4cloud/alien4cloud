define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators', ['a4c-common']).factory('orchestratorInstanceService', ['$alresource',
    function($alresource) {
      var deleteParams = {
        force: '@force',
        clear: '@clear' // clear all deployments associated with the orchestrator
      };

      var operations = {
        'remove': {
          method: 'DELETE',
          params: deleteParams,
          isArray: false,
          headers: {'Content-Type': 'application/json; charset=UTF-8'}
        }
      };

      return $alresource('rest/v1/orchestrators/:orchestratorId/instance', operations);
    }
  ]);
});
