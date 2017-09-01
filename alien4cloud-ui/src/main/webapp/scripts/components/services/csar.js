define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-components', ['ngResource']).factory('csarService', ['$resource', '$translate', function($resource, $translate) {

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

    return {
      'getAndDeleteCsar': resultGetAndDelete,
      'searchCsar': searchCsar,
      'builtErrorResultList': builtResultList
    };
  }]);
});
