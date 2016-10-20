define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/properties_services');

  modules.get('a4c-common').controller('SimpleModalCtrl', ['$scope', '$translate', '$modal',
    function($scope, $translate, $modal) {

      var ModalInstanceCtrl = ['$scope', '$modalInstance', 'title', 'content', function($scope, $modalInstance, title, content) {
        $scope.title = title;
        $scope.content = content;
        $scope.close = function() {
          $modalInstance.dismiss('close');
        };
      }];

      $scope.openSimpleModal = function(modalTitle, modalContent, event) {
        if (_.defined(event)) {
          event.stopPropagation();
        }
        $modal.open({
          templateUrl: 'views/common/simple_modal.html',
          controller: ModalInstanceCtrl,
          resolve: {
            title: function() {
              return modalTitle;
            },
            content: function() {
              return modalContent;
            }
          }
        });
      };
    }
  ]); // controller
}); // define
