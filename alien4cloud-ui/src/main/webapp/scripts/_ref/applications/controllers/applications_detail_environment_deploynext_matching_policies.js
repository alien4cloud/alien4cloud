define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/_ref/applications/directives/resources_matching_directive');
  require('scripts/applications/services/policies_matching_services.js');

  states.state('applications.detail.environment.deploynext.matching.policies', {
    url: '/policies',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_matching_policies.html',
    controller: 'AppEnvDeployNextPoliciesMatchingCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.matching.policies',
      state: 'applications.detail.environment.deploynext.matching.policies',
      key: 'APPLICATIONS.DEPLOYMENT.MATCHING.POLICIES_TITLE',
      icon: '',
      priority: 100,
      step: {
        taskCodes: ['NO_NODE_MATCHES', 'NODE_NOT_SUBSTITUTED', 'IMPLEMENT', 'REPLACE']
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextPoliciesMatchingCtrl',
    ['$scope', 'policiesMatchingService',
    function ($scope, policiesMatchingService) {

      $scope.serviceContext = {
        service: policiesMatchingService,
        successCallback: $scope.updateScopeDeploymentTopologyDTO
      };

    }
  ]);
});
