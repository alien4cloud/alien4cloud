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

    // var envEnumTypes = $resource('rest/enums/environmenttype', {}, {
    //   'get': {
    //     method: 'GET',
    //     cache : true
    //   }
    // });

    // TODO : replace this by a real call to rest service on application versions controller
    var versions = [{
      id: 'id-0.0.1',
      name: '0.0.1'
    }, {
      id: 'id-0.0.2-SNAPSHOT',
      name: '0.0.2-SNAPSHOT'
    }, {
      id: 'id-0.0.3-M1',
      name: '0.0.3-M1'
    }];

    return {
      'getVersions': versions
    };

  }
]);
