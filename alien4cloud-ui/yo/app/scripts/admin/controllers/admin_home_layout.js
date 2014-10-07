'use strict';

angular.module('alienUiApp').controller('AdminLayoutHomeCtrl', ['$scope', '$interval', 'hopscotchService', 'adminMenu',
  function($scope, $interval, hopscotchService, adminMenu) {
    $scope.menu = adminMenu;
    $interval(function() {
      hopscotchService.resumeTour('admin-hopscotch', 1);
    }, 10, 1);
  }]);
