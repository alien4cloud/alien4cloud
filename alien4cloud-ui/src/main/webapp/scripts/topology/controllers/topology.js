// Topology editor controller
define(function (require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  require('d3');
  require('toaster');
  require('bower_components/jquery-ui/ui/resizable');

  require('scripts/common/directives/drag_drop');
  require('scripts/common/directives/property_display');
  require('scripts/common/directives/simple_modal');
  require('scripts/components/services/component_services');
  require('scripts/tosca/services/tosca_service');
  require('scripts/tosca/services/node_template_service');

  require('scripts/topology/controllers/topology_editor_artifacts');
  require('scripts/topology/controllers/topology_editor_display');
  require('scripts/topology/controllers/topology_editor_groups');
  require('scripts/topology/controllers/topology_editor_inputs');
  require('scripts/topology/controllers/topology_editor_nodes');
  require('scripts/topology/controllers/topology_editor_nodesswap');
  require('scripts/topology/controllers/topology_editor_outputs');
  require('scripts/topology/controllers/topology_editor_properties');
  require('scripts/topology/controllers/topology_editor_relationships');
  require('scripts/topology/controllers/topology_editor_substitution');
  require('scripts/topology/controllers/topology_editor_dependencies');

  require('scripts/topology/controllers/search_relationship');

  require('scripts/topology/services/topology_editor_events_services');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.bootstrap', 'a4c-tosca', 'a4c-styles']).controller('TopologyCtrl',
    ['$scope', '$modal', '$timeout', 'componentService', 'nodeTemplateService', 'toscaService',
    'defaultFilters',
    'topoEditArtifacts',
    'topoEditDisplay',
    'topoEditGroups',
    'topoEditInputs',
    'topoEditNodes',
    'topoEditNodesSwap',
    'topoEditOutputs',
    'topoEditProperties',
    'topoEditRelationships',
    'topoEditSubstitution',
    'topoEditDependencies',
    function($scope, $modal, $timeout, componentService, nodeTemplateService, toscaService,
    defaultFilters,
    topoEditArtifacts,
    topoEditDisplay,
    topoEditGroups,
    topoEditInputs,
    topoEditNodes,
    topoEditNodesSwap,
    topoEditOutputs,
    topoEditProperties,
    topoEditRelationships,
    topoEditSubstitution,
    topoEditDependencies) {
      // if there is workspaces in the scope application add them to the scope
      if(_.defined($scope.workspaces) && $scope.workspaces.length > 0) {
        if(_.undefined(defaultFilters)) {
          defaultFilters = {};
        }
        $scope.staticFacets = {workspace: []};
        _.each($scope.workspaces, function(workspace) {
          $scope.staticFacets.workspace.push({facetValue: workspace, count: ''});
        });
        $scope.staticFacets.workspace[0].staticFilter = $scope.workspaces;
        defaultFilters.workspace =  $scope.workspaces;
      }

      $scope.defaultFilters = defaultFilters;
      $scope.isRuntime = false;

      $scope.isNodeTemplateCollapsed = false;
      $scope.isPropertiesCollapsed = false;
      $scope.isRelationshipsCollapsed = false;
      $scope.isRelationshipCollapsed = false;
      $scope.isArtifactsCollapsed = false;
      $scope.isArtifactCollapsed = false;
      $scope.isRequirementsCollapsed = false;
      $scope.isCapabilitiesCollapsed = false;
      $scope.displays = {
        catalog: { active: true, size: 500, selector: '#catalog-box', only: ['topology', 'catalog'] },
        dependencies: { active: false, size: 400, selector: '#dependencies-box', only: ['topology', 'dependencies'] },
        inputs: { active: false, size: 400, selector: '#inputs-box', only: ['topology', 'inputs'], keep: ['nodetemplate'] },
        artifacts: { active: false, size: 400, selector: '#artifacts-box', only: ['topology', 'artifacts'], keep: ['nodetemplate'] },
        groups: { active: false, size: 400, selector: '#groups-box', only: ['topology', 'groups'], keep: ['nodetemplate'] },
        substitutions: { active: false, size: 400, selector: '#substitutions-box', only: ['topology', 'substitutions'], keep: ['nodetemplate'] },
        nodetemplate: { active: false, size: 500, selector: '#nodetemplate-box', only: ['topology', 'nodetemplate'], keep: ['inputs'] },
        workflows: { active: false, size: 400, selector: '#workflows-box', only:['workflows'] }
      };

      topoEditDisplay($scope, '#topology-editor');
      topoEditArtifacts($scope);
      topoEditGroups($scope);
      topoEditInputs($scope);
      topoEditNodes($scope);
      topoEditNodesSwap($scope);
      topoEditOutputs($scope);
      topoEditProperties($scope);
      topoEditRelationships($scope);
      topoEditSubstitution($scope);
      topoEditDependencies($scope);

      var refresh = function(selectedNodeTemplate) {
        if(_.undefined($scope.groupCollapsed)) { // we perform this only at init time.
          $scope.groupCollapsed = {};
          _.each($scope.topology.topology.groups, function(value, key) {
            $scope.groupCollapsed[key] = { main: false, members: true, policies: true };
          });
        }

        $scope.outputs.init($scope.topology.topology);
        // TODO trigger yaml update ?

        function reselectNodeTemplate(name) {
          $scope.selectedNodeTemplate = $scope.topology.topology.nodeTemplates[name];
          if(_.defined($scope.selectedNodeTemplate)) {
            $scope.selectedNodeTemplate.selected = true;
            fillNodeSelectionVars($scope.selectedNodeTemplate);
          } else {
            $scope.selectedNodeTemplate = null;
          }
        }

        if(_.defined(selectedNodeTemplate)) {
          reselectNodeTemplate(selectedNodeTemplate);
        } else if(_.defined($scope.selectedNodeTemplate)) {
          reselectNodeTemplate($scope.selectedNodeTemplate.name);
        } else {
          $scope.selectedNodeTemplate = null;
        }

        // we need an ordered nodeGroup array (on index property)
        $scope.orderedNodeGroups = [];
        angular.forEach($scope.topology.topology.groups, function(value) {
          $scope.orderedNodeGroups.push(value);
        });
        $scope.orderedNodeGroups.sort(function(a, b){
          return a.index - b.index;
        });

        $scope.substitution.refresh();
      };

      $scope.$on('topologyRefreshedEvent', function(event, param) {
        var selectedNodeTemplate = param.selectedNodeTemplate;
        refresh(selectedNodeTemplate);
      });

      if(_.defined($scope.topology)) {
        refresh();
      }

      $scope.checkMapSize = function(map) {
        return angular.isDefined(map) && map !== null && Object.keys(map).length > 0;
      };

      $scope.nodeNameObj = {}; // added to support <tabset> "childscope issue"
      function fillNodeSelectionVars(nodeTemplate) {
        $scope.selectedNodeTemplate = nodeTemplate;
        $scope.nodeNameObj.val = nodeTemplate.name;
        $scope.selectionabstract = $scope.topology.nodeTypes[nodeTemplate.type].abstract;
      }

      $scope.getIcon = toscaService.getIcon;
      // check if compute type
      $scope.isComputeType = function(nodeTemplate) {
        if (_.undefined($scope.topology) || _.undefined(nodeTemplate)) {
          return false;
        }
        return toscaService.isComputeType(nodeTemplate.type, $scope.topology.nodeTypes);
      };

      /**
       * Capabilities and Requirements docs
       */
       // FIXME this is actually to get Requirements and Capabilities but their version may not be the same as the actual node template
       // may be from different archives..
      $scope.getComponent = function(nodeTemplate, type) {
        var nodeType = $scope.topology.nodeTypes[nodeTemplate.type];
        if(_.defined($scope.topology) && _.defined($scope.topology.capabilityTypes) && _.defined($scope.topology.capabilityTypes[type])) {
          return $scope.topology.capabilityTypes[type];
        }
        var componentId = type + ':' + nodeType.archiveVersion;
        return componentService.get({
          componentId: componentId
        }, function(successResult) {
          if (successResult.error === null) {
            if (successResult.data !== null) {
              return successResult.data.description;
            }
          }
        });
      };

      $scope.editorCallback = {
        autoOpenRelationshipModal: $scope.relationships.autoOpenRelationshipModal,

        addRelationship: function(sourceId, requirementName, requirementType, targetId, capabilityName, relationship) {
          if(_.defined(relationship)) {
            // generate relationship name
            var name = toscaService.generateRelationshipName(relationship.elementId, targetId);
            // add the relationship
            $scope.relationships.doAddRelationship(sourceId, {
              name: name,
              target: targetId,
              targetedCapabilityName: capabilityName,
              relationship: relationship
            }, requirementName, requirementType);
          } else {
            // open the modal so user can choose the target relationship.
            $scope.relationships.openSearchRelationshipModal(sourceId, requirementName, targetId, capabilityName);
          }
        },
        selectNodeTemplate: function(newSelectedName) {
          $scope.display.set('nodetemplate', true);
          if (_.defined($scope.selectedNodeTemplate)) {
            var oldSelected = $scope.topology.topology.nodeTemplates[$scope.selectedNodeTemplate.name];
            if (oldSelected) {
              oldSelected.selected = false;
            }
          }

          var newSelected = $scope.topology.topology.nodeTemplates[newSelectedName];
          newSelected.selected = true;

          fillNodeSelectionVars(newSelected);
          $scope.triggerTopologyRefresh = {};
          $scope.$digest();
        }
      };
    }
  ]);
}); // define
