define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/deployment/services/deployment_services');

  modules.get('a4c-applications').controller('DeploymentHistoryCtrl',
    ['$scope', '$state', 'deploymentServices', 'historyConf', 'searchServiceFactory',
      function ($scope, $state, deploymentServices, historyConf, searchServiceFactory) {
        $scope.now = new Date();

        var searchServiceUrl = 'rest/latest/deployments/search';
        $scope.queryManager = {
          query: ''
        };
        $scope.searchService = searchServiceFactory(searchServiceUrl, true, $scope.queryManager, 15, 50, true, null, historyConf.searchParam);
        $scope.searchService.search();
        $scope.queryManager.onSearchCompleted = function(searchResult) {
          $scope.deployments = searchResult.data.data;
        };

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
