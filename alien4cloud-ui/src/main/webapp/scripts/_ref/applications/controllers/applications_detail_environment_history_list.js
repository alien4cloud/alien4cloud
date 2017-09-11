define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');

  require('scripts/_ref/applications/controllers/applications_detail_environment_history_detail');

  states.state('applications.detail.environment.history.list', {
    url: '',
    templateUrl: 'views/_ref/applications/applications_detail_environment_history.html',
    controller: 'ApplicationEnvHistoryCtrl',
  });

  modules.get('a4c-applications').controller('ApplicationEnvHistoryCtrl',
    ['$scope', '$translate', '$state', 'deploymentServices',
      function ($scope, $translate, $state, deploymentServices) {

        $scope.now = new Date();

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

        deploymentServices.get({
          environmentId: $scope.environment.id,
          includeSourceSummary: false
        }, function (result) {
          processDeployments(result.data);
          $scope.deployments = result.data;
        });

        var goToDeploymentDetail = function(deployment) {
          $state.go('applications.detail.environment.history.detail', {
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
