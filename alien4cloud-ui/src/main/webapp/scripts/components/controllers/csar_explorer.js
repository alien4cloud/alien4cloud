define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('angular-ui-ace');

  require('angular-tree-control');
  require('scripts/common/services/explorer_service');

  modules.get('a4c-components', ['a4c-common', 'ui.ace', 'treeControl']).controller(
    'CsarExplorerController', ['$scope', '$modalInstance', '$http', 'explorerService', 'archiveName', 'archiveVersion', 'openOnFile', function($scope, $modalInstance, $http, explorerService, archiveName, archiveVersion, openOnFile) {

    $scope.isImage = false;
    $scope.archiveName = archiveName;
    $scope.archiveVersion = archiveVersion;
    $scope.treedata = {
      children: [],
      name: 'loading...'
    };

    var selected = null;
    if(openOnFile && openOnFile !== null) {
      selected = {fullPath: '/expanded/'+openOnFile};
      $scope.selected = selected;
    }

    $scope.mode = explorerService.getDefaultMode(); // jshint ignore:line
    $scope.expandedNodes = [];
    $scope.opts = explorerService.getOps(false);

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
            url: selectedUrl})
            .success(function(data) {
              $scope.editorContent = data;
              $scope.mode = explorerService.getMode(node);
            });
        }
      });
    };

    // Load archive content file
    $http({method: 'GET', url: '/static/tosca/'+archiveName+'/'+archiveVersion+'/content.json'}).success(function(data) {
      $scope.treedata.children = data.children[0].children;
      if(selected !== null) {
        $scope.showSelected(selected);
        explorerService.expand($scope.expandedNodes, $scope.selected.fullPath, data.children[0], false);
      }
    });
    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };
  }]);
});
