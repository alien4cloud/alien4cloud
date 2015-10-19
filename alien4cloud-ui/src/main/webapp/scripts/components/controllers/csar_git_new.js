define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-components', ['ui.bootstrap']).controller('NewCsarGitController', ['$scope', '$modalInstance',
    function($scope, $modalInstance) {
      $scope.csarGitTemplate = {};
      $scope.create = function(csarGit) {
        var locations = $scope.importLocation;
        var csargitDTO = {
          'username': csarGit.username,
          'password': csarGit.password,
          'repositoryUrl': csarGit.url,
          'importLocations': locations,
          'storedLocally': csarGit.isStoredLocally
        };
        $modalInstance.close(csargitDTO);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
        $scope.id = 0;
      };

      $scope.removeCsarLocation = function(subPath){
        for (var i=0;i<$scope.importLocation.length;i++) {
          var loc = $scope.importLocation[i];
          if (loc.subPath === subPath) {
            $scope.importLocation.splice(i, 1);
            return;
          }
        }
      }
      var removeIfLocationExists=function(location){
        for (var i=0;i<$scope.importLocation.length;i++) {
          var loc = $scope.importLocation[i];
          if (loc.subPath === location.subPath && loc.branchId === location.branchId) {
            $scope.importLocation.splice(i, 1);
            return;
          }
        }
      };

      var resetLocationForm=function(location){
        location.subPath = '';
        location.branchId = '';
      };

      $scope.addLocation=function(location){
        $scope.importLocation = $scope.importLocation || [];
        removeIfLocationExists(location);
        $scope.importLocation.push({
          subPath: location.subPath,
          branchId: location.branchId
        });
        resetLocationForm(location);
      };
    }
  ]);
});
