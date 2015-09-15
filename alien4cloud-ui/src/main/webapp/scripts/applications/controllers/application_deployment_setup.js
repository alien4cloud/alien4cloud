define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  states.state('applications.detail.deployment.setup', {
    url: '/setup',
    templateUrl: 'views/applications/application_deployment_setup.html',
    controller: 'ApplicationDeploymentSetupCtrl',
    menu: {
      id: 'am.applications.detail.deployment.setup',
      state: 'applications.detail.deployment.setup',
      key: 'APPLICATIONS.DEPLOYMENT.SETUP',
//      icon: 'fa fa-cloud-upload',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'], // is deployer,
      priority: 300
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentSetupCtrl',
    ['$scope', 'authService', '$upload', 'applicationServices', 'toscaService', '$resource', '$http', '$translate', 'application', '$state', 'applicationEnvironmentServices', 'appEnvironments', 'toaster', '$filter', 'menu',
    function($scope, authService, $upload, applicationServices, toscaService, $resource, $http, $translate, applicationResult, $state, applicationEnvironmentServices, appEnvironments, toaster, $filter, menu) {
      
      $scope.isAllowedInputDeployment = function() {
        return ! _.isEmpty($filter('allowedInputs')($scope.inputs));
      };
      
      // update the deployment configuration for the given environment.
      function refreshDeploymentSetup() {
        applicationServices.getDeploymentSetup({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, undefined, function(response) {
          $scope.setup = response.data;
          // update configuration of the PaaSProvider associated with the deployment setup
          $scope.deploymentProperties = $scope.setup.providerDeploymentProperties;
//          refreshSelectedCloud();
//          checkGroupZonesAssociation();
        });
      }
      
      /* Handle properties inputs */
      $scope.updateInputValue = function(definition, inputValue, inputId) {
        // No update if it's the same value
        if (_.undefined($scope.setup.inputProperties)) {
          $scope.setup.inputProperties = {};
        }
        if (inputValue === $scope.setup.inputProperties[inputId]) {
          return;
        } else {
          $scope.setup.inputProperties[inputId] = inputValue;
        }
        return applicationServices.updateDeploymentSetup({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, angular.toJson({
          inputProperties: $scope.setup.inputProperties
        }), function() {
          refreshDeploymentSetup();
//          $scope.checkTopology();
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
          url: 'rest/topologies/' + $scope.topologyDTO.topology.id + '/inputArtifacts/' + artifactName + '/upload',
          file: file
        }).progress(function(evt) {
          $scope.uploads[artifactName].uploadProgress = parseInt(100.0 * evt.loaded / evt.total);
        }).success(function(success) {
          $scope.inputArtifacts[artifactName].artifactRef = success.data.topology.inputArtifacts[artifactName].artifactRef;
          $scope.inputArtifacts[artifactName].artifactName = success.data.topology.inputArtifacts[artifactName].artifactName;
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
      
      
      
      
      /**
       ****************** ON FIRST LAUNCH *********************
       */
      
      refreshDeploymentSetup();
    }
  ]); //controller
}); //Define
