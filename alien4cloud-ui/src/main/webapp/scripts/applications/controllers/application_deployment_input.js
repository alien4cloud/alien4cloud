define(function(require) {
  'use strict';

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
      priority: 300,
      step: {
        nextStepId: 'am.applications.detail.deployment.deploy',
        taskCodes: ['INPUT_PROPERTY', 'ORCHESTRATOR_PROPERTY', 'NODE_FILTER_INVALID', 'PROPERTIES', 'SCALABLE_CAPABILITY_INVALID']
      }
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentSetupCtrl',
    ['$scope', '$upload', 'applicationServices', '$http', '$filter', 'deploymentTopologyServices',
      function($scope, $upload, applicationServices, $http, $filter, deploymentTopologyServices) {

        $scope._=_;
        $scope.isAllowedInputDeployment = function() {
          return !_.isEmpty($filter('allowedInputs')($scope.deploymentContext.deploymentTopologyDTO.topology.inputs));
        };

        /* Handle properties inputs */
        $scope.updateInputValue = function(definition, inputValue, inputId) {
          // No update if it's the same value
          var updatedProperties = {};
          updatedProperties[inputId] = inputValue;
          return deploymentTopologyServices.updateInputProperties({
            appId: $scope.application.id,
            envId: $scope.deploymentContext.selectedEnvironment.id
          }, angular.toJson({
            inputProperties: updatedProperties
          }), function(result){
            if(!result.error) {
              $scope.updateScopeDeploymentTopologyDTO(result.data);
            }
          }).$promise;
        };

        // Artifact upload handler
        $scope.doUploadArtifact = function(file, artifactName) {
          if (_.undefined($scope.uploads)) {
            $scope.uploads = {};
          }
          $scope.uploads[artifactName] = {
            'isUploading': true,
            'type': 'info'
          };
          $upload.upload({
            url: 'rest/latest/topologies/' + $scope.topologyDTO.topology.id + '/inputArtifacts/' + artifactName + '/upload',
            file: file
          }).progress(function(evt) {
            $scope.uploads[artifactName].uploadProgress = parseInt(100.0 * evt.loaded / evt.total);
          }).success(function(success) {
            $scope.deploymentContext.deploymentTopologyDTO.topology.inputArtifacts[artifactName].artifactRef = success.data.topology.inputArtifacts[artifactName].artifactRef;
            $scope.deploymentContext.deploymentTopologyDTO.topology.inputArtifacts[artifactName].artifactName = success.data.topology.inputArtifacts[artifactName].artifactName;
            $scope.uploads[artifactName].isUploading = false;
            $scope.uploads[artifactName].type = 'success';
          }).error(function(data, status) {
            $scope.uploads[artifactName].type = 'error';
            $scope.uploads[artifactName].error = {};
            $scope.uploads[artifactName].error.code = status;
            $scope.uploads[artifactName].error.message = 'An Error has occurred on the server!';
          });
        };

        $scope.onArtifactSelected = function($files, artifactName) {
          var file = $files[0];
          $scope.doUploadArtifact(file, artifactName);
        };

        $scope.refreshOrchestratorDeploymentPropertyDefinitions = function() {
          return $http.get('rest/latest/orchestrators/' + $scope.deploymentContext.deploymentTopologyDTO.topology.orchestratorId + '/deployment-property-definitions').success(function(result) {
            if (result.data) {
              $scope.deploymentContext.orchestratorDeploymentPropertyDefinitions = result.data;
            }
          });
        };
        $scope.refreshOrchestratorDeploymentPropertyDefinitions();

        $scope.updateDeploymentProperty = function(propertyDefinition, propertyName, propertyValue) {
          if (propertyValue === $scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties[propertyName]) {
            return; // no change
          }
          var deploymentPropertyObject = {
            'definitionId': propertyName,
            'value': propertyValue
          };

          return applicationServices.checkProperty({
            orchestratorId: $scope.deploymentContext.deploymentTopologyDTO.topology.orchestratorId
          }, angular.toJson(deploymentPropertyObject), function(data) {
            if (data.error === null) {
              $scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties[propertyName] = propertyValue;
              // Update deployment setup when properties change
              deploymentTopologyServices.updateInputProperties({
                  appId: $scope.application.id,
                  envId: $scope.deploymentContext.selectedEnvironment.id
                }, angular.toJson({
                  providerDeploymentProperties: $scope.deploymentContext.deploymentTopologyDTO.topology.providerDeploymentProperties
                }), function(result){
                    if(!result.error) {
                      $scope.updateScopeDeploymentTopologyDTO(result.data);
                    }
                  }
                );
            }
          }).$promise;
        };
      }
    ]); //controller
}); //Define
