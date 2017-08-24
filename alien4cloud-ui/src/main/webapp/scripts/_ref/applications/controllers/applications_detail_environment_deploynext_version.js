define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('applications.detail.environment.deploynext.version', {
    url: '/version',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_version.html',
    controller: 'AppEnvDeployNextVersionCtrl',

    menu: {
      id: 'applications.detail.environment.deploynext.version',
      state: 'applications.detail.environment.deploynext.version',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT.VERSION',
      icon: '',
      priority: 100,
      step: {
        taskCodes: []
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextVersionCtrl',
    ['$scope', 'archiveVersions', 'deploymentTopologyDTO','applicationEnvironmentServices','environment',
      function ($scope, archiveVersionsResponse, deploymentTopologyDTO, applicationEnvironmentServices, environment) {
        $scope.archive = {
          versions: archiveVersionsResponse.data
        };

        $scope.onVersionSelected = function ($event, version) {
          $scope.selectedAppVersion = version;
          $scope.selectedTopologyVersion = null;

          if (_.defined($event)) {
            $event.stopPropagation();
          }

          if (Object.keys(version.topologyVersions).length > 1) {
            $scope.dropdown = { 'style': { 'top': '-20px' } };
          } else {
            $scope.dropdown = { 'style': { 'top': '-4px' } };
          }
        };

        $scope.onTopologyVersionSelected = function ($event, topologyVersion) {
          $scope.selectedTopologyVersion = topologyVersion;

          if (_.defined($event)) {
            $event.stopPropagation();
          }

          // update server info
          $scope.setAppTopologyVersion(topologyVersion);
        };

        $scope.isSelectedVersion = function (version) {
          return version.id === _.get($scope, 'selectedAppVersion.id');
        };

        $scope.setAppTopologyVersion = function(selectedTopologyVersion) {
          applicationEnvironmentServices.getInputCandidates({
            applicationId: $scope.application.id
          }, angular.toJson({
            applicationEnvironmentId: environment.id,
            applicationTopologyVersion: selectedTopologyVersion.archiveId
          })).$promise.then(function(result) {
            console.log('result -> ' + result);
            console.log('result -> ' + JSON.stringify(result));
            /*
            if (_.defined(result.data) && result.data.length > 0) {
              var modalInstance = $uibModal.open({
                templateUrl: 'views/applications/select_environment_to_copy_inputs.html',
                controller: SelectEnvironmentToCopyInputCtrl,
                resolve: {
                  inputCandidates: function(){
                    return result.data;
                  }
                }
              });
              modalInstance.result.then(function(inputCandidate) {
                doUpdateTopologyVersion(environment, selectedTopologyVersion, inputCandidate);
              });
            } else {
              doUpdateTopologyVersion(environment, selectedTopologyVersion, null);
            }
            */
          });
        };

        function setSelectedTopologyId(topologyId) {
          _.forEach($scope.archive.versions, function (version) {
            if (_.defined(version.topologyVersions[topologyId])) {
              $scope.onVersionSelected(null, version);
              $scope.onTopologyVersionSelected(null, version.topologyVersions[topologyId]);
              return;
            }
          });
        }

        setSelectedTopologyId(deploymentTopologyDTO.topology.archiveVersion);
      }
    ]);
    
});
