define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  var alien4cloud = modules.get('a4c-common');

  alien4cloud.factory('layoutService', ['authService',
    function(authService) {
      return {
        process: function(menu) {
          _.each(menu, function(menuItem) {
            menuItem.show = false;
            if (authService.hasRole('ADMIN')) {
              menuItem.show = true;
            } else if(_.has(menuItem, 'roles')) {
              _.every(menuItem.roles, function(role) {
                if (authService.hasRole(role)) {
                  menuItem.show = true;
                  return false; // stop the every loop
                }
              });
            } else { // if there is no roles requirement or if it's an ADMIN then the menu is visible
              menuItem.show = true;
            }
          });
        }
      };
    }
  ]);

  // defines layout controller
  alien4cloud.controller('LayoutCtrl', ['$scope', 'menu', 'layoutService', 'context',
    function( $scope, menu, layoutService, context) {
      $scope.context = context;
      layoutService.process(menu);
      $scope.menu = menu;
    }
  ]);

  return alien4cloud;
});
