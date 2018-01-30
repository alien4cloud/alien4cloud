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
    ['$scope', 'applicationServices', 'deploymentTopologyServices', '$alresource', '$uibModal', 'locationsMatchingServices', 'deploymentContextUtils','toaster', '$translate',
      function($scope, applicationServices, deploymentTopologyServices, $alresource, $uibModal, locationsMatchingServices, deploymentContextUtils, toaster, $translate) {
        $scope._ = _;

        $scope.$watch('deploymentContext.deploymentTopologyDTO', function() {
          locationsMatchingServices.getLocationsMatches({topologyId: $scope.topologyId, environmentId: $scope.deploymentContext.selectedEnvironment.id}, function(result) {
            deploymentContextUtils.formatLocationMatches($scope, result.data);
            deploymentContextUtils.initSelectedLocation($scope);
          });
        });

        ////////////////////////////////////////////
        ///  CONFIRMATION BEFORE DEPLOYMENT / UPDATE
        ///
        var ConfirmationModalCtrl = ['$scope', '$uibModalInstance', '$translate', 'applicationName', 'deploymentContext',
          function($scope, $uibModalInstance, $translate, applicationName, deploymentContext) {
            $scope.locationResources = deploymentContext.deploymentTopologyDTO.topology.substitutedNodes;
            $scope.application = applicationName;
            $scope.environment = deploymentContext.selectedEnvironment;
            $scope.location = deploymentContext.selectedLocation;
            $scope.orchestrator = _.get(_.find(deploymentContext.locationMatches, {orchestrator: {id: deploymentContext.selectedLocation.orchestratorId}}), 'orchestrator');
            // location.orchestrator

            $scope.proceed = function (action) {
              $uibModalInstance.close(action);
            };
            $scope.cancel = function () {
              $uibModalInstance.dismiss('canceled');
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
            // the deployed version is the current one
            $scope.deploymentContext.selectedEnvironment.deployedVersion = $scope.deploymentContext.selectedEnvironment.currentVersionName;
            $scope.isDeploying = false;
          }, function() {
            $scope.isDeploying = false;
          });
        }

        function doUpdate() {
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
                0, 'trustedHtml', null
              );
            }
          }, function() {
            $scope.isDeploying = false;
          });
        }

        var openModal = function(templateUrl, modalType, proceedFunction) {
          var modalInstance = $uibModal.open({
            templateUrl: templateUrl,
            controller: ConfirmationModalCtrl,
            resolve: {
              applicationName: function() {
                return $scope.application.name;
              },
              deploymentContext: function() {
                return $scope.deploymentContext;
              }
            },
            size: 'lg'
          });

          modalInstance.result.then(function() {
            proceedFunction();
          });
        };

        $scope.deploy = function(){
          openModal('views/applications/deploy_confirm_modal.html', 'DEPLOY_MODAL', doDeploy);
        };

        $scope.update = function(){
          openModal('views/applications/update_confirm_modal.html', 'UPDATE_MODAL', doUpdate);
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
          if(_.defined($scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties) && propertyValue === $scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties[propertyName]) {
            console.log('defined and equals');
          }
          if(_.undefined($scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties) && _.undefined(propertyValue)) {
            console.log('both undefined');
          }
          if ((_.defined($scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties) && propertyValue === $scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties[propertyName]) || (_.undefined($scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties) && _.undefined(propertyValue))) {
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
              if(_.undefined($scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties)) {
                $scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties = {};
              }
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

        // the topology deployment is updatable if:
        // - the status is one of DEPLOYED , UPDATED,
        // - the current selectedlocation is the same as the one of the deployed topology
        $scope.isUpdatable = function() {
          return _.includes(['DEPLOYED', 'UPDATED', 'UPDATE_FAILURE'], $scope.deploymentContext.selectedEnvironment.status) &&
                 _.definedPath($scope.deploymentContext, 'deploymentTopologyDTO.locationPolicies._A4C_ALL') &&
                 _.get($scope.deploymentContext, 'deploymentTopologyDTO.locationPolicies._A4C_ALL') === _.get($scope.deployedContext, 'dto.topology.locationGroups._A4C_ALL.policies[0].locationId');
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
