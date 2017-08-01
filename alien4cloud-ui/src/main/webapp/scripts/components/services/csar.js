define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-components', ['ngResource']).factory('csarService', ['$resource', '$translate', function($resource, $translate) {
    var nodeTypeCreateDAO = $resource('rest/latest/csars/:csarId/nodetypes/', {}, {
      'upload': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var nodeTypeCRUDDAO = $resource('rest/latest/csars/:csarId/nodetypes/:nodeTypeId', {}, {});

    var resultGetAndDelete = $resource('rest/latest/csars/:csarId', {
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

    var searchCsar = $resource('rest/latest/csars/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    // Prepare result html for toaster message
    var builtResultList = function builtResultList(resultObject) {
      var resourtceList;
      if (resultObject.error) {
        var baseResponse = $translate.instant('CSAR.ERRORS.' + resultObject.error.code);
        resourtceList = baseResponse + ' : <ul>';
        resultObject.data.forEach(function getResource(resource) {
          resourtceList += '<li>';
          resourtceList += resource.resourceName + ' (' + resource.resourceType + ')';
          resourtceList += '</li>';
        });
      }
      return resourtceList;
    };

    // Download the csar
    var downloadCsar = $resource('rest/latest/csars/:csarId/download', {}, {
       'download': {
          method: 'GET',
          responseType: 'arraybuffer',
          transformResponse: function(data, headers, status) {
            var response = {};
            response.data = data;
            response.headers = headers();
            response.status = status;
            return response;
          }
       }
    })


    return {
      'getAndDeleteCsar': resultGetAndDelete,
      'searchCsar': searchCsar,
      'createNodeType': nodeTypeCreateDAO,
      'nodeTypeCRUDDAO': nodeTypeCRUDDAO,
      'builtErrorResultList': builtResultList,
      'downloadCsar': downloadCsar.download
    };
  }]);
});
