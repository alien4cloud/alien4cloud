define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-editor', ['ui.bootstrap']).controller('EditorGitRemoteModalController', ['$scope', '$uibModalInstance', 'remoteGit',
    function($scope, $uibModalInstance, remoteGit) {
      $scope.remoteGit = remoteGit;

      $scope.setRemote = function() {
        $uibModalInstance.close($scope.remoteGit.remoteUrl);
      };

      $scope.close = function() {
        $uibModalInstance.dismiss('close');
      };
    }
  ]);

  modules.get('a4c-topology-editor', ['ui.bootstrap']).controller('EditorGitPushPullModalController', ['$scope', '$uibModalInstance', 'action',
    function($scope, $uibModalInstance, action) {
      $scope.gitPushPullForm = {};
      $scope.gitPushPullForm.credentials = {};
      $scope.action = action;

      $scope.push = function() {
        var form = {
          'credentials': {
            'username': $scope.gitPushPullForm.credentials.username,
            'password': $scope.gitPushPullForm.credentials.password
          },
          'remoteBranch': $scope.gitPushPullForm.remoteBranch
        };
        $uibModalInstance.close(form);
      };

      $scope.pull = function() {
        var form = {
          'credentials': {
            'username': $scope.gitPushPullForm.credentials.username,
            'password': $scope.gitPushPullForm.credentials.password
          },
          'remoteBranch': $scope.gitPushPullForm.remoteBranch
        };
        $uibModalInstance.close(form);
      };
      $scope.close = function() {
        $uibModalInstance.dismiss('close');
      };
    }
  ]);
});
