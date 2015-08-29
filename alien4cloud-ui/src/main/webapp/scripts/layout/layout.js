define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  var alien4cloud = modules.get('alien4cloud');

  alien4cloud.controller('LayoutCtrl', ['$scope', 'menu', 'authService',
    function($scope, menu, authService) {
      _.each(menu, function(menuItem) {
        menuItem.show = false;
        if (authService.hasRole('ADMIN')) {
          menuItem.show = true;
        } else if(_.has(menuItem, 'roles')) {
          for (var role in menuItem.roles) {
            if (authService.hasRole(role)) {
              menuItem.show = true;
              break;
            }
          }
        } else { // if there is no roles requirement or if it's an ADMIN then the menu is visible
          menuItem.show = true;
        }
      });
      $scope.menu = menu;
    }
   ]);

  return alien4cloud;
});
