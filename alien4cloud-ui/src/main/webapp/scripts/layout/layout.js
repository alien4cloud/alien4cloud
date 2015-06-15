define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  var alien4cloud = modules.get('alien4cloud');

  // defines layout controller
  alien4cloud.controller('LayoutCtrl', ['$scope', 'menu',
    function($scope, menu) {
      _.each(menu, function(menuItem) {
        if(_.has(menuItem, 'roles')) {
          // TODO check roles to see if we can display the element.
          console.log('Role management is not yet implemented...');
        } else { // if there is no rôles requirement then the menu is visible
          menuItem.show = true;
        }
      });
      $scope.menu = menu;
    }
   ]);

  return alien4cloud;
});
