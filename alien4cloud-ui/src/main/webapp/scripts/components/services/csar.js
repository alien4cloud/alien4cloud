define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-components', ['ngResource']).factory('csarService', ['$resource', function($resource) {
    var nodeTypeCreateDAO = $resource('rest/csars/:csarId/nodetypes/', {}, {
      'upload': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var nodeTypeCRUDDAO = $resource('rest/csars/:csarId/nodetypes/:nodeTypeId', {}, {});

    var resultGetAndDelete = $resource('rest/csars/:csarId', {
      csarId: '@csarId'
    }, {
      'remove': {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'get': {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var searchCsar = $resource('rest/csars/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var csarActiveDeploymentDAO = $resource('rest/csars/:csarId/active-deployment');

    return {
      'getAndDeleteCsar': resultGetAndDelete,
      'getActiveDeployment': csarActiveDeploymentDAO,
      'searchCsar': searchCsar,
      'createNodeType': nodeTypeCreateDAO,
      'nodeTypeCRUDDAO': nodeTypeCRUDDAO
    };
  }]);
});
