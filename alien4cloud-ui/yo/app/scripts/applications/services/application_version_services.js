'use strict';

angular.module('alienUiApp').factory('applicationVersionServices', ['$resource',
  function($resource) {

    // // Search for application environments
    // var searchEnvironmentResource = $resource('rest/applications/:applicationId/environments/search', {}, {
    //   'search': {
    //     method: 'POST',
    //     isArray: false,
    //     headers: {
    //       'Content-Type': 'application/json; charset=UTF-8'
    //     }
    //   }
    // });
    //
    // var applicationEnvironmentResource = $resource('rest/applications/:applicationId/environments', {}, {
    //   'create': {
    //     method: 'POST',
    //     isArray: false,
    //     headers: {
    //       'Content-Type': 'application/json; charset=UTF-8'
    //     }
    //   }
    // });
    //
    // var applicationEnvironmentMiscResource = $resource('rest/applications/:applicationId/environments/:applicationEnvironmentId', {}, {
    //   'get': {
    //     method: 'GET'
    //   },
    //   'delete': {
    //     method: 'DELETE'
    //   },
    //   'update': {
    //     method: 'PUT'
    //   }
    // });

    var versions = $resource('/rest/applications/:applicationId/environments/allVersions', {
      applicationId: '@applicationId'
    }, {
      'get': {
        method: 'GET',
        cache: true
      }
    });

    return {
      'getVersions': versions.get
    };

  }
]);
