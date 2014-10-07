'use strict';

angular.module('alienUiApp').controller('AdminHomeCtrl', ['$scope', 'hopscotchService',
  function($scope, hopscotchService) {
    $scope.adminTour = function() {
      hopscotchService.startTour('admin')
    };
  }]);
