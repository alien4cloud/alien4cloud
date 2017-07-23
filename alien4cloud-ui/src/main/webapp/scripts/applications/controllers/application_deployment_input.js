define(function (require) {
  'use strict';

  require('scripts/applications/controllers/application_deployment_input_artifact_modal');

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  states.state('applications.detail.deployment.input', {
    url: '/input',
    templateUrl: 'views/applications/application_deployment_input.html',
    controller: 'ApplicationDeploymentSetupCtrl',
    menu: {
      id: 'am.applications.detail.deployment.input',
      state: 'applications.detail.deployment.input',
      key: 'APPLICATIONS.DEPLOYMENT.INPUT',
      icon: 'fa fa-sign-in',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'], // is deployer,
      priority: 100,
      step: {
        taskCodes: ['INPUT_PROPERTY', 'INPUT_ARTIFACT_INVALID']
      }
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentSetupCtrl',
      ['$scope', '$resource', 'Upload', 'applicationServices', '$http', '$filter', 'deploymentTopologyServices', '$uibModal', 'topologyServices', '$state',
        function ($scope, $resource, $upload, applicationServices, $http, $filter, deploymentTopologyServices, $uibModal, topologyServices) {

          $scope.isAllowedInputDeployment = function () {
            return _.isNotEmpty($filter('allowedInputs')(_.get($scope, 'deploymentContext.deploymentTopologyDTO.topology.inputs')));
          };

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
              envId: $scope.deploymentContext.selectedEnvironment.id
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
          $scope.openInputArtifactModal = function (artifactKey, artifact) {
            var key = artifactKey;
            topologyServices.availableRepositories({
                topologyId: $scope.topologyId
              }, function (result) {
                $scope.availableRepositories = result.data;
                var modalInstance = $uibModal.open({
                  templateUrl: 'views/applications/application_deployment_input_artifact_modal.html',
                  controller: 'ApplicationInputArtifactModalCtrl',
                  resolve: {
                    archiveContentTree: function () {
                      return $scope.topologyDTO.archiveContentTree;
                    },
                    availableRepositories: function () {
                      return $scope.availableRepositories;
                    },
                    artifact: function () {
                      return artifact;
                    },
                    application: function() {
                      return $scope.application;
                    },
                    deploymentContext: function(){
                      return $scope.deploymentContext;
                    },
                    artifactKey: function(){
                      return artifactKey;
                    },
                    updateScopeDeploymentTopologyDTO: function(){
                      return $scope.updateScopeDeploymentTopologyDTO;
                    },
                    topology: function(){
                      return $scope.topologyDTO.topology;
                    }

                  }
                });

                modalInstance.result.then(function (selectedArtifact) {
                  if(selectedArtifact){
                    var inputArtifactsDao = $resource('rest/latest/applications/' + $scope.application.id + '/environments/' + $scope.deploymentContext.selectedEnvironment.id + '/deployment-topology/inputArtifacts/' + key + '/update', {}, {
                      'update': {
                        method: 'POST'
                      }
                    });

                    inputArtifactsDao.update({
                      artifactType: selectedArtifact.artifactType,
                      artifactName   : selectedArtifact.artifactName,
                      artifactRef: selectedArtifact.reference,
                      artifactRepository : selectedArtifact.repository,
                      archiveName: selectedArtifact.archiveName,
                      archiveVersion: selectedArtifact.archiveVersion,
                      repositoryURL: selectedArtifact.repositoryUrl,
                      repositoryCredential: selectedArtifact.repositoryCredential,
                      repositoryName: selectedArtifact.repositoryName
                    }).$promise.
                    then(response => {
                      if(!response.error){
                        $scope.updateScopeDeploymentTopologyDTO(response.data);
                      }
                    });
                  }
              });
            });
          };
        } // function
      ]); //controller
}); //Define
