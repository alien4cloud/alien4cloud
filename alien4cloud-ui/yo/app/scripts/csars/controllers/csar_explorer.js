'use strict';

angular.module('alienUiApp').controller(
  'CsarExplorerController', ['$scope', '$modalInstance', '$http', 'archiveName', 'archiveVersion', 'openOnFile', function($scope, $modalInstance, $http, archiveName, archiveVersion, openOnFile) {

  $scope.archiveName = archiveName;
  $scope.archiveVersion = archiveVersion;

  $scope.treedata = {
    children: [],
    name: 'arf'
  };

  var selected = null;
  if(openOnFile && openOnFile !== null) {
    selected = {fullPath: '/expanded/'+openOnFile};
    $scope.selected = selected;
  }

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

  $http({method: 'GET', url: '/csarrepository/'+archiveName+'/'+archiveVersion+'/content.json'}).success(function(data) {
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

  $scope.showSelected = function(node) {
    $http({method: 'GET',
      transformResponse: function(data) { return data; },
      url: '/csarrepository/'+archiveName+'/'+archiveVersion+node.fullPath})
      .success(function(data) {
        $scope.editorContent = data;
      });
  };

  $scope.cancel = function() {
    $modalInstance.dismiss('cancel');
  };
}]);
