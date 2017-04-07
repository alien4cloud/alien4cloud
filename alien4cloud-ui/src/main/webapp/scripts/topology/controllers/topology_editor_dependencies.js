/** Manage inputs in a topology edition. */
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor', ['pascalprecht.translate', 'toaster']).factory('topoEditDependencies',
    ['$translate', 'toaster', '$alresource', 'searchServiceFactory',
    function($translate, toaster, $alresource, searchServiceFactory) {
      var TopologyEditorMixin = function(scope) {
        var self = this;
        self.scope = scope;
        self.scope.$on('topologyRefreshedEvent', function() {
          self.renderDependencyConflicts(scope.topology.dependencyConflicts);
        });
      };

      var dependenciesQueryProvider = {
        query: '',
        onSearchCompleted: undefined
      };

      var searchService = searchServiceFactory('rest/latest/csars/search', false, dependenciesQueryProvider, 20, 10);
      searchService.filtered(true);

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,

        getVersionsForDependency: function(dependency) {
          var self = this;
          dependenciesQueryProvider.filters = { name: [dependency] };
          dependenciesQueryProvider.onSearchCompleted = function (searchResult) {
            if (_.undefined(searchResult.error)) {
              self.scope.currentVersionCandidatesForDependency = [];
              _.forEach(searchResult.data.data, function(csar) { self.scope.currentVersionCandidatesForDependency.push(csar.version); });
            }
          };
          self.scope.currentVersionCandidatesForDependency = [];
          searchService.search();
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
