define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators', ['a4c-common']).factory('orchestratorService', ['$alresource',
    function($alresource) {
      return $alresource('rest/orchestrators/:orchestratorId');
    }
  ]);
  
});
