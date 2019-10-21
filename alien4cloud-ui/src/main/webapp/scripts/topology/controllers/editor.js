/**
* Main controller for topology edition.
* It loads topology and manage common server communication and topology refresh through common parent scope.
*/
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/topology/controllers/topology');
  require('scripts/topology/controllers/editor_browser');
  require('scripts/topology/controllers/editor_workflow');
  require('scripts/topology/controllers/editor_history');
  require('scripts/topology/controllers/editor_git_modal');
  require('scripts/topology/controllers/editor_validation');
  require('scripts/topology/controllers/editor_inputs_variables');

  require('scripts/tosca/services/tosca_cardinalities_service');
  require('scripts/topology/services/topology_json_processor');
  require('scripts/topology/services/topology_recovery_service');
  require('scripts/topology/services/topology_services');

  // manage websockets for topology editor
  // require('scripts/topology/services/topology_editor_events_services');
  require('scripts/common/directives/parsing_errors');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.bootstrap', 'a4c-tosca', 'a4c-styles', 'cfp.hotkeys']).controller('TopologyEditorCtrl',
    ['$scope', '$state', '$stateParams', '$alresource', '$uibModal', '$translate', 'toaster', 'hotkeys', 'menu', 'topologyServices', 'topologyJsonProcessor', 'toscaService', 'toscaCardinalitiesService', 'topologyRecoveryServices',
    function($scope, $state, $stateParams, $alresource, $uibModal, $translate, toaster, hotkeys, menu,  topologyServices, topologyJsonProcessor, toscaService, toscaCardinalitiesService, topologyRecoveryServices) {
      // This controller acts as a specific layout for the topology edition.
      $scope.menu = menu;
      $scope.topologyId = $stateParams.archiveId;

      $scope.getShortName = toscaService.simpleName;
      // this allow to avoid file edition in the ui-ace.
      $scope.released = false;

      // Initial load of the topology
      topologyServices.dao.get({ topologyId: $scope.topologyId },
        function(result) {
          if (_.undefined(result.error)) {
            $scope.refreshTopology(result.data, null, true);
            return;
          }
          // case there actually is an error
          var lastOperationId = 'null';
          if (_.undefined($scope.topology) && _.defined(result.data.lastOperationId)) {
            lastOperationId = result.data.lastOperationId;
          } else {
            lastOperationId = $scope.getLastOperationId(true);
          }
          topologyRecoveryServices.handleTopologyRecovery(result.data, $scope.topologyId, lastOperationId).then(function(recoveryResult){
            if (_.definedPath(recoveryResult, 'data')) {
              $scope.refreshTopology(recoveryResult.data, null, true);
              return;
            }
          });
        });

      /**
      * Add bounds information to the requirements and capabilities in the topology based on relationships.
      */
      function fillBounds(topology) {
        _.each(topology.nodeTemplates, function(nodeTemplate) {
          toscaCardinalitiesService.fillRequirementBounds($scope.topology.nodeTypes, nodeTemplate);
          toscaCardinalitiesService.fillCapabilityBounds($scope.topology.nodeTypes, $scope.topology.topology.nodeTemplates, nodeTemplate);
        });
      }

      // Version selection management is below, find here topology update handling
      /**
      * refreshTopology has to be triggered when the topology is updated.
      * Added to the scope as right now every operation returns the full and update topology.
      */
      $scope.refreshTopology = function(topologyDTO, selectedNodeTemplateName, initial) {
        $scope.topology = topologyDTO;
        if(topologyDTO.topology.workspace === 'ALIEN_GLOBAL_WORKSPACE') {
          $scope.workspaces = ['ALIEN_GLOBAL_WORKSPACE'];
        } else {
          $scope.workspaces = [topologyDTO.topology.workspace, 'ALIEN_GLOBAL_WORKSPACE'];
        }
        // Process the topology to enrich it with some additional data
        _.each(topologyDTO.topology.nodeTemplates, function(value, key){
          value.name = key;
        });
        // enrich objects to add maps for the fields that are currently mapped as array of map entries.
        topologyJsonProcessor.process($scope.topology);
        fillBounds($scope.topology.topology);
        _.each($scope.topology.topology.inputs, function(value, key){
          value.inputId = key;
        });

        // trigger refresh event so child scope can update what they need. Initial flag allows to know if this is the initial loading of the topology.
        $scope.$broadcast('topologyRefreshedEvent', {
          topology: topologyDTO,
          initial: initial,
          selectedNodeTemplateName: selectedNodeTemplateName
        });
      };

      $scope.getLastOperationId = function(nullAsString) {
        if(_.get($scope.topology, 'lastOperationIndex')>= 0) {
          return $scope.topology.operations[$scope.topology.lastOperationIndex].id;
        }
        return _.defined(nullAsString) && nullAsString ? 'null' : null;
      };

      var editorResource = $alresource('rest/latest/editor/:topologyId/execute');
      $scope.execute = function(operation, successCallback, errorCallback, selectedNodeTemplateName, isPropertyEdit) {
        operation.previousOperationId = $scope.getLastOperationId();
        // execute operations, create is a post
        return editorResource.create({
          topologyId: $scope.topologyId
        }, angular.toJson(operation), function(result) {
          if(_.defined(result.error) && result.error.code === 860) {
            // Topology recovery
            topologyRecoveryServices.handleTopologyRecovery(result.data, $scope.topologyId, $scope.getLastOperationId(true)).then(function(recoveryResult) {
              if(_.definedPath(recoveryResult, 'data')) {
                $scope.refreshTopology(recoveryResult.data, selectedNodeTemplateName);
                if(_.defined(successCallback)) {
                  successCallback(recoveryResult);
                }
                return;
              }
            });
            return result;
          }

          if(_.undefined(result.error)) {
            if(_.defined(isPropertyEdit)) {
              // If the call is related to a property value edition this may be a complex one and we should not perform full topology override.
              $scope.topology.operations = result.data.operations;
              $scope.topology.lastOperationIndex = result.data.lastOperationIndex;
            } else {
              $scope.refreshTopology(result.data, selectedNodeTemplateName);
            }
          }
          if(_.defined(successCallback)) {
            successCallback(result);
          }
          return result;
        }, function(error) {
          if(_.defined(errorCallback)) {
            errorCallback(error);
          }
          return error;
        }).$promise;
      };

      var editorSaveResource = $alresource('rest/latest/editor/:topologyId');
      $scope.save = function() {
        if($scope.topology.operations.length === 0 || $scope.topology.lastOperationIndex===-1) {
          // nothing to save
          return;
        }
        editorSaveResource.create({
          topologyId: $scope.topologyId,
          lastOperationId: $scope.getLastOperationId(true)
        }, null, function(result) {
          if(_.undefined(result.error)) {
            $scope.refreshTopology(result.data);
            $state.reload();
          }
        });
      };

      var editorDiscardResource = $alresource('rest/latest/editor/:topologyId/discard');
      $scope.discard = function() {
        if($scope.topology.operations.length === 0 || $scope.topology.lastOperationIndex===-1) {
          // nothing to discard
          return;
        }
        editorDiscardResource.create({
          topologyId: $scope.topologyId,
          lastOperationId: $scope.getLastOperationId(true)
        }, null, function(result) {
          if(_.undefined(result.error)) {
            $scope.refreshTopology(result.data);
          }
        });
      };

      var editorUndoResource = $alresource('rest/latest/editor/:topologyId/undo');
      function undoRedo(at) {
        editorUndoResource.create({
          topologyId: $scope.topologyId,
          at: at,
          lastOperationId: $scope.getLastOperationId(true)
        }, null, function(result) {
          if(_.undefined(result.error)) {
            $scope.refreshTopology(result.data);
            return;
          }

          //case there actually is an error
          topologyRecoveryServices.handleTopologyRecovery(result.data, $scope.topologyId, $scope.getLastOperationId(true)).then(function(recoveryResult){
            if(_.definedPath(recoveryResult, 'data')){
              $scope.refreshTopology(recoveryResult.data);
              return;
            }
          });

        });
      }
      $scope.undo = function() {
        if(0 === ($scope.topology.lastOperationIndex + 1)) {
          return;
        }
        var at = $scope.topology.lastOperationIndex - 1;
        undoRedo(at);
      };
      $scope.redo = function() {
        if($scope.topology.operations.length === ($scope.topology.lastOperationIndex + 1)) {
          return;
        }
        var at = $scope.topology.lastOperationIndex + 1;
        undoRedo(at);
      };

      // -- Begin GIT SECTIONS --
      var gitRemoteResource = $alresource('rest/latest/editor/:topologyId/git/remote');

      // Define if the push/pull buttons are enabled or not
      //
      gitRemoteResource.get({topologyId: $scope.topologyId}).$promise.then(function(response){
        if(_.defined(response.data.remoteUrl)) {
          $scope.isGitValid = true;
        } else {
          $scope.isGitValid = false;
        }
      });

      // GIT SET REMOTE
      //
      $scope.gitRemote = function() {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/topology/editor_git_remote_modal.html',
          controller: 'EditorGitRemoteModalController',
          scope: $scope,
          resolve: {
            remoteGit: ['$alresource', function($alresource) {
              return $alresource('rest/latest/editor/:topologyId/git/remote').get({topologyId: $scope.topologyId}).$promise.then(function(response){
                return response.data;
              });
            }]
          }
        });

        modalInstance.result.then(function(remoteUrl) {
          gitRemoteResource.update({
              topologyId: $scope.topologyId,
              remoteUrl: remoteUrl
            }, null, function() {
              $scope.isGitValid = true;
            });
        });
      };


      $scope.invalidTopo = $translate.instant('APPLICATIONS.TOPOLOGY.TASK.LABEL').replace(/:/g,"");;
            // Fetching topology validation status
      var editedTopologyValidatorResource = $alresource('rest/latest/editor/:topologyId/isvalid');
      function updateValidationDtos() {
              //validate topology beeing edited
        editedTopologyValidatorResource.create({
          topologyId: $scope.topologyId
        }, null, function (result) {
          if (_.undefined(result.error)) {
            $scope.editedTopologyValidationDTO = result.data;
            tasksProcessor.processAll($scope.editedTopologyValidationDTO);
          }
        });
      }
      updateValidationDtos();


      // GIT PUSH FUNCTION
      //
      $scope.gitPush = function() {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/topology/editor_git_push_pull_modal.html',
          controller: 'EditorGitPushPullModalController',
          scope: $scope,
          resolve: {
            action: function() {
              return 'PUSH';
            }
          }
        });
        modalInstance.result.then(function(gitPushPullForm) {
          var gitPushResource= $alresource('rest/latest/editor/:topologyId/git/push');
          gitPushResource.update({topologyId: $scope.topologyId, remoteBranch: gitPushPullForm.remoteBranch}, angular.toJson(gitPushPullForm.credentials), function(response) {
            if(_.undefined(response.error)) {
              toaster.pop('success', $translate.instant('EDITOR.GIT.OPERATIONS.PUSH.TITLE'), $translate.instant('EDITOR.GIT.OPERATIONS.PUSH.SUCCESS_MSGE'), 4000, 'trustedHtml', null);
            }
            console.debug('pushed');
          });
        });
      };

      $scope.showParsingErrors = function (response) {
        $uibModal.open({
          templateUrl: 'views/topology/topology_parsing_error.html',
          controller: ['$scope', '$uibModalInstance', 'uploadInfo',
            function ($scope, $uibModalInstance, uploadInfo) {
              $scope.uploadInfo = uploadInfo;
              $scope.close = function () {
                $uibModalInstance.dismiss('close');
              };
            }],
          resolve: {
            uploadInfo: function() {
              return {
                errors: response.data.errors,
                infoType: 'danger'
              };
            }
          },
          size: 'lg'
        });
      };

      // GIT PULL FUNCTION
      //
      $scope.gitPull = function() {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/topology/editor_git_push_pull_modal.html',
          controller: 'EditorGitPushPullModalController',
          scope: $scope,
          resolve: {
            action: function() {
              return 'PULL';
            }
          }
        });
        modalInstance.result.then(function(gitPushPullForm) {
          var gitPullResource= $alresource('rest/latest/editor/:topologyId/git/pull');
          gitPullResource.update({topologyId: $scope.topologyId, remoteBranch: gitPushPullForm.remoteBranch}, angular.toJson(gitPushPullForm.credentials), function(response) {
            if(_.undefined(response.error)){
              $scope.refreshTopology(response.data);
              toaster.pop('success', $translate.instant('EDITOR.GIT.OPERATIONS.PULL.TITLE'), $translate.instant('EDITOR.GIT.OPERATIONS.PULL.SUCCESS_MSGE'), 4000, 'trustedHtml', null);
            } else {
              $scope.showParsingErrors(response);
            }
          });
        });
      };
      // -- End of GIT SECTIONS --

      // key binding
      hotkeys.bindTo($scope)
        .add({
          combo: 'mod+s',
          description: 'save and commit the operations.',
          callback: function(e) {
            $scope.save();
            if(e.preventDefault) {
              e.preventDefault();
            } else {
              e.returnValue = false;
            }
          }
        })
        .add({
          combo: 'mod+z',
          description: 'undo the last operation.',
          callback: function(e) {
            $scope.undo();
            if(e.preventDefault) {
              e.preventDefault();
            } else {
              e.returnValue = false;
            }
          }
        })
        .add ({
          combo: 'mod+y',
          description: 'redo the last operation.',
          callback: function(e) {
            $scope.redo();
            if(e.preventDefault) {
              e.preventDefault();
            } else {
              e.returnValue = false;
            }
          }
        });

      var AskSaveTopologyController = ['$scope', '$uibModalInstance',
        function($scope, $uibModalInstance) {
          $scope.save = function () {
            $uibModalInstance.close(0);
          };

          $scope.undo = function() {
            $uibModalInstance.close(1);
          };

          $scope.doNotSave = function () {
            $uibModalInstance.close(2);
          };
        }];

      $scope.$on('$stateChangeStart',
        function(event, toState, toParams, fromState) {
          if (_.undefined($scope.topology) || _.undefined($scope.topology.operations) || $scope.topology.operations.length === 0 || $scope.topology.lastOperationIndex === -1) {
            // nothing to save
            return;
          }
          var getStateBasePath = function (state) {
            var lastIndexOfPoint = state.lastIndexOf('.');
            if(lastIndexOfPoint >= 0) {
              return state.substring(0, lastIndexOfPoint);
            } else {
              return state;
            }
          };
          if (getStateBasePath(toState.name) === getStateBasePath(fromState.name)) {
            // We are always inside editor
            return;
          }
          if ($scope.skipStateChangeStart) {
            // Just skip once
            return;
          }
          event.preventDefault();
          var modalInstance = $uibModal.open({
            templateUrl: 'views/topology/editor_ask_save.html',
            controller: AskSaveTopologyController
          });

          var proceedToStateChange = function () {
            // Don't intercept the next one
            $scope.skipStateChangeStart = true;
            // Save
            $state.go(toState, toParams);
          };

          modalInstance.result.then(function(result) {
            switch(result) {
            case 0:
              $scope.save();
              break;
            case 1:
               $scope.discard();
               break;
            }
            proceedToStateChange();
          }, function() {
            proceedToStateChange();
          });
        });
      }
    ]);
  }
); // define
