define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-components', ['ui.bootstrap']).controller('CsarGitCrudController', ['$scope', '$uibModalInstance', 'gitRepository',
    function($scope, $uibModalInstance, gitRepository) {
      $scope.gitRepository = _.cloneDeep(gitRepository);

      $scope.create = function(gitRepo) {
        $uibModalInstance.close(gitRepo);
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
        $scope.id = 0;
        $scope.gitRepository = undefined;
      };

      $scope.removeCsarLocation = function(index){
        $scope.gitRepository.importLocations.splice(index, 1);
      };

      $scope.validLocation = function(location) {
        if (_.undefined(location) || _.isEmpty(location.branchId)){
          return false;
        }

        //check it doesn't already exist
        if(_.definedPath($scope, 'gitRepository.importLocations')){
          return _.undefined(_.find($scope.gitRepository.importLocations,
            function(loc){
              return loc.branchId === location.branchId && (loc.subPath||'') === (location.subPath||'');
            }));
        }else{
          return true;
        }
      };

      var resetLocationForm=function(location){
        location.subPath = '';
        location.branchId = '';
      };

      $scope.addLocation=function(location){
        $scope.gitRepository.importLocations = $scope.gitRepository.importLocations || [];
        $scope.gitRepository.importLocations.push({
          subPath: location.subPath,
          branchId: location.branchId
        });
        resetLocationForm(location);
      };
    }
  ]);
});
