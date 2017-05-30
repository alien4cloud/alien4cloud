define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-editor', ['ui.bootstrap']).controller('TopologyEditorArtifactModalCtrl', ['$scope', '$uibModalInstance', '$translate', 'explorerService','archiveContentTree', 'availableRepositories', 'artifact', 'topology', 'toaster',
    function($scope, $uibModalInstance, $translate, explorerService, archiveContentTree, availableRepositories, artifact, topology, toaster) {
      $scope.artifact = {};

      $scope.opts = explorerService.getOps(false);
      $scope.treedata = {
        children: [],
        name: 'loading...'
      };

      if (artifact.artifactRepository === 'alien_topology') {
        $scope.activeTab = 'local';
      } else {
        $scope.activeTab = 'remote';
      }
      $scope.initialRepositoryName = artifact.repositoryName;

      $scope.onSelect = function(node) {
        var dirName = node.fullPath.substring(node.fullPath.split('/', 2).join('/').length+1);
        $scope.artifact.repository = undefined;
        $scope.artifact.reference = dirName;
        $scope.artifact.archiveVersion = topology.archiveVersion;
        $scope.artifact.archiveName = topology.archiveName;
      };

      var root = archiveContentTree.children[0];
      $scope.treedata.children = root.children;

      $scope.isRemoteArtifact = function(classifier) {
        return classifier === 'archive' || classifier === 'global' || classifier === 'new';
      };

      $scope.save = function(valid) {
        if (valid) {
          $uibModalInstance.close($scope.artifact);
        }
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };

      // remote tab
      $scope.repositorySelected = function(selectedRepositoryIdx) {
        // we clone the repo because the use can change it's id (if it's not an archive repo)
        $scope.selectedRepository = {};
        $scope.selectedRepository.id = $scope.repositories[selectedRepositoryIdx].id;
        $scope.selectedRepository.idx = $scope.repositories[selectedRepositoryIdx].idx;
        $scope.selectedRepository.type = $scope.repositories[selectedRepositoryIdx].type;
        $scope.selectedRepository.url = $scope.repositories[selectedRepositoryIdx].url;
        $scope.selectedRepository.classifier = $scope.repositories[selectedRepositoryIdx].classifier;
      };

      $scope.selectRemote = function(valid) {
        if (valid) {
          if (!$scope.isRepositoryUnique()) {
            toaster.pop('error', $translate.instant('EDITOR.ARTIFACTS.REPOS.MUST_BE_UNIQUE'), $translate.instant('EDITOR.ARTIFACTS.REPOS.MUST_BE_UNIQUE'), 3000, 'trustedHtml', null);
            return;
          }
          var artifact = {};
          artifact.repository = $scope.selectedRepository.type;
          artifact.reference = $scope.selectedRepository.file;
          artifact.repositoryUrl = $scope.selectedRepository.url;
          artifact.repositoryName = $scope.selectedRepository.id;
          artifact.archiveVersion = topology.archiveVersion;
          artifact.archiveName = topology.archiveName;
          
          $uibModalInstance.close(artifact);
        }
      };

      $scope.isRepositoryUnique = function() {
        if ($scope.selectedRepository === undefined) {
          return true;
        }
        for (var i = 0; i < $scope.repositories.length; i++) {
          if ($scope.repositories[i].id === $scope.selectedRepository.id && $scope.repositories[i].idx !== $scope.selectedRepository.idx && $scope.repositories[i].classifier === 'archive') {
            return false;
          }
        }
        return true;
      };
      $scope.setActiveTab = function(tab) {
        $scope.activeTab = tab;
      };

      // here build the data structure for the select box
      $scope.selectedRepository = undefined;
      $scope.repositoryTypes = availableRepositories.repositoryTypes;
      $scope.repositories = [];
      var idx = 0;
      var availableRepository;
      for (var i = 0; i < availableRepositories.archiveRepository.length; i++) {
        availableRepository = {};
        availableRepository.idx = idx++;
        availableRepository.id = availableRepositories.archiveRepository[i].id;
        availableRepository.type = availableRepositories.archiveRepository[i].type;
        availableRepository.url = availableRepositories.archiveRepository[i].url;
        availableRepository.classifier = 'archive';
        $scope.repositories.push(availableRepository);
        if (!$scope.isLocalRepository && $scope.initialRepositoryName === availableRepository.id) {
          $scope.repositorySelected(availableRepository.idx);
          $scope.selectedRepository.file = artifact.artifactRef;
        }
      }
      for (i = 0; i < availableRepositories.alienRepository.length; i++) {
        availableRepository = {};
        availableRepository.idx = idx++;
        availableRepository.id = availableRepositories.alienRepository[i].id;
        availableRepository.type = availableRepositories.alienRepository[i].type;
        availableRepository.url = availableRepositories.alienRepository[i].url;
        availableRepository.classifier = 'global';
        $scope.repositories.push(availableRepository);
      }
      availableRepository = {};
      availableRepository.idx = idx++;
      availableRepository.id = 'MyRepository';
      availableRepository.type = '';
      availableRepository.url = '';
      availableRepository.classifier = 'new';
      $scope.repositories.push(availableRepository);


    }
  ]);
});
