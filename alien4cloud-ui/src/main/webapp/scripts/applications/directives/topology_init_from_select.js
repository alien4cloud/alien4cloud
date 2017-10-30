define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/topologytemplates/directives/topology_template_search');

  modules.get('a4c-applications').directive('topologyInitFromSelect', ['topologyServices', function(topologyServices) {
    return {
      templateUrl: 'views/applications/directives/topology_init_from_select.html',
      restrict: 'E',
      link: function (scope) {
        scope.fromVersion = {};
        scope.topologyTemplate = {};
        scope.selectTemplate = function (topology) {
          scope.topologyTemplate.name = topology.archiveName;
          scope.topologyTemplate.version = topology.archiveVersion;
          scope.topologyTemplate.versionId = topology.id;
          // We have to check that the topology template we select
          // - do not need a recovery
          // - is in the global workspace as we don't manage potential promotions or dependencies if selecting a template from personnal workspace.
          topologyServices.dao.get({
            topologyId: topology.id
          }, function (result) {
            delete scope.selectedTopologyWarning;
            delete scope.selectedTopologyError;
            if (_.defined(result.error)) {
              switch (result.error.code) {
                case 860:
                  scope.selectedTopologyWarning = 860;
                  break;
                default:
                  scope.selectedTopologyError = result.error.code;
                  break;
              }
            } else if (result.data.topology.workspace !== 'ALIEN_GLOBAL_WORKSPACE') {
              scope.selectedTopologyError = 870;
            }
          });
        };

        scope.isSelected = function(topology){
          return _.isEqual(_.get(scope, 'topologyTemplate.versionId'), topology.id);
        };
      }
    };
  }]);
});
