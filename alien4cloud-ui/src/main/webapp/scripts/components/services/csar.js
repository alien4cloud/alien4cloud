define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-components', ['ngResource']).factory('csarService', ['$resource', '$translate', function($resource, $translate) {
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

    // Prepare result html for toaster message
    var builtResultList = function builtResultList(resultObject) {
      var resourtceList;
      if (resultObject.error) {
        var baseResponse = $translate('CSAR.ERRORS.' + resultObject.error.code);
        resourtceList = baseResponse + ' : <ul>';
        resultObject.data.forEach(function getResource(resource) {
          resourtceList += '<li>';
          resourtceList += resource.resourceName + ' (' + resource.resourceType + ')';
          resourtceList += '</li>';
        });
      }
      return resourtceList;
    };


    return {
      'getAndDeleteCsar': resultGetAndDelete,
      'getActiveDeployment': csarActiveDeploymentDAO,
      'searchCsar': searchCsar,
      'createNodeType': nodeTypeCreateDAO,
      'nodeTypeCRUDDAO': nodeTypeCRUDDAO,
      'builtErrorResultList': builtResultList
    };
  }]);
});
