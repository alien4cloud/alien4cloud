define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common').controller('ConfirmModalCtrl', ['$scope', '$uibModalInstance', 'title', 'content', function($scope, $uibModalInstance, title, content) {
    $scope.title = title;
    $scope.content = content;
    $scope.confirm = function() {
      $uibModalInstance.close();
    };
    $scope.cancel = function() {
      $uibModalInstance.dismiss('cancel');
    };
  }]); // controller
}); // define
