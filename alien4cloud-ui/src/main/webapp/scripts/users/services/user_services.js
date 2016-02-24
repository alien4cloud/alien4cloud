define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-security', ['ngResource']).factory('userServices', ['$resource',
  function($resource) {

    //search user
    var searchUsers = $resource('rest/v1/users/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    //CRUD user
    var crudUsers = $resource('rest/v1/users/:username', {}, {
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

    var getUsers = $resource('rest/v1/users/getUsers', {}, {
      'get': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var alienRolesResource = $resource('rest/v1/auth/roles', {}, {
      method: 'GET'
    });

    //add / remove roles
    var manageRoles = $resource('rest/v1/users/:username/roles/:role', {}, {
      'add': {
        method: 'PUT',
        params: {
          username: '@username',
          role: '@role'
        }
      },
      'remove': {
        method: 'DELETE',
        params: {
          username: '@username',
          role: '@role'
        }
      }
    });

    //add / remove roles
    var groupResource = $resource('rest/v1/users/:username/groups/:group', {}, {
      'add': {
        method: 'PUT',
        params: {
          username: '@username',
          group: '@group'
        }
      },
      'remove': {
        method: 'DELETE',
        params: {
          username: '@username',
          group: '@group'
        }
      }
    });

    return {
      'search': searchUsers.search,
      'create': crudUsers.create,
      'update': crudUsers.update,
      'remove': crudUsers.remove,
      'get': getUsers.get,
      'getAlienRoles': alienRolesResource.get,
      'addRole': manageRoles.add,
      'removeRole': manageRoles.remove,
      'addGroup': groupResource.add,
      'removeGroup': groupResource.remove,
      'addToGroupArray': function(user, group) {
        if (!user.groups) {
          user.groups = [];
        }
        user.groups.push(group);
      },
      'removeFromGroupArray': function(user, group) {
        if (!user.groups) {
          return;
        }
        var index = user.groups.indexOf(group);
        user.groups.splice(index, 1);
      },
      'addToRoleArray': function(user, role) {
        if (!user.roles) {
          user.roles = [];
        }
        user.roles.push(role);
      },
      'removeFromRoleArray': function(user, role) {
        if (!user.roles) {
          return;
        }
        var index = user.roles.indexOf(role);
        user.roles.splice(index, 1);
      },
      'initRolesToDisplay': function(user) {
        // idenfity if the role if from a group, the user himself or both
        var finalMerge = {};
        if (user.roles) {
          user.roles.forEach(function(elem) {
            finalMerge[elem] = finalMerge[elem] || [];
            finalMerge[elem].push('u');
          });
        }
        if (user.groupRoles) {
          user.groupRoles.forEach(function(elem) {
            finalMerge[elem] = finalMerge[elem] || [];
            finalMerge[elem].push('g');
          });
        }
        user.allRoles = finalMerge;
        user.allRoles.size = Object.keys(finalMerge).length;
      }
    };
  }]);
});
