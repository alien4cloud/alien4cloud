define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/user_context_services');

  states.state('applications.detail.version', {
    url: '/version/:versionId',
    templateUrl: 'views/_ref/applications/applications_detail_version.html',
    controller: 'ApplicationVersionCtrl',
    resolve: {
      version: ['archiveVersions', '$stateParams',
        function (archiveVersionsResult, $stateParams) {
          return _.catch(function () {
            var result;
            _.each(archiveVersionsResult.data, function(archiveVersion) {
              if(archiveVersion.id === $stateParams.versionId) {
                result = archiveVersion;
                return -1;
              }
            });
            return result;
          });
        }
      ]
    },
    params: {
      // optional id of the environment to automatically select when triggering this state
      environmentId: null
    }
  });

  modules.get('a4c-applications').controller('ApplicationVersionCtrl',
    ['$scope', '$state', 'breadcrumbsService', 'version',
      function ($scope, $state, breadcrumbsService, version) {
        $scope.version = version;
        
        breadcrumbsService.putConfig({
          state: 'applications.detail.version',
          text: function () {
            return version.version;
          },
          onClick: function () {
            $state.go('applications.detail.version', {
              id: $scope.application.id,
              environmentId: $scope.version.id
            });
          }
        });
      }
    ]);
});
