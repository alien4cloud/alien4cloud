define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-editor', ['ui.bootstrap']).controller('EditorGitRemoteModalController', ['$scope', '$modalInstance', 'remoteGit',
    function($scope, $modalInstance, remoteGit) {
      $scope.remoteGit = remoteGit;

      $scope.setRemote = function() {
        $modalInstance.close($scope.remoteGit.remoteUrl);
      };

      $scope.close = function() {
        $modalInstance.dismiss('close');
      };
    }
  ]);

  modules.get('a4c-topology-editor', ['ui.bootstrap']).controller('EditorGitPushPullModalController', ['$scope', '$modalInstance', 'action',
    function($scope, $modalInstance, action) {
      $scope.gitPushPullForm = {};
      $scope.gitPushPullForm.credentials = {};
      $scope.action = action;

      $scope.push = function() {
        var form = {
          'credentials': {
            'username': $scope.gitPushPullForm.credentials.username,
            'password': $scope.gitPushPullForm.credentials.password,
          },
          'remoteBranch': $scope.gitPushPullForm.remoteBranch
        };
        $modalInstance.close(form);
      };

      $scope.pull = function() {
        var form = {
          'credentials': {
            'username': $scope.gitPushPullForm.credentials.username,
            'password': $scope.gitPushPullForm.credentials.password,
          },
          'remoteBranch': $scope.gitPushPullForm.remoteBranch
        };
        $modalInstance.close(form);
      };
      $scope.close = function() {
        $modalInstance.dismiss('close');
      };
    }
  ]);
});
