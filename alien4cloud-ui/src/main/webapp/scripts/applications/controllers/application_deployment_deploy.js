define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/applications/services/application_services');
  require('scripts/services/directives/managed_service');

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
        taskCodes: ['NODE_FILTER_INVALID', 'ORCHESTRATOR_PROPERTY','PROPERTIES', 'SCALABLE_CAPABILITY_INVALID']
      }
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentTriggerCtrl',
    ['$scope', 'applicationServices', 'deploymentTopologyServices', '$alresource', 'toaster', '$translate',
      function($scope, applicationServices, deploymentTopologyServices, $alresource, toaster, $translate) {
        $scope._ = _;
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

        $scope.update_deployment = function() {
          $scope.isDeploying = true;
          applicationServices.deploymentUpdate({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.deploymentContext.selectedEnvironment.id
          }, undefined, function(data) {
            if (data.error === null) {
              $scope.deploymentContext.selectedEnvironment.status = 'UPDATE_IN_PROGRESS';
              $scope.isDeploying = false;
            } else {
              $scope.deploymentContext.selectedEnvironment.status = 'UPDATE_FAILURE';
              $scope.isDeploying = false;
              toaster.pop(
                'error',
                $translate.instant('DEPLOYMENT.STATUS.UPDATE_FAILURE'),
                $translate.instant('DEPLOYMENT.TOASTER_STATUS.UPDATE_FAILURE', {
                  envName : $scope.deploymentContext.selectedEnvironment.name,
                  appName : $scope.application.name
                }),
                6000, 'trustedHtml', null
              );
            }
          }, function() {
            $scope.isDeploying = false;
          });
        };

        /**
        * DEPLOYMENT PROPERTIES
        **/
        function refreshOrchestratorDeploymentPropertyDefinitions() {
          return $alresource('rest/latest/orchestrators/:orchestratorId/deployment-property-definitions')
          .get({orchestratorId: $scope.deploymentContext.deploymentTopologyDTO.topology.orchestratorId}, function (result) {
            if (result.data) {
              $scope.deploymentContext.orchestratorDeploymentPropertyDefinitions = result.data;
            }
          });
        }

        $scope.updateDeploymentProperty = function (propertyDefinition, propertyName, propertyValue) {
          if (propertyValue === $scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties[propertyName]) {
            return; // no change
          }
          var deploymentPropertyObject = {
            'definitionId': propertyName,
            'value': propertyValue
          };

          return applicationServices.checkProperty({
            orchestratorId: $scope.deploymentContext.deploymentTopologyDTO.topology.orchestratorId
          }, angular.toJson(deploymentPropertyObject), function (data) {
            if (data.error === null) {
              $scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties[propertyName] = propertyValue;
              // Update deployment setup when properties change
              deploymentTopologyServices.updateInputProperties({
                  appId: $scope.application.id,
                  envId: $scope.deploymentContext.selectedEnvironment.id
                }, angular.toJson({
                  providerDeploymentProperties: $scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties
                }), function (result) {
                  if (!result.error) {
                    $scope.updateScopeDeploymentTopologyDTO(result.data);
                  }
                }
              );
            }
          }).$promise;
        };

        $scope.$watch('deploymentContext.deploymentTopologyDTO.topology.orchestratorId', function(newValue){
          if(_.undefined(newValue)){
            return;
          }
          refreshOrchestratorDeploymentPropertyDefinitions();
        });
      } //function
    ]); //controller
}); //Define
