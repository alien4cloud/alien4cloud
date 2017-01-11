define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/properties_services');

  modules.get('a4c-common').controller('SimpleModalCtrl', ['$scope', '$translate', '$uibModal',
    function($scope, $translate, $uibModal) {

      var ModalInstanceCtrl = ['$scope', '$uibModalInstance', 'title', 'content', function($scope, $uibModalInstance, title, content) {
        $scope.title = title;
        $scope.content = content;
        $scope.close = function() {
          $uibModalInstance.dismiss('close');
        };
      }];

      $scope.openSimpleModal = function(modalTitle, modalContent, event) {
        if (_.defined(event)) {
          event.stopPropagation();
        }
        $uibModal.open({
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
