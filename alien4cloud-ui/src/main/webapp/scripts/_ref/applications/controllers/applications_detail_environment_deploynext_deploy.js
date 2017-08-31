define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('applications.detail.environment.deploynext.deploy', {
    url: '/deploy',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_deploy.html',
    controller: 'AppEnvDeployNextDeployCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.deploy',
      state: 'applications.detail.environment.deploynext.deploy',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT.DEPLOY',
      icon: '',
      priority: 600,
      step: {
        taskCodes: ['NODE_FILTER_INVALID', 'ORCHESTRATOR_PROPERTY', 'PROPERTIES', 'SCALABLE_CAPABILITY_INVALID']
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextDeployCtrl',
    ['$scope', '$alresource', '$translate', 'toaster', '$uibModal', 'deploymentTopologyServices', 'applicationServices',
    function ($scope, $alresource, $translate, toaster, $uibModal, deploymentTopologyServices, applicationServices) {
      //  CONFIRMATION BEFORE DEPLOYMENT / UPDATE
      var ConfirmationModalCtrl = ['$scope', '$uibModalInstance', '$translate', 'applicationName',
        function(modalScope, $uibModalInstance, $translate, applicationName) {
          modalScope.locationResources = $scope.deploymentTopologyDTO.topology.substitutedNodes;
          modalScope.application = applicationName;
          modalScope.environment = $scope.environment;
          modalScope.location = $scope.selectedLocation;
          modalScope.orchestrator = _.get(_.find($scope.locationMatches, {orchestrator: {id: modalScope.location.orchestratorId}}), 'orchestrator');

          modalScope.proceed = function (action) {
            $uibModalInstance.close(action);
          };
          modalScope.cancel = function () {
            $uibModalInstance.dismiss('canceled');
          };
        }
      ];

      function doDeploy() {
        var deployApplicationRequest = {
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.environment.id
        };
        $scope.setState('INIT_DEPLOYMENT');
        applicationServices.deployApplication.deploy([], angular.toJson(deployApplicationRequest), function() {
          $scope.environment.status = 'INIT_DEPLOYMENT';
          // the deployed version is the current one
          $scope.environment.deployedVersion = $scope.environment.currentVersionName;
          $scope.setEnvironment($scope.environment);
        }, function() {
          $scope.reloadEnvironment();
        });
      }

      function doUpdate() {
        $scope.setState('INIT_DEPLOYMENT');
        
        applicationServices.deploymentUpdate({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.environment.id
        }, undefined, function(data) {
          if (data.error === null) {
            $scope.environment.status = 'UPDATE_IN_PROGRESS';
            $scope.setEnvironment($scope.environment);
          } else {
            $scope.environment.status = 'UPDATE_FAILURE';
            $scope.setEnvironment($scope.environment);
            toaster.pop(
              'error',
              $translate.instant('DEPLOYMENT.STATUS.UPDATE_FAILURE'),
              $translate.instant('DEPLOYMENT.TOASTER_STATUS.UPDATE_FAILURE', {
                envName : $scope.environment.name,
                appName : $scope.application.name
              }),
              0, 'trustedHtml', null
            );
          }
        }, function() {
          $scope.reloadEnvironment();
        });
      }

      var openModal = function(templateUrl, modalType, proceedFunction) {
        var modalInstance = $uibModal.open({
          templateUrl: templateUrl,
          controller: ConfirmationModalCtrl,
          resolve: {
            applicationName: function() {
              return $scope.application.name;
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
        .get({orchestratorId: $scope.deploymentTopologyDTO.topology.orchestratorId}, function (result) {
          if (result.data) {
            $scope.orchestratorDeploymentPropertyDefinitions = result.data;
          }
        });
      }

      $scope.updateDeploymentProperty = function (propertyDefinition, propertyName, propertyValue) {
        if ((_.defined($scope.deploymentTopologyDTO.topology.providerDeploymentProperties) && propertyValue === $scope.deploymentTopologyDTO.topology.providerDeploymentProperties[propertyName]) || (_.undefined($scope.deploymentTopologyDTO.topology.providerDeploymentProperties) && _.undefined(propertyValue))) {
          return; // no change
        }
        var deploymentPropertyObject = {
          'definitionId': propertyName,
          'value': propertyValue
        };

        return applicationServices.checkProperty({
          orchestratorId: $scope.deploymentTopologyDTO.topology.orchestratorId
        }, angular.toJson(deploymentPropertyObject), function (data) {
          if (data.error === null) {
            if(_.undefined($scope.deploymentTopologyDTO.topology.providerDeploymentProperties)) {
              $scope.deploymentTopologyDTO.topology.providerDeploymentProperties = {};
            }
            $scope.deploymentTopologyDTO.topology.providerDeploymentProperties[propertyName] = propertyValue;
            // Update deployment setup when properties change
            deploymentTopologyServices.updateInputProperties({
                appId: $scope.application.id,
                envId: $scope.environment.id
              }, angular.toJson({
                providerDeploymentProperties: $scope.deploymentTopologyDTO.topology.providerDeploymentProperties
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
        return _.includes(['DEPLOYED', 'UPDATED'], $scope.environment.status) &&
               _.definedPath($scope.deploymentContext, 'deploymentTopologyDTO.locationPolicies._A4C_ALL') &&
               _.get($scope.deploymentContext, 'deploymentTopologyDTO.locationPolicies._A4C_ALL') === _.get($scope.deployedContext, 'dto.topology.locationGroups._A4C_ALL.policies[0].locationId');
      };

      $scope.$watch('deploymentTopologyDTO.topology.orchestratorId', function(newValue){
        if(_.undefined(newValue)){
          return;
        }
        refreshOrchestratorDeploymentPropertyDefinitions();
      });
    }
  ]);
});
