define(function (require) {
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
      priority: 100,
      step: {
        taskCodes: ['INPUT_PROPERTY', 'INPUT_ARTIFACT_INVALID']
      }
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentSetupCtrl',
      ['$scope', 'Upload', 'applicationServices', '$http', '$filter', 'deploymentTopologyServices', '$state',
        function ($scope, $upload, applicationServices, $http, $filter, deploymentTopologyServices) {

          $scope.isAllowedInputDeployment = function () {
            return _.isNotEmpty($filter('allowedInputs')(_.get($scope, 'deploymentContext.deploymentTopologyDTO.topology.inputs')));
          };

          /* Handle properties inputs */
          $scope.updateInputValue = function (definition, inputValue, inputId) {
            // No update if it's the same value
            if(_.get($scope.deploymentContext.deploymentTopologyDTO.topology.inputProperties[inputId], 'value') === inputValue){
              return;
            }
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

          // Artifact upload handler
          $scope.doUploadArtifact = function (file, artifactName) {
            if (_.undefined($scope.uploads)) {
              $scope.uploads = {};
            }
            $scope.uploads[artifactName] = {
              'isUploading': true,
              'type': 'info'
            };
            $upload.upload({
              url: 'rest/latest/applications/' + $scope.application.id + '/environments/' + $scope.deploymentContext.selectedEnvironment.id + '/deployment-topology/inputArtifacts/' + artifactName + '/upload',
              file: file
            }).progress(function (evt) {
              $scope.uploads[artifactName].uploadProgress = parseInt(100.0 * evt.loaded / evt.total);
            }).success(function (success) {
              $scope.uploads[artifactName].isUploading = false;
              $scope.uploads[artifactName].type = 'success';
              $scope.updateScopeDeploymentTopologyDTO(success.data);
            }).error(function (data, status) {
              $scope.uploads[artifactName].type = 'error';
              $scope.uploads[artifactName].error = {};
              $scope.uploads[artifactName].error.code = status;
              $scope.uploads[artifactName].error.message = 'An Error has occurred on the server!';
            });
          };

          $scope.onArtifactSelected = function ($files, artifactName) {
            var file = $files[0];
            $scope.doUploadArtifact(file, artifactName);
          };
        } // function
      ]); //controller
}); //Define
