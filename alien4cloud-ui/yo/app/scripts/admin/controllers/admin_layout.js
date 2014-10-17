'use strict';

angular.module('alienUiApp').controller('AdminCtrl', ['$rootScope', '$scope', '$state', 'hopscotchService', 'adminMenu',
  function($rootScope, $scope, $state, hopscotchService, adminMenu) {
    $scope.menu = adminMenu;
  }]);
