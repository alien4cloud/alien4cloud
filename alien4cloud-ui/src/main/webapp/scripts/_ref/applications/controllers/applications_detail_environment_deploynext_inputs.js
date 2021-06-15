define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/common/filters/inputs');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext_inputs_artifact_modal');

  states.state('applications.detail.environment.deploynext.inputs', {
    url: '/inputs',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_inputs.html',
    controller: 'AppEnvDeployNextInputsCtrl',
    resolve: {
      topologyDTO: ['deploymentTopologyDTO', 'topologyServices',
        function (deploymentTopologyDTO, topologyServices) {
          return _.catch(function () {
            return topologyServices.dao.get({ topologyId: deploymentTopologyDTO.topology.id })
            .$promise.then(function(result) {
              return result.data;
            });
          });
        }
      ]
    },
    menu: {
      id: 'applications.detail.environment.deploynext.inputs',
      state: 'applications.detail.environment.deploynext.inputs',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT_INPUTS',
      icon: '',
      priority: 300,
      step: {
        taskCodes: ['INPUT_PROPERTY', 'INPUT_ARTIFACT_INVALID', 'UNRESOLVABLE_PREDEFINED_INPUTS', 'MISSING_VARIABLES', 'PREDEFINED_INPUTS_CONSTRAINT_VIOLATION', 'PREDEFINED_INPUTS_TYPE_VIOLATION']
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextInputsCtrl',
    ['$scope', '$state', '$filter', '$resource', '$uibModal', 'deploymentTopologyServices', 'topologyServices', 'breadcrumbsService','$translate', 'topoEditSecrets', 'topoEditProperties', 'topologyDTO',
    function ($scope, $state, $filter, $resource, $uibModal, deploymentTopologyServices, topologyServices, breadcrumbsService, $translate, topoEditSecrets, topoEditProperties, topologyDTO) {

      topoEditSecrets($scope);
      topoEditProperties($scope);

      breadcrumbsService.putConfig({
        state : 'applications.detail.environment.deploynext.inputs',
        text: function(){
          return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_NEXT_INPUTS');
        },
        onClick: function(){
          $state.go('applications.detail.environment.deploynext.inputs');
        }
      });

      // Filter inputs to remove the internal inputs
      var allInputs = $filter('internalInputs')($scope.deploymentTopologyDTO.topology.inputs);
      $scope.deployerInputs = {};
      $scope.predefiniedInputs = {};
      _.each(allInputs, function (inputValue, inputId) {
        if (_.isUndefined(_.get($scope, 'deploymentTopologyDTO.topology.preconfiguredInputProperties[' + inputId + ']'))) {
          $scope.deployerInputs[inputId] = inputValue;
        } else {
          $scope.predefiniedInputs[inputId] = inputValue;
        }
      });

      /* ******************************************************
       *      Handle properties inputs
       **********************************************************/
      $scope.updateInputValue = function (definition, inputValue, inputId) {
        // No update if it's the same value
        // Cannot be done with complex object

        var updatedProperties = {};
        updatedProperties[inputId] = inputValue;
        return deploymentTopologyServices.updateInputProperties({
          appId: $scope.application.id,
          envId: $scope.environment.id
        }, angular.toJson({
          inputProperties: updatedProperties
        }), function (result) {
          if (!result.error) {
            $scope.updateScopeDeploymentTopologyDTO(result.data);
          }
        }).$promise;
      };

      /* ******************************************************
       *      Handle inputs artifacts
       **********************************************************/
      $scope.openInputArtifactModal = function (artifactKey) {
        var key = artifactKey;
        topologyServices.availableRepositories({
          topologyId: $scope.deploymentTopologyDTO.topology.id
        }, function (result) {
          $scope.availableRepositories = result.data;
          var modalInstance = $uibModal.open({
            templateUrl: 'views/applications/application_deployment_input_artifact_modal.html',
            controller: 'ApplicationInputArtifactModalCtrl',
            size: 'lg',
            resolve: {
              archiveContentTree: function () {
                return topologyDTO.archiveContentTree;
              },
              availableRepositories: function () {
                return $scope.availableRepositories;
              },
              artifact: function () {
                return $scope.deploymentTopologyDTO.topology.uploadedInputArtifacts[artifactKey];
              },
              application: function () {
                return $scope.application;
              },
              artifactKey: function () {
                return artifactKey;
              },
              environment: function() {
                return $scope.environment;
              },
              updateScopeDeploymentTopologyDTO: function () {
                return $scope.updateScopeDeploymentTopologyDTO;
              },
              topology: function () {
                return topologyDTO.topology;
              }
            }
          });

          modalInstance.result.then(function (selectedArtifact) {
            if (selectedArtifact) {
              var inputArtifactsDao = $resource('rest/latest/applications/' + $scope.application.id + '/environments/' + $scope.environment.id + '/deployment-topology/inputArtifacts/' + key + '/update', {}, {
                'update': {
                  method: 'POST'
                }
              });

              inputArtifactsDao.update({
                artifactType: selectedArtifact.artifactType,
                artifactName: selectedArtifact.artifactName,
                artifactRef: selectedArtifact.reference,
                artifactRepository: selectedArtifact.repository,
                archiveName: selectedArtifact.archiveName,
                archiveVersion: selectedArtifact.archiveVersion,
                repositoryURL: selectedArtifact.repositoryUrl,
                repositoryCredential: selectedArtifact.repositoryCredential,
                repositoryName: selectedArtifact.repositoryName
              }).$promise.
                then(response => {
                  if (!response.error) {
                    $scope.updateScopeDeploymentTopologyDTO(response.data);
                  }
                });
            }
          });
        }); // availableRepositories callback
      }; // openInputArtifactModal function

      $scope.saveInputSecret = function(secretPath, propertyValue, propertyName){
        propertyValue.parameters[0] = secretPath;
        $scope.updateInputValue(null, propertyValue, propertyName);
      };

      $scope.$emit('$contextPush',
          { type: "DeploymentInput",
            data: { topologyId: $scope.deploymentTopologyDTO.topology.id }
          });
      $scope.$on('$destroy', function() {
        $scope.$emit('$contextPoll');
      });
    }
  ]);
});
