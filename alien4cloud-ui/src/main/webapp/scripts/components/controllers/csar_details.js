define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/components/services/csar');
  require('scripts/common/services/websocket_services');
  require('scripts/deployment/services/deployment_services');

  states.state('components.csars.csardetail', {
    url: '/details/:csarId',
    templateUrl: 'views/components/csar_details.html',
    controller: 'CsarDetailsCtrl'
  });

  modules.get('a4c-components', ['ui.router', 'ui.bootstrap', 'a4c-deployment']).controller(
    'CsarDetailsCtrl', ['$scope', '$stateParams', '$state', 'csarService', 'deploymentServices', 'webSocketServices', '$translate', 'toaster',
    function($scope, $stateParams, $state, csarService, deploymentServices, webSocketServices, $translate, toaster) {

      /* Retrieve CSAR to display */
      csarService.getAndDeleteCsar.get({
        csarId: $stateParams.csarId
      }, function(successResult) {
        $scope.csar = successResult.data;
      });

      $scope.remove = function(csarId) {
        csarService.getAndDeleteCsar.remove({
          csarId: csarId
        }, function(result) {
          var errorMessage = csarService.builtErrorResultList(result);
          if (errorMessage) {
            var title = $translate('CSAR.ERRORS.' + result.error.code + '_TITLE');
            toaster.pop('error', title, errorMessage, 4000, 'trustedHtml', null);
          } else {
            $state.go('components.csars');
          }
        });
      };

      //Go to runtime view for a deployment
      $scope.goToRuntimeView = function(id){
        $state.go('topologytemplates.detail.topology', {
          id:id
        });
      };

    }
  ]); // controller
});// define
