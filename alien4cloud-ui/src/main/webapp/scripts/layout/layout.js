define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  var alien4cloud = modules.get('alien4cloud');

  // defines layout controller
  alien4cloud.controller('LayoutCtrl', ['$scope', 'menu', 'authService', 'context',
    function( $scope, menu, authService, context) {
      $scope.context = context;
      _.each(menu, function(menuItem) {
        menuItem.show = false;
        if (authService.hasRole('ADMIN')) {
          menuItem.show = true;
        } else if(_.has(menuItem, 'roles')) {
          _.every(menuItem.roles, function(role){
            if (authService.hasRole(role)) {
              menuItem.show = true;
              return false; // stop the every loop
            }
          });
        } else { // if there is no roles requirement or if it's an ADMIN then the menu is visible
          menuItem.show = true;
        }
      });
      $scope.menu = menu;
    }
   ]);

  return alien4cloud;
});
