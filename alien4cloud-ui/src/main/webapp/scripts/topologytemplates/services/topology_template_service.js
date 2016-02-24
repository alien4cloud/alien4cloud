
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-templates', ['ngResource']).factory('topologyTemplateService', ['$resource',
    function($resource) {
      return $resource('rest/v1/templates/topology/:topologyTemplateId', {}, {
        'get': {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        },
        'put': {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          },
          params: {
            topologyTemplateId: '@topologyTemplateId'
          }
        }
      });
    }
  ]); // controller
}); // define
