'use strict';

/* Main CSAR search controller */
angular.module('alienUiApp').controller('CsarListCtrl', ['$scope', '$modal', '$state', 'csarService', '$translate', 'toaster',
  function($scope, $modal, $state, csarService, $translate, toaster) {
    $scope.search = function() {
      var searchRequestObject = {
        'query': $scope.query,
        'from': 0,
        'size': 50
      };
      $scope.csarSearchResult = csarService.searchCsar.search([], angular.toJson(searchRequestObject));
    };

    $scope.openCsar = function(csarId) {
      $state.go('components.csars.csardetail', {
        csarId: csarId
      });
    };

    // remove a csar
    $scope.remove = function(csarId) {
      csarService.getAndDeleteCsar.remove({
        csarId: csarId
      }, function(result) {
        var errorMessage = csarService.builtErrorResultList(result);
        if (errorMessage) {
          var title = $translate('CSAR.ERRORS.' + result.error.code + '_TITLE');
          toaster.pop('error', title, errorMessage, 4000, 'trustedHtml', null);
        }
        // refresh csar list
        $scope.search();
      });
    };

    // init search
    $scope.search();
  }
]);
