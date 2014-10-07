'use strict';

/**
* This service allows manage a topology template.
*/
angular.module('alienUiApp').factory('topologyTemplateService', ['$resource',
  function($resource) {
    return $resource('rest/templates/topology/:topologyTemplateId', {}, {
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
  }]);
