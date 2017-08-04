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
      var resourceList;
      if (resultObject.error) {
        var baseResponse = $translate.instant('CSAR.ERRORS.' + resultObject.error.code);
        resourceList = baseResponse + ' : <ul>';
        resultObject.data.forEach(function getResource(resource) {
          resourceList += '<li>';
          resourceList += resource.resourceName + ' (' + resource.resourceType + ')';
          resourceList += '</li>';
        });
      }
      return resourceList;
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
    });

    var buildCsarZip = function(response, document, csarId) {
        var anchor = angular.element('<a/>');
        anchor.css({display: 'none'});
        angular.element(document.body).append(anchor);
        var url = URL.createObjectURL(new Blob([response.data], {'type':'application/octet-stream'}));
        anchor.attr({
          href: url,
          target: '_blank',
          download: csarId + '.zip'
        })[0].click();
        anchor.remove();
    };


    return {
      'getAndDeleteCsar': resultGetAndDelete,
      'searchCsar': searchCsar,
      'createNodeType': nodeTypeCreateDAO,
      'nodeTypeCRUDDAO': nodeTypeCRUDDAO,
      'builtErrorResultList': builtResultList,
      'downloadCsar': downloadCsar.download,
      'buildCsarZip': buildCsarZip
    };
  }]);
});
