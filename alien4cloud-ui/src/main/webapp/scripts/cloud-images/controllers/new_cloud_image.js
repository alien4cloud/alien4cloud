// list of cloud images that can be defined for multiple clouds actually.
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  modules.get('a4c-clouds', ['ui.router', 'ui.bootstrap']).controller(
    'NewCloudImageController', [ '$scope', '$modalInstance', 'cloudImageServices','$q',
      function($scope, $modalInstance, cloudImageServices, $q) {
        $scope.cloudImageFormDescriptor = cloudImageServices.getFormDescriptor();
        $scope.cloudImage = {};

        $scope.save = function(cloudImage) {
          return cloudImageServices.create({}, angular.toJson(cloudImage), function success(response) {
            if (_.defined(response.error)) {
              var errorsHandle = $q.defer();
              return errorsHandle.resolve(response.error);
            } else {
              $modalInstance.close(response.data);
            }
          }).$promise;
        };

        $scope.cancel = function() {
          $modalInstance.dismiss('cancel');
        };
      }
    ]
  ); // controller
}); // define
