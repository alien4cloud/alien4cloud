define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/applications/services/application_services');

  states.state('applications.detail.deployment.deploy', {
    url: '/trigger',
    templateUrl: 'views/applications/application_deployment_deploy.html',
    controller: 'ApplicationDeploymentTriggerCtrl',
    menu: {
      id: 'am.applications.detail.deployment.deploy',
      state: 'applications.detail.deployment.deploy',
      key: 'APPLICATIONS.DEPLOYMENT.DEPLOY',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'], // is deployer
      priority: 400,
      step: {
        taskCodes: []
      }
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentTriggerCtrl',
    ['$scope', 'applicationServices',
      function($scope, applicationServices) {
        // Deployment handler
        $scope.deploy = function() {
          // Application details with deployment properties
          var deployApplicationRequest = {
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.deploymentContext.selectedEnvironment.id
          };
          $scope.isDeploying = true;
          applicationServices.deployApplication.deploy([], angular.toJson(deployApplicationRequest), function() {
            $scope.deploymentContext.selectedEnvironment.status = 'INIT_DEPLOYMENT';
            $scope.isDeploying = false;
          }, function() {
            $scope.isDeploying = false;
          });
        };
      }
    ]); //controller
}); //Define
