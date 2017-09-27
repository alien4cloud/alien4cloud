define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/_ref/common/services/breadcrumbs_service');


  modules.get('a4c-common', []).directive('a4cBreadcrumbs', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/_ref/common/breadcrumbs.html',
      controller: 'BreadcrumbsCtrl'
    };
  });

  modules.get('a4c-common', []).controller('BreadcrumbsCtrl', ['$scope', '$rootScope', 'breadcrumbsService',
    function ($scope, $rootScope, breadcrumbsService) {
      $scope.items = breadcrumbsService.getItems();

      $rootScope.$on('breadcrumbsUpdated', function(){
        $scope.items = breadcrumbsService.getItems();
      });

    }]);
});
