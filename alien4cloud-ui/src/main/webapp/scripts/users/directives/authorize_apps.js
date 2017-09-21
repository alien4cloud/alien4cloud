define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/users/controllers/apps_authorization_modal_ctrl');

  modules.get('a4c-security').directive('alienAuthorizeApps', ['$uibModal', function ($uibModal) {
    return {
      restrict: 'A',
      scope:{

        /**
        Already authorized subjects
        */
        'authorizedSubjects': '=',

        /*searchConfigBuilder, a function to pass to the add authorisation modal that should return an object like
          {
            url: // the url for searching
            params: // eventually params to complete the url
            useParams: // whether the remaining params after constructing the url shold be passed like param args or into the body of the request
        }
      */
        'buildSearchConfig': '&searchConfigBuilder',
        /* onClose: this is the function called when closing the modal
        it will be passed as arg an object named:
        result {
          subjects: an array containing the subjects to process
          foce: whether to trigger the service with force option
        }
        */
        'onClose': '&',
        /*
          display force option
        */
        'displayCustomSearch': '=',

        /**
        application: if we want to open the modal on an application only
        */
        'application': '='
      },
      link: function(scope, element) {

        var openAuthorizationModal = function () {
          var modalInstance = $uibModal.open({
            templateUrl: 'views/users/apps_authorization_popup.html',
            controller: 'AppsAuthorizationModalCtrl',
            scope: scope,
            windowClass: 'authorization-modal'
          });

          modalInstance.result.then(function (result) {
            //call provided onClose function
            scope.onClose({result: result});
          });
        };

        //bind click event
        element.on('click', openAuthorizationModal);
      }
    };
  }]);
});
