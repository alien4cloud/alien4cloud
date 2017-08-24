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
    ['$scope', 'archiveVersions', 'deploymentTopologyDTO', 'applicationEnvironmentServices', 'environment', 'application',
      function ($scope, archiveVersionsResponse, deploymentTopologyDTO, applicationEnvironmentServices, environment, applicationResponse) {

        function setSelectedTopologyId(topologyId) {
          _.forEach($scope.archive.versions, function (version) {
            if (_.defined(version.topologyVersions[topologyId])) {
              $scope.onVersionSelected(null, version);
              $scope.onTopologyVersionSelected(null, version.topologyVersions[topologyId]);
              return;
            }
          });
        }

        function doUpdateTopologyVersion(applicationId, environmentId, selectedTopologyVersion) {
          applicationEnvironmentServices.updateTopologyVersion({
            applicationId: applicationId,
            applicationEnvironmentId: environmentId
          }, angular.toJson({
            newTopologyVersion: selectedTopologyVersion,
            environmentToCopyInput: null
          })).$promise.then(function () {
            // when OK
            deploymentTopologyDTO.topology.archiveVersion = selectedTopologyVersion;
          }).catch(function () {
            // when Error
            setSelectedTopologyId(deploymentTopologyDTO.topology.archiveVersion);
          });
        }

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

            // update server info
            var versionOnly = topologyVersion.archiveId.split(':')[1];
            doUpdateTopologyVersion(applicationResponse.data.id, environment.id, versionOnly);
          }
        };

        $scope.isSelectedVersion = function (version) {
          return version.id === _.get($scope, 'selectedAppVersion.id');
        };

        setSelectedTopologyId(deploymentTopologyDTO.topology.archiveVersion);
      }
    ]);

});
