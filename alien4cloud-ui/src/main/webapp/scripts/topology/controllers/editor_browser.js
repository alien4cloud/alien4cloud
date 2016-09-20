// Editor file browser controller.
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('angular-ui-ace');

  require('angular-tree-control');
  require('scripts/common/services/explorer_service');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('TopologyBrowserCtrl',
    ['$scope', '$http', 'explorerService', '$stateParams', 'topoEditDisplay', 'uploadServiceFactory',
    function($scope, $http, explorerService, $stateParams, topoEditDisplay, uploadServiceFactory) {
    var openOnFile = $stateParams.file;

    $scope.displays = {
      tree: { active: true, size: 400, selector: '#tree-box', only: [] },
    };
    topoEditDisplay($scope, '#editor-explorer');

    $scope.isImage = false;
    $scope.treedata = {
      children: [],
      name: 'loading...'
    };

    var selected = null;
    if(_.defined(openOnFile)) {
      selected = {fullPath: openOnFile};
      $scope.selected = selected;
    }
    var aceEditor;
    $scope.aceLoaded = function(_editor){
      aceEditor = _editor;
      _editor.commands.addCommand({
        name: 'save',
        bindKey: {win: 'Ctrl-S', mac: 'Command-S'},
        exec: function() {
          $scope.saveFile();
        }
      });
    };

    $scope.mode = explorerService.getDefaultMode(); // jshint ignore:line
    $scope.expandedNodes = [];
    $scope.opts = explorerService.getOps(false);
    $scope.filePath = '';

    $scope.onToggled = function(node) {
      var dirName = node.fullPath.substring(node.fullPath.split('/', 2).join('/').length+1);
      $scope.filePath = dirName + '/';
    };

    $scope.showSelected = function(node) {
      var dirName = node.fullPath.substring(node.fullPath.split('/', 2).join('/').length+1);
      $scope.filePath = dirName;
      var selectedUrl;
      if(_.defined(node.artifactId)) {
        // temp file under edition
        selectedUrl = '/rest/latest/editor/' + $scope.topology.topology.id + '/file/' + node.artifactId;
      } else {
        // commited file
        selectedUrl = '/static/tosca/' + $scope.topology.topology.id + node.fullPath;
      }
      _.isImage(selectedUrl).then(function(isImage) {
        if(isImage) {
          $scope.imageUrl = selectedUrl;
          $scope.isImage = isImage;
          $scope.$apply();
        } else {
          $scope.isImage = false;
          $http({method: 'GET',
            url: selectedUrl})
            .success(function(data) {
              $scope.aceFilePath = dirName;
              aceEditor.getSession().setValue(data);
              // $scope.editorContent = data;
              $scope.mode = explorerService.getMode(node);
            });
        }
      });
    };
    function update() {
      var root = $scope.topology.archiveContentTree.children[0];
      $scope.treedata.children = root.children;
      if(selected !== null) {
        $scope.showSelected(selected);
        explorerService.expand($scope.expandedNodes, $scope.selected.fullPath, root, false);
      }
    }
    // Load archive content file
    $scope.$on('topologyRefreshedEvent', function() {
      update();
    });
    if(_.defined($scope.topology)) {
      update();
    }

    $scope.uploadSuccessCallback = function(result) {
      $scope.refreshTopology(result.data);
    };
    var uploadService = uploadServiceFactory($scope);
    $scope.onFileSelect = function($files) {
      // if there is a callback for before uploding, then call it first
      var file = $files[0];
      var url = 'rest/latest/editor/' + $scope.topology.topology.id + '/upload/';
      if($scope.filePath.length ===0 || $scope.filePath.endsWith('/')) {
        $scope.filePath += file.name;
      }
      uploadService.doUpload(file, {file: file, url: url, data: {lastOperationId: $scope.getLastOperationId(), path: $scope.filePath}});
    };

    $scope.deleteFile = function() {
      $scope.execute({
        type: 'org.alien4cloud.tosca.editor.operations.DeleteFileOperation',
        path: $scope.filePath
      });
    };
    $scope.isFile = function() {
      return $scope.filePath.length!==0 || !$scope.filePath.endsWith('/');
    };
    $scope.isNewFile = function() {
      if(!$scope.isFile()) {
        return false; // this is not a file so this is not a new file.
      }
      return !$scope.exists();
    };
    $scope.exists = function() {
      if(_.undefined($scope.topology)) {
        return false;
      }
      var currentNode = $scope.topology.archiveContentTree.children[0];
      var part, parts = $scope.filePath.split('/');
      for(var i=0;i<parts.length;i++) {
        part = parts[i];
        if(part.length === 0) {
          continue;
        }
        var partIndex = _.findIndex(currentNode.children, 'name', part);
        if(partIndex === -1) {
          return false; // not found
        }
        currentNode = currentNode.children[partIndex];
      }
      return true;
    };
    $scope.createFile = function() {
      $scope.execute({
        type: 'org.alien4cloud.tosca.editor.operations.UpdateFileContentOperation',
        path: $scope.filePath,
        content: ''
      });
    };
    $scope.saveFile = function() {
      if(_.undefined($scope.aceFilePath) || _.undefined(aceEditor)) {
        return;
      }
      $scope.execute({
        type: 'org.alien4cloud.tosca.editor.operations.UpdateFileContentOperation',
        path: $scope.aceFilePath,
        content: aceEditor.getSession().getDocument().getValue()
      });
    };
  }]);
});
