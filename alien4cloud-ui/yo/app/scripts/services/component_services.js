'use strict';

angular.module('alienUiApp').factory('componentService', ['$http', '$resource', function($http, $resource) {
  var componentResource = $resource('rest/components/:componentId', {}, {
    'get': {
      method: 'GET'
    }
  });
  return {
    'get': componentResource.get,
    'getInArchives': function(elementName, componentType, dependencies) {
      return $http.post('rest/components/getInArchives', {
        'elementName': elementName,
        'componentType': componentType,
        'dependencies': dependencies
      });
    }
  };
}]);

angular.module('alienUiApp').factory('componentTagUpdate', ['$resource', function($resource) {
  // API REST Definition
  var resultUpdateComponentTag = $resource('rest/components/:componentId/tags', {}, {
    'upsert': {
      method: 'POST',
      isArray: false,
      headers: {
        'Content-Type': 'application/json; charset=UTF-8'
      }
    }
  });
  return resultUpdateComponentTag;
}]);

angular.module('alienUiApp').factory('componentTagDelete', ['$resource', function($resource) {
  // API REST Definition
  var resultDeleteComponentTag = $resource('rest/components/:componentId/tags/:tagKey', {}, {
    'remove': {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json; charset=UTF-8'
      }
    }
  });
  return resultDeleteComponentTag;
}]);

/* CSAR handling */
angular.module('alienUiApp').factory('csarService', ['$resource', function($resource) {

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

  // API REST Definition
  var resultCreateSnapshot = $resource('rest/csars', {}, {
    'create': {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json; charset=UTF-8'
      }
    }
  });

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
    'createCsarSnapshot': resultCreateSnapshot,
    'getAndDeleteCsar': resultGetAndDelete,
    'getActiveDeployment': csarActiveDeploymentDAO,
    'searchCsar': searchCsar,
    'createNodeType': nodeTypeCreateDAO,
    'nodeTypeCRUDDAO': nodeTypeCRUDDAO
  };
}]);
