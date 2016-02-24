/* global define */

'use strict';

define(function (require) {
  var modules = require('modules');

  modules.get('a4c-security', ['ngResource']).factory('groupServices', ['$resource', function($resource) {

    //search group
    var searchGroups = $resource('rest/v1/groups/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    //CRUD group
    var crudGroups = $resource('rest/v1/groups/:groupId', {}, {
      'create': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'update': {
        method: 'PUT',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'read': {
        method: 'GET',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'remove': {
        method: 'DELETE',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var getGroups = $resource('rest/v1/groups/getGroups', {}, {
      'get': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    //add / remove roles
    var manageRoles = $resource('rest/v1/groups/:groupId/roles/:role', {}, {
      'add': {
        method: 'PUT',
        params: {
          groupId: '@groupId',
          role: '@role'
        }
      },
      'remove': {
        method: 'DELETE',
        params: {
          groupId: '@groupId',
          role: '@role'
        }
      }
    });

    //add / remove users
    var manageUsers = $resource('rest/v1/groups/:groupId/users/:username', {}, {
      'add': {
        method: 'PUT',
        params: {
          groupId: '@groupId',
          username: '@username'
        }
      },
      'remove': {
        method: 'DELETE',
        params: {
          groupId: '@groupId',
          username: '@username'
        }
      }
    });

    return {
      'ALL_USERS_GROUP': 'ALL_USERS',
      'search': searchGroups.search,
      'create': crudGroups.create,
      'update': crudGroups.update,
      'remove': crudGroups.remove,
      'get': crudGroups.read,
      'getMultiple': getGroups.get,
      'addRole': manageRoles.add,
      'removeRole': manageRoles.remove,
      'addUser': manageUsers.add,
      'removeUser': manageUsers.remove
    };
  }]);
});
