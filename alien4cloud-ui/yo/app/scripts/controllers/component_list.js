'use strict';

angular.module('alienUiApp').controller('SearchComponentCtrl', ['alienAuthService', '$scope', '$state', 'resizeServices',
  function(alienAuthService, $scope, $state, resizeServices) {
    $scope.isComponentManager = alienAuthService.hasRole('COMPONENTS_MANAGER');

    $scope.uploadSuccessCallback = function(data) {
      $scope.refresh = data;
    };

    $scope.openComponent = function(component) {
      $state.go('components.detail', { id: component.id });
    };

    function onResize(width, height) {
      $scope.heightInfo = { height: height };
      $scope.$apply();
    }

    // register for resize events
    window.onresize = function() {
      $scope.onResize();
    };

    resizeServices.register(onResize, 0, 0);
    $scope.heightInfo = { height: resizeServices.getHeight(0) };
  }
]);
