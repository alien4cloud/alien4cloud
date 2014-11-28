'use strict';

/* Main CSAR search controller */
angular.module('alienUiApp').controller('CsarListCtrl', ['$scope', '$modal', '$state', 'csarService',
  function($scope, $modal, $state, csarService) {
    $scope.search = function() {
      var searchRequestObject = {
        'query': $scope.query,
        'from': 0,
        'size': 50
      };
      $scope.csarSearchResult = csarService.searchCsar.search([], angular.toJson(searchRequestObject));
    };

    $scope.openCsar = function(csarId) {
      $state.go('csardetail', { csarId: csarId });
    };

    // remove a csar
    $scope.remove = function(csarId) {
      csarService.getAndDeleteCsar.remove({
        csarId: csarId
      }, function() {
        // refresh csar list
        $scope.search();
      });
    };

    // init search
    $scope.search();
  }
]);
