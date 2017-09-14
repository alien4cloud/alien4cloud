define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/deployment/services/deployment_services');

  modules.get('a4c-applications').controller('DeploymentHistoryCtrl',
    ['$scope', '$state', 'deploymentServices', 'historyConf',
      function ($scope, $state, deploymentServices, historyConf) {

        function processDeployments(deployments) {
          if (_.defined(deployments)) {
            _.each(deployments, function (deployment) {
              if(_.defined(deployment.deployment.endDate)){
                deployment.deployment.duration = '';
              }else{
                deployment.deployment.duration = '';
              }
            });
          }
        }

        $scope.now = new Date();

        deploymentServices.get(historyConf.searchParam, function (result) {
          processDeployments(result.data);
          $scope.deployments = result.data;
        });

        var goToDeploymentDetail = function(deployment) {
          $state.go(historyConf.rootState+'.detail', {
            deploymentId: deployment.deployment.id,
            deploymentDTO: deployment
          });
        };

        var goToCurrentDeploymentTab = function(deployment){
          $state.go('applications.detail.environment.deploycurrent', {
            id: deployment.source.id,
            environmentId: deployment.deployment.environmentId
          });
        };

        $scope.handleHistoryClick = function(deployment) {
          if(_.defined(deployment.deployment.endDate)){
            goToDeploymentDetail(deployment);
          }else{
            goToCurrentDeploymentTab(deployment);
          }
        };
      }
    ]);
});
