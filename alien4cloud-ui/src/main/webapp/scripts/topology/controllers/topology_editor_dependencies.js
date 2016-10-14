/** Manage inputs in a topology edition. */
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor', ['pascalprecht.translate', 'toaster']).factory('topoEditDependencies',
    ['$translate', 'toaster', '$alresource', 'searchServiceFactory', 
    function($translate, toaster, $alresource, searchServiceFactory) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      var dependencieQueryProvider = {
        query: '',
        onSearchCompleted: undefined
      };

      var searchService = searchServiceFactory('rest/latest/csars/search', false, dependencieQueryProvider, 20, 10);
      searchService.filtered(true);

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,

        getVersionsForDependency: function(dependency) {
          var self = this;
          dependencieQueryProvider.filters = { "name": [dependency] };
          dependencieQueryProvider.onSearchCompleted = function (searchResult) {
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
          }, function(result) {
            if (!result.error) {
              callback();
            }
          });          
        }

      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.dependencies = instance;
      };
    }
  ]); // modules
}); // define
