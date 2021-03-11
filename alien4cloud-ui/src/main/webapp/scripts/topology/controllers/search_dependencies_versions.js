// Topology editor controller
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.bootstrap', 'pascalprecht.translate', 'a4c-tosca']).controller('SearchDependenciesVersions',
    ['$scope', '$uibModalInstance', 'searchServiceFactory',
    function($scope, $uibModalInstance, searchServiceFactory) {

      $scope.selectedVersion = $scope.dependency.version;

      $scope.queryManager = {
        filters: { name: [$scope.dependency.name] },
        hiddenFilters: ['name']
      };
      $scope.searchService = searchServiceFactory('rest/latest/csars/search', false, $scope.queryManager, 10, 20);

      $scope.select = function(selection) {
        if (selection !== $scope.dependency.version) {
          $scope.selectedVersion = selection;
        }
      }

      $scope.ok = function() {
        $uibModalInstance.close($scope.selectedVersion);
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };

  }]); // controller
}); // define
