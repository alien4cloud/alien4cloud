/** global Promise */
define(function(require) {
  'use strict';

  var modules = require('modules');

  require('scripts/orchestrators/directives/orchestrator_location_policy_template');
  require('scripts/orchestrators/services/common_location_resources_service');

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('OrchestratorLocationPoliciesTemplateCtrl',
    ['$scope', 'commonLocationResourcesService', 'locationPoliciesService', 'locationPoliciesPropertyService',
      function($scope, commonLocationResourcesService, locationPoliciesService, locationPoliciesPropertyService) {
        $scope.catalogType = 'POLICY_TYPE';

        $scope.resourceTemplateEditDisplayUrl = 'views/orchestrators/includes/location_policy_template_edit.html';

        commonLocationResourcesService($scope, 'policies', locationPoliciesService, locationPoliciesPropertyService);

      }
    ]);
});
