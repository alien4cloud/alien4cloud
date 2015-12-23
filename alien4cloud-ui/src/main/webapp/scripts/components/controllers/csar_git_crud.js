define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-components', ['ui.bootstrap']).controller('CsarGitCrudController', ['$scope', '$modalInstance', 'csar',
    function($scope, $modalInstance, csar) {
      $scope.csarGit = csar;
      $scope.create = function(csarGit) {
        $modalInstance.close($scope.csarGit);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
        $scope.id = 0;
        $scope.csarGit = undefined;
      };

      $scope.removeCsarLocation = function(index){
        $scope.csarGit.importLocations.splice(index, 1);
      }
      var removeIfLocationExists=function(location){
        for (var i=0;i<$scope.csarGit.importLocations.length;i++) {
          var loc = $scope.csarGit.importLocations[i];
          if (loc.subPath === location.subPath && loc.branchId === location.branchId) {
            $scope.csarGit.importLocations.splice(i, 1);
            return;
          }
        }
      };

      var resetLocationForm=function(location){
        location.subPath = '';
        location.branchId = '';
      };

      $scope.addLocation=function(location){
        $scope.csarGit.importLocations = $scope.csarGit.importLocations || [];
        removeIfLocationExists(location);
        $scope.csarGit.importLocations.push({
          subPath: location.subPath,
          branchId: location.branchId
        });
        resetLocationForm(location);
      };
    }
  ]);
});
