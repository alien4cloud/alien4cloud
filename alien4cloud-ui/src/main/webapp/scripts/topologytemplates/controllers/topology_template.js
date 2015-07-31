// topology templates list is the entry point for browsing and managing global topologies in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/topologytemplates/services/topology_template_service');
  require('scripts/topology/controllers/topology');

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
      ]
    },
    controller: 'TopologyTemplateCtrl'
  });
  states.state('topologytemplates.detail.topology', {
    url: '/topology',
    templateUrl: 'views/topology/topology_editor.html',
    resolve: {
      topologyId: ['topologyTemplate',
        function(topologyTemplate) {
          return topologyTemplate.data.topologyId;
        }
      ],
      appVersions: function() {
        // TODO : handle versions for topology templates
        return null;
      }
    },
    controller: 'TopologyCtrl'
  });

  var NewTopologyTemplateCtrl = ['$scope', '$modalInstance',
    function($scope, $modalInstance) {
      $scope.topologytemplate = {};
      $scope.create = function(valid) {
        if (valid) {
          $modalInstance.close($scope.topologytemplate);
        }
      };
      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ];

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
          function() {
            // Success
          }, function(errorResponse) {
            // Error
            return $translate('ERRORS.' + errorResponse.data.error.code);
          }
        );
      };
    }
  ]); // controller
}); // define
