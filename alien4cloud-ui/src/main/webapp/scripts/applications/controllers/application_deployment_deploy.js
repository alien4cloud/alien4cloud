define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');


  require('scripts/applications/services/application_services');
  require('scripts/applications/services/deployment_context_utils');
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
    ['$scope', 'applicationServices', 'deploymentTopologyServices', '$alresource', '$uibModal', 'locationsMatchingServices', 'deploymentContextUtils',
      function($scope, applicationServices, deploymentTopologyServices, $alresource, $uibModal, locationsMatchingServices, deploymentContextUtils) {
        $scope._ = _;

        $scope.$watch('deploymentContext.deploymentTopologyDTO', function() {
          locationsMatchingServices.getLocationsMatches({topologyId: $scope.topologyId, environmentId: $scope.deploymentContext.selectedEnvironment.id}, function(result) {
            deploymentContextUtils.formatLocationMatches($scope, result.data);
            deploymentContextUtils.initSelectedLocation($scope);
          });
        });

        ////////////////////////////////////
        ///  CONFIRMATION BEFORE DEPLOYMENT
        ///
        var DeployConfirmationModalCtrl = ['$scope', '$uibModalInstance', '$translate', 'nodeTemplates', 'locationName', 'orchestratorName', 'environment',
          function($scope, $uibModalInstance, $translate, nodeTemplates, locationName, orchestratorName, environment) {
            $scope.nodeTemplates = nodeTemplates;
            $scope.content = $translate.instant('APPLICATIONS.DEPLOY_MODAL.CONTENT.HEADER', {
              'application': environment.applicationId,
              'type': environment.environmentType,
              'version': environment.currentVersionName
            });
            $scope.footer = $translate.instant('APPLICATIONS.DEPLOY_MODAL.CONTENT.FOOTER', {
              'location': locationName,
              'orchestrator': orchestratorName
            });

            $scope.deploy = function () {
              $uibModalInstance.close();
            };
            $scope.close = function () {
              $uibModalInstance.dismiss();
            };
          }
        ];

        function doDeploy() {
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
        }

        $scope.deploy = function() {
          var orchestratorName;
          _.each($scope.deploymentContext.locationMatches, function(location) {
            if (location.orchestrator.id === $scope.deploymentContext.selectedLocation.orchestratorId) {
              orchestratorName = location.orchestrator.name;
              return;
            }
          });

          var modalInstance = $uibModal.open({
            templateUrl: 'views/applications/deploy_confirm_modal.html',
            controller: DeployConfirmationModalCtrl,
            resolve: {
              nodeTemplates: function() {
                return $scope.deploymentContext.deploymentTopologyDTO.topology.substitutedNodes;
              },
              locationName: function() {
                return $scope.deploymentContext.selectedLocation.name;
              },
              orchestratorName: function() {
                return orchestratorName;
              },
              environment: function() {
                return $scope.deploymentContext.selectedEnvironment;
              }
            }
          });

          modalInstance.result.then(function() {
            doDeploy();
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
