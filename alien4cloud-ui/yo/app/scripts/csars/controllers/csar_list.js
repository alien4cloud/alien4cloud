'use strict';

var newCsarCtrl = ['$scope', '$modalInstance',
  function($scope, $modalInstance) {
    $scope.csar = {};
    $scope.create = function(valid) {
      if (valid) {
        $modalInstance.close($scope.csar);
      }
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

  }
];

/* Main CSAR search controller */
angular.module('alienUiApp').controller('CsarListCtrl', ['$scope', '$modal', '$state', 'csarService',
  function($scope, $modal, $state, csarService) {
    /* Modal to create a new CSAR snapshot */
    $scope.openNewCSAR = function() {

      var modalInstance = $modal.open({
        templateUrl: 'newCsar.html',
        controller: newCsarCtrl
      });

      modalInstance.result.then(function(csar) {
        // create a new csar from name / version / description
        // by default, the version is created with an -SNAPSHOT extension
        csarService.createCsarSnapshot.create([], angular.toJson(csar), function() {
          $scope.search();
        });
      });
    };

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
