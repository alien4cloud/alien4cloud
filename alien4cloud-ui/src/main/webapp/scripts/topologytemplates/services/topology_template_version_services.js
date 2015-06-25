
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-templates', ['ngResource']).factory('topologyTemplateVersionServices', ['$resource',
    function($resource) {
      var searchVersionResource = $resource('rest/templates/:delegateId/versions/search', {}, {
        'search': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var versionResource = $resource('rest/templates/:delegateId/versions', {}, {
        'create': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        },
        'get': {
          method: 'GET'
        }
      });

      var versionMiscResource = $resource('rest/templates/:delegateId/versions/:versionId', {}, {
        'get': {
          method: 'GET'
        },
        'delete': {
          method: 'DELETE'
        },
        'update': {
          method: 'PUT'
        }
      });

      return {
        'getFirst': versionResource.get,
        'create': versionResource.create,
        'get': versionMiscResource.get,
        'delete': versionMiscResource.delete,
        'update': versionMiscResource.update,
        'searchVersion': searchVersionResource.search
      };

    }
  ]); // controller
}); // define
