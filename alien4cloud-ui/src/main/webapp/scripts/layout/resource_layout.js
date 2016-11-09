define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  var alien4cloud = modules.get('a4c-common');

  alien4cloud.factory('resourceLayoutService', ['authService',
    function(authService) {
      return {
        process: function(menu, resource) {
          _.each(menu, function(menuItem) {
            if(_.has(menuItem, 'roles')) {
              menuItem.show = authService.hasResourceRoleIn(resource, menuItem.roles);
            } else { // if there is no roles requirement or if it's an ADMIN then the menu is visible
              menuItem.show = true;
            }
          });
        }
      };
    }
  ]);

  // defines layout controller
  alien4cloud.controller('ResourceLayoutCtrl', ['$scope', 'menu', 'resourceLayoutService', 'resource',
    function( $scope, menu, layoutService, resource) {
      // console.log('resource', resource);
      layoutService.process(menu, resource);
      $scope.menu = menu;
      $scope.updateMenu = function() {
        layoutService.process(menu, resource);
      };
      $scope.onItemClick = function($event, menuItem) {
        if (menuItem.disabled) {
          $event.preventDefault();
          $event.stopPropagation();
        }
      };
    }
  ]);

  return alien4cloud;
});
