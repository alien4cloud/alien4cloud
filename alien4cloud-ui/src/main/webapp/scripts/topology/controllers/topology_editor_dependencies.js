/** Manage dependencies in a topology edition. */
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');
  require('scripts/common/directives/pagination');

  modules.get('a4c-topology-editor', ['pascalprecht.translate', 'toaster']).factory('topoEditDependencies',
    ['$translate', 'toaster', '$uibModal', '$alresource', 'searchServiceFactory',
    function($translate, toaster, $uibModal, $alresource, searchServiceFactory) {
      var TopologyEditorMixin = function(scope) {
        var self = this;
        self.scope = scope;
        self.scope.$on('topologyRefreshedEvent', function() {
          self.renderDependencyConflicts(scope.topology.dependencyConflicts);
        });

        self.scope.dependenciesQueryProvider = {
          query: '',
          onSearchCompleted: undefined
        };
        self.scope.searchService = searchServiceFactory('rest/latest/csars/search', false, self.scope.dependenciesQueryProvider, 10, 20);
        self.scope.searchService.filtered(true);
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,

        getVersionsForDependency: function(dependency) {
          var self = this;
          self.scope.dependenciesQueryProvider.filters = { name: [dependency] };
          self.scope.dependenciesQueryProvider.onSearchCompleted = function (searchResult) {
            if (_.undefined(searchResult.error)) {
              self.scope.currentVersionCandidatesForDependency = [];
              _.forEach(searchResult.data.data, function(csar) { self.scope.currentVersionCandidatesForDependency.push(csar.version); });
            }
          };
          self.scope.currentVersionCandidatesForDependency = [];
          self.scope.searchService.search();
        },

        changeDependencyVersion: function(dependencyName, dependencyVersion, newVersion) {
          if (dependencyVersion === newVersion) {
            return;
          }
          this.scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.ChangeDependencyVersionOperation',
            dependencyName: dependencyName,
            dependencyVersion: newVersion
          });
        },

        // Display a modal to able version search when many results
        displayMoreVersions: function (dependency) {
          var scope = this.scope;
          scope.dependency = dependency;
          var modalInstance = $uibModal.open({
            templateUrl: 'views/topology/search_dependencies_versions_modal.html',
            controller: 'SearchDependenciesVersions',
            windowClass: 'dialog',
            scope: scope,
            size: 'dialog'
          });
          modalInstance.result.then(function (selectedVersion) {
            var modalConfirm = $uibModal.open({
              templateUrl: 'views/common/confirm_modal.html',
              controller: 'ConfirmModalCtrl',
              resolve: {
                title: function() {
                  return $translate('APPLICATIONS.TOPOLOGY.DEPENDENCIES.CHANGE_VERSION.TITLE', {name: scope.dependency.name});
                },
                content: function() {
                  return $translate('APPLICATIONS.TOPOLOGY.DEPENDENCIES.CHANGE_VERSION.CONFIRM');
                }
              }
            });
            modalConfirm.result.then(function () {
              scope.dependencies.changeDependencyVersion(scope.dependency.name, scope.dependency.version, selectedVersion);
            });
          });
        },

        renderDependencyConflicts: function(conflicts) {
          var self = this;
          if (conflicts) {
            self.renderedConflicts = _.groupBy(conflicts, function(item) {
              return item.dependency.split(':')[0];
            });
          }
        },

        renderedConflicts: {}

      };

      return function(scope) {
        scope.dependencies = new TopologyEditorMixin(scope);
      };
    }
  ]); // modules
}); // define
