define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('angular-ui-ace');

  require('angular-tree-control');

  modules.get('a4c-components', ['ui.ace', 'treeControl']).controller(
    'CsarExplorerController', ['$scope', '$modalInstance', '$http', 'archiveName', 'archiveVersion', 'openOnFile', function($scope, $modalInstance, $http, archiveName, archiveVersion, openOnFile) {

    $scope.isImage = false;
    $scope.archiveName = archiveName;
    $scope.archiveVersion = archiveVersion;

    var extToMode = {
      yaml: 'yaml',
      yml:  'yaml',
      groovy: 'groovy',
      sh: 'sh',
      properties: 'properties',
      bat: 'batchfile',
      cmd: 'batchfile',
      py: 'python',
      default_mode: 'yaml'
    };

    $scope.treedata = {
      children: [],
      name: 'arf'
    };

    var selected = null;
    if(openOnFile && openOnFile !== null) {
      selected = {fullPath: '/expanded/'+openOnFile};
      $scope.selected = selected;
    }

    $scope.mode = extToMode['default_mode'];

    $scope.expandedNodes = [];

    function expand(node, isExpandable) {
      if(isExpandable) {
        if(selected.fullPath !== node.fullPath && selected.fullPath.indexOf(node.fullPath) > -1) {
          $scope.expandedNodes.push(node);
        }
      }
      if(node.children && node.children!==null) {
        for(var i=0;i<node.children.length;i++) {
          expand(node.children[i], true);
        }
      }
    }

    $http({method: 'GET', url: '/static/tosca/'+archiveName+'/'+archiveVersion+'/content.json'}).success(function(data) {
      $scope.treedata.children = data.children[0].children;
      if(selected !== null) {
        $scope.showSelected(selected);
        expand(data.children[0], false);
      }
    });
    $scope.opts = {
      dirSelectable: false,
      equality: function(node1, node2) {
        if(node1 && node2) {
          return node1.fullPath === node2.fullPath;
        }
        return false;
      }
    };

    var updateMode = function(node){
      var fileName = node.fullPath;
      var tokens = fileName.trim().split('/');
      if (tokens.length > 0) {
        fileName = tokens[tokens.length - 1];
      }
      var fileExt = fileName.substr((fileName.lastIndexOf('.') -1 >>> 0) + 2).toLowerCase();
      $scope.mode = _.defined(extToMode[fileExt]) ? extToMode[fileExt] :extToMode['default_mode'];
    };

    $scope.showSelected = function(node) {
      var selectedUrl = '/static/tosca/'+archiveName+'/'+archiveVersion+node.fullPath;

      _.isImage(selectedUrl).then(function(isImage) {
        if(isImage) {
          $scope.imageUrl = selectedUrl;
          $scope.isImage = isImage;
          $scope.$apply();
        } else {
          $scope.isImage = false;
          $http({method: 'GET',
            transformResponse: function(data) { return data; },
            url: selectedUrl})
            .success(function(data) {
              $scope.editorContent = data;
              updateMode(node);
            });
        }
      });
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };
  }]);
});
