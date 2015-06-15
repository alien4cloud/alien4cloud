'use strict';

angular.module('alienUiApp').controller('ComponentCtrl', ['$rootScope', '$scope', '$state',
  function($rootScope, $scope, $state) {
    $scope.menu = [
      {
        id: 'cm.components.list',
        state: 'components.list',
        key: 'NAVBAR.MENU_COMPONENTS',
        icon: 'fa fa-cubes',
        show: true
      },
      {
        id: 'cm.components.csars.list',
        state: 'components.csars.list',
        key: 'NAVBAR.MENU_CSARS',
        icon: 'fa fa-archive',
        show: true
      }
    ];
  }]);
