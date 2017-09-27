define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/applications/directives/topology_init_from_select');

  modules.get('a4c-applications', ['ui.bootstrap']).controller('NewApplicationCtrl', ['$scope', '$uibModalInstance', '$http',
    function($scope, $uibModalInstance) {
      $scope.app = {};
      $scope.namePattern=new RegExp('^[^\/\\\\]+$');
      $scope.archiveNamePattern=new RegExp('^\\w+$');
      $scope.fromIndex = 3;
      var autoGenArchiveName = true;
      $scope.nameChange = function () {
        if (autoGenArchiveName && $scope.app.name) {
          $scope.app.archiveName = _.capitalize(_.camelCase($scope.app.name));
        }
      };
      $scope.archiveNameChange = function () {
        autoGenArchiveName = false;
      };
      $scope.create = function (valid) {
        if (valid) {
          // if we create from template let's set the template id to the app.
          if($scope.fromIndex === 2) {
            $scope.app.topologyTemplateVersionId = $scope.topologyTemplate.versionId;
          }
          $uibModalInstance.close($scope.app);
        }
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ]);
});
