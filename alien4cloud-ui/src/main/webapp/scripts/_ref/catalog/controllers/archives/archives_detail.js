define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/components/services/csar');

  // register archives detail state
  states.state('catalog.archives.detail', {
    url: '/detail/:id',
    templateUrl: 'views/_ref/catalog/archives/archives_detail.html',
    controller: 'ArchivesDetailCtrl'
  });

  modules.get('a4c-catalog').controller(
    'ArchivesDetailCtrl', ['$scope', '$stateParams', '$state', 'csarService', '$translate', 'toaster', 'breadcrumbsService',
    function($scope, $stateParams, $state, csarService, $translate, toaster, breadcrumbsService) {

      // register breadcrumbs config
      breadcrumbsService.putConfig({
        state : 'catalog.archives.detail',
        text: function(){
          return $stateParams.id;
        }
      });

      /* Retrieve CSAR to display */
      csarService.getAndDeleteCsar.get({
        csarId: $stateParams.id
      }, function(successResult) {
        $scope.csar = successResult.data;
      });

      $scope.remove = function(csarId) {
        csarService.getAndDeleteCsar.remove({
          csarId: csarId
        }, function(result) {
          var errorMessage = csarService.builtErrorResultList(result);
          if (errorMessage) {
            var title = $translate.instant('CSAR.ERRORS.' + result.error.code + '_TITLE');
            toaster.pop('error', title, errorMessage, 4000, 'trustedHtml', null);
          } else {
            $state.go('catalog.archives');
          }
        });
      };
    }
  ]); // controller
});
