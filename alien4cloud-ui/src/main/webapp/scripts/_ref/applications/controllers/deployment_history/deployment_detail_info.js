define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-applications').controller('DeploymentDetailInfoCtrl',
    ['$scope', 'deploymentDTO',
      function ($scope, deploymentDTO) {
        $scope.deploymentDTO = deploymentDTO.data;
      }
    ]);
});
