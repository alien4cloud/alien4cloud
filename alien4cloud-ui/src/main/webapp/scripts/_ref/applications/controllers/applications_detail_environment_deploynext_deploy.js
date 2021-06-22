define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/services/directives/managed_service');
  require('scripts/applications/services/application_services');
  require('scripts/_ref/applications/services/secret_display_modal');

  states.state('applications.detail.environment.deploynext.deploy', {
    url: '/deploy',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_deploy.html',
    controller: 'AppEnvDeployNextDeployCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.deploy',
      state: 'applications.detail.environment.deploynext.deploy',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT_DEPLOY',
      icon: '',
      priority: 600,
      step: {
        taskCodes: ['NODE_FILTER_INVALID', 'ORCHESTRATOR_PROPERTY', 'PROPERTIES', 'SCALABLE_CAPABILITY_INVALID']
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextDeployCtrl',
    ['$scope', '$alresource', '$translate', 'toaster', 'deploymentTopologyServices', 'applicationServices', 'breadcrumbsService', '$state', 'secretDisplayModal',
      function ($scope, $alresource, $translate, toaster, deploymentTopologyServices, applicationServices, breadcrumbsService, $state, secretDisplayModal) {
        breadcrumbsService.putConfig({
          state: 'applications.detail.environment.deploynext.deploy',
          text: function () {
            return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_NEXT_DEPLOY');
          },
          onClick: function () {
            $state.go('applications.detail.environment.deploynext.deploy');
          }
        });

        $scope.doDeploy = function () {
          secretDisplayModal($scope.deploymentTopologyDTO.secretCredentialInfos).then(function (secretProviderInfo) {
            var deployApplicationRequest = {
              'applicationId': $scope.application.id,
              'applicationEnvironmentId': $scope.environment.id
            };
            if (_.defined(secretProviderInfo)) {
              deployApplicationRequest.secretProviderPluginName = secretProviderInfo.pluginName;
              deployApplicationRequest.secretProviderCredentials = secretProviderInfo.credentials;
            }
            $scope.setState('INIT_DEPLOYMENT');
            applicationServices.deployApplication.deploy([], angular.toJson(deployApplicationRequest), function () {
              $scope.environment.status = 'INIT_DEPLOYMENT';
              // the deployed version is the current one
              $scope.environment.deployedVersion = $scope.environment.currentVersionName;
              $scope.setEnvironment($scope.environment);
            }, function () {
              $scope.reloadEnvironment();
            });
          });
        };

        $scope.doUpdate = function () {
          secretDisplayModal($scope.deploymentTopologyDTO.secretCredentialInfos).then(function (secretProviderInfo) {
            var secretProviderCredentials = {};
            if (_.defined(secretProviderInfo)) {
              secretProviderCredentials.pluginName = secretProviderInfo.pluginName;
              secretProviderCredentials.credentials = secretProviderInfo.credentials;
            }
            $scope.setState('INIT_DEPLOYMENT');

            applicationServices.deploymentUpdate({
              applicationId: $scope.application.id,
              applicationEnvironmentId: $scope.environment.id
            }, angular.toJson(secretProviderCredentials), function (data) {
              if (data.error === null) {
                //$scope.environment.status = 'UPDATE_IN_PROGRESS';
                $scope.setEnvironment($scope.environment);
              } else {
                $scope.environment.status = 'UPDATE_FAILURE';
                $scope.setEnvironment($scope.environment);
                toaster.pop(
                  'error',
                  $translate.instant('DEPLOYMENT.STATUS.UPDATE_FAILURE'),
                  $translate.instant('DEPLOYMENT.TOASTER_STATUS.UPDATE_FAILURE', {
                    envName: $scope.environment.name,
                    appName: $scope.application.name
                  }),
                  0, 'trustedHtml', null
                );
              }
            }, function () {
              $scope.reloadEnvironment();
            });
          });
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
              if (_.undefined($scope.deploymentTopologyDTO.topology.providerDeploymentProperties)) {
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
        $scope.isUpdatable = function () {
          return _.includes(['DEPLOYED', 'UPDATED', 'UPDATE_FAILURE'], $scope.environment.status) &&
            _.definedPath($scope.deploymentTopologyDTO, 'locationPolicies._A4C_ALL') &&
            _.get($scope.deploymentTopologyDTO, 'locationPolicies._A4C_ALL') === _.get($scope.deployedTopology, 'topology.locationGroups._A4C_ALL.policies[0].locationId');
        };

        $scope.$watch('deploymentTopologyDTO.topology.orchestratorId', function (newValue) {
          if (_.undefined(newValue)) {
            return;
          }
          refreshOrchestratorDeploymentPropertyDefinitions();
        });

        $scope.$watch('environment.status', function (newValue) {
          if (_.undefined(newValue)) {
            return;
          }
          if (_.indexOf(['DEPLOYMENT_IN_PROGRESS', 'UNDEPLOYMENT_IN_PROGRESS', 'UPDATE_IN_PROGRESS'], newValue) !== -1 && $scope.lockNextDeploymentTabs == true) {
            $state.go('applications.detail.environment.deploycurrent');
          }
        });
      }
    ]);
});
