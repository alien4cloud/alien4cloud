define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators', ['a4c-common']).factory('orchestratorConfigurationService', ['$alresource',
    function($alresource) {
      return $alresource('rest/v1/orchestrators/:orchestratorId/configuration');
    }
  ]);
});
