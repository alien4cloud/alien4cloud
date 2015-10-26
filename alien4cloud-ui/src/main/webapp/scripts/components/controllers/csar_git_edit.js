define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-components', ['ui.bootstrap']).controller('EditCsarGitController', ['$scope', '$modalInstance','csar',
  function($scope, $modalInstance,csar) {
    $scope.csarGitTemplate = {};
    $scope.url = csar.repositoryUrl;
    $scope.username = csar.username;
    $scope.password = csar.password;
    $scope.id = csar.id;

    $scope.update = function(url,username,password) {
      var csargitDTO = {
        'repositoryUrl': url,
        'username': username,
        'password': password,
      };
      var id = $scope.id;
      var dtoData = {
        'dto':csargitDTO,
        'id':id
      }
      $modalInstance.close(dtoData);
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };
  }
  ]);
});
