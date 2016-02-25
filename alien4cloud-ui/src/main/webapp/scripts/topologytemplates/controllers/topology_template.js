// topology templates list is the entry point for browsing and managing global topologies in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/topologytemplates/services/topology_template_service');
  require('scripts/topologytemplates/services/topology_template_version_services');
  require('scripts/topology/controllers/topology');
  require('scripts/applications/controllers/application_versions');

  // register components root state
  states.state('topologytemplates.detail', {
    url: '/details/:id',
    templateUrl: 'views/topologytemplates/topology_template.html',
    resolve: {
      topologyTemplate: ['topologyTemplateService', '$stateParams',
        function(topologyTemplateService, $stateParams) {
          return topologyTemplateService.get({
            topologyTemplateId: $stateParams.id
          }).$promise;
        }
      ],
      appVersions: ['$http', 'topologyTemplate', 'topologyTemplateVersionServices',
        function($http, topologyTemplate, topologyTemplateVersionServices) {
          var searchAppVersionRequestObject = {
            'from': 0,
            'size': 400
          };
          return topologyTemplateVersionServices.searchVersion({
            delegateId: topologyTemplate.data.id
          }, angular.toJson(searchAppVersionRequestObject)).$promise.then(function(result) {
              return result.data;
            });
        }
      ]
    },
    controller: 'TopologyTemplateCtrl'
  });
  states.state('topologytemplates.detail.topology', {
    url: '/topology/:version',
    templateUrl: 'views/topology/topology_editor.html',
    resolve: {
      topologyId: ['topologyTemplate',
        function(topologyTemplate) {
          return topologyTemplate.data.topologyId;
        }
      ],
      preselectedVersion: ['$stateParams', function($stateParams) {
        if(_.isEmpty($stateParams.version)) {
          return undefined;
        }
        return $stateParams.version;
      }]
    },
    controller: 'TopologyCtrl'
  });
  states.state('topologytemplates.detail.versions', {
    url: '/versions',
    templateUrl: 'views/applications/application_versions.html',
    resolve: {
      versionServices: ['topologyTemplateVersionServices', function(topologyTemplateVersionServices) { return topologyTemplateVersionServices; }],
      searchServiceUrl: ['topologyTemplate', function(topologyTemplate) { return 'rest/templates/' + topologyTemplate.data.id + '/versions/search'; }],
      delegateId: ['topologyTemplate', function(topologyTemplate) { return topologyTemplate.data.id; }],
      userCanModify: ['authService', function(authService) { return authService.hasRole('ARCHITECT'); }]
    },
    controller: 'ApplicationVersionsCtrl'
  });

  modules.get('a4c-topology-templates', ['a4c-common', 'ui.bootstrap', 'pascalprecht.translate']).controller('TopologyTemplateCtrl',
    ['$scope', 'topologyTemplate', 'topologyTemplateService', '$translate',
    function($scope, topologyTemplateResult, topologyTemplateService, $translate) {
      $scope.topologyTemplate = topologyTemplateResult.data;
      $scope.topologyId = $scope.topologyTemplate.topologyId;
      $scope.topologyTemplateId = $scope.topologyTemplate.id;

      $scope.updateTopologyTemplate = function(fieldName, fieldValue) {
        var topologyTemplateUpdateRequest = {};
        topologyTemplateUpdateRequest[fieldName] = fieldValue;
        return topologyTemplateService.put({
          topologyTemplateId: $scope.topologyTemplateId
        }, angular.toJson(topologyTemplateUpdateRequest), undefined).$promise.then(
          function() {}, // Success
          function(errorResponse) { // Error
            return $translate('ERRORS.' + errorResponse.data.error.code);
          }
        );
      };

      $scope.menu = [{
        id: 'am.topologytemplate.detail.topology',
        state: 'topologytemplates.detail.topology',
        key: 'NAVAPPLICATIONS.MENU_TOPOLOGY',
        icon: 'fa fa-sitemap',
        show: true
      }, {
        id: 'am.topologytemplate.detail.versions',
        state: 'topologytemplates.detail.versions',
        key: 'NAVAPPLICATIONS.MENU_VERSIONS',
        icon: 'fa fa-tasks',
        show: true
      }];

    }
  ]); // controller
}); // define
