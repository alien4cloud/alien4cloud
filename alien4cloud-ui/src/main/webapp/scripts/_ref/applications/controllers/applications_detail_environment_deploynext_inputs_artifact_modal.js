define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-applications', ['ui.bootstrap']).controller('ApplicationInputArtifactModalCtrl', ['$scope', 'Upload', '$uibModalInstance', '$translate', 'explorerService','archiveContentTree', 'availableRepositories', 'artifact', 'artifactKey', 'application', 'environment','updateScopeDeploymentTopologyDTO', 'topology', 'toaster',
    function($scope, $upload, $uibModalInstance, $translate, explorerService, archiveContentTree, availableRepositories, artifact, artifactKey, application, environment, updateScopeDeploymentTopologyDTO, topology, toaster) {

      var newRepositoryName = 'MyRepository';
      $scope.environment = environment;
      $scope.opts = explorerService.getOps(false);
      $scope.treedata = {
        children: [],
        name: 'loading...'
      };

      if(_.undefined(artifact)){
        artifact = {
          repositoryName: newRepositoryName
        };
      }

      $scope.initialRepositoryName = artifact.repositoryName;


      if(_.isEqual(artifact.artifactRepository, 'alien_repository')){
        $scope.activeTabIndex = 0;
        $scope.initialArtifactClassifier = 'local_fs';
      } else if (_.isEqual(artifact.artifactRepository, 'alien_topology')) {
        $scope.activeTabIndex = 1;
        $scope.initialArtifactClassifier = 'csar';
      } else {
        $scope.activeTabIndex = 2;
        $scope.initialArtifactClassifier = 'remote';
      }


      // tab local file
      $scope.selectedCSARArtifact = {};
      $scope.onCSARFileSelected = function(node) {
        var dirName = node.fullPath.substring(node.fullPath.split('/', 2).join('/').length+1);
        $scope.selectedCSARArtifact.repository = 'alien_topology';
        $scope.selectedCSARArtifact.reference = dirName;
        $scope.selectedCSARArtifact.archiveVersion = topology.archiveVersion;
        $scope.selectedCSARArtifact.archiveName = topology.archiveName;
        $scope.selectedCSARArtifact.valid = true;
      };

      var root = archiveContentTree.children[0];
      $scope.treedata.children = root.children;

      $scope.isRemoteArtifact = function(classifier) {
        return classifier === 'archive' || classifier === 'global' || classifier === 'new';
      };

      $scope.isRepositoryNameEditable = function(repository) {
        return _.undefined(repository) || _.undefined(repository.classifier) || repository.classifier === 'new';
      };

      $scope.save = function(valid) {
        if (valid) {
          $uibModalInstance.close($scope.selectedCSARArtifact);
        }
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };

      // Upload tab
      $scope.doUploadArtifact = function (file) {
        $scope.uploadArtifactInfo = {
          'isUploading': true,
          'isUploaded' : false,
          'type': 'info'
        };
        $upload.upload({
          url: 'rest/latest/applications/' + application.id + '/environments/' + environment.id + '/deployment-topology/inputArtifacts/' + artifactKey + '/upload',
          file: file
        }).progress(function (evt) {
          $scope.uploadArtifactInfo.uploadProgress = parseInt(100.0 * evt.loaded / evt.total);
        }).success(function (success) {
          $scope.uploadArtifactInfo.isUploading = false;
          $scope.uploadArtifactInfo.isUploaded = true;
          $scope.uploadArtifactInfo.type = 'success';
          updateScopeDeploymentTopologyDTO(success.data);
        }).error(function (data, status) {
          $scope.uploadArtifactInfo.type = 'error';
          $scope.uploadArtifactInfo.error = {};
          $scope.uploadArtifactInfo.error.code = status;
          $scope.uploadArtifactInfo.error.message = 'An Error has occurred on the server!';
        });
      };

      $scope.onArtifactSelected = function ($files) {
        var file = $files[0];
        $scope.doUploadArtifact(file);
      };

      // remote tab
      $scope.selectedRepositoryIdx = 0;
      $scope.repositorySelected = function(selectedRepositoryIdx) {
        $scope.selectedRepositoryIdx = selectedRepositoryIdx;
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
          var selectedRemoteArtifact = {
            repository: $scope.selectedRepository.type,
            reference: $scope.selectedRepository.file,
            repositoryUrl: $scope.selectedRepository.url,
            repositoryName: $scope.selectedRepository.id,
            archiveVersion: null,
            archiveName: null
          };

          $uibModalInstance.close(selectedRemoteArtifact);
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
      $scope.setActiveTabIndex = function(index) {
        $scope.activeTabIndex = index;
      };

      // here build the data structure for the select box
      $scope.selectedRepository = undefined;
      $scope.repositoryTypes = availableRepositories.repositoryTypes;
      $scope.repositories = [];
      var idx = 0;
      var availableRepository;
      var artifactRepositoryFound = false;
      var isRemoteArtifact = _.isEqual($scope.initialArtifactClassifier, 'remote');
      for (var i = 0; i < availableRepositories.archiveRepository.length; i++) {
        availableRepository = {};
        availableRepository.idx = idx++;
        availableRepository.id = availableRepositories.archiveRepository[i].id;
        availableRepository.type = availableRepositories.archiveRepository[i].type;
        availableRepository.url = availableRepositories.archiveRepository[i].url;
        availableRepository.classifier = 'archive';
        $scope.repositories.push(availableRepository);
        if (isRemoteArtifact && $scope.initialRepositoryName === availableRepository.id) {
          $scope.repositorySelected(availableRepository.idx);
          $scope.selectedRepository.file = artifact.artifactRef;
          artifactRepositoryFound = true;
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
        if (isRemoteArtifact && $scope.initialRepositoryName === availableRepository.id) {
          $scope.repositorySelected(availableRepository.idx);
          $scope.selectedRepository.file = artifact.artifactRef;
          artifactRepositoryFound = true;
        }
      }

      var newRepositoryEntry = {};
      newRepositoryEntry.idx = idx++;
      newRepositoryEntry.id = newRepositoryName;
      newRepositoryEntry.type = '';
      newRepositoryEntry.url = '';
      newRepositoryEntry.classifier = 'new';
      $scope.repositories.push(newRepositoryEntry);

      if (!artifactRepositoryFound) {
        $scope.repositorySelected(newRepositoryEntry.idx);
        $scope.selectedRepository.id = artifact.repositoryName;
        $scope.selectedRepository.type = artifact.artifactRepository;
        $scope.selectedRepository.url = artifact.repositoryURL;
        $scope.selectedRepository.file = artifact.artifactName;
      }
    }
  ]);
});
