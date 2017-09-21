// Helper utils for authorizations directives
define(function () {
  'use strict';

  var utils = {};
  /**
    templateUrl: url of the template to use for the modal
    defaultSearchUrl: the default search endpoint to use if no search config is provided
    idPropertyName: name of the property that holds the id of the subject (for example username or id)
  */
  utils.buildAuthorizeModalDirective = function(templateUrl, defaultSearchUrl, idPropertyName, $parse, $uibModal) {
    return {
      restrict: 'A',
      scope: {
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
        'displayCustomSearch': '='
      },
      link: function(scope, element) {

        // property name to retrieve the id of the subject
        scope.idPropertyName = idPropertyName;
        // the default search url
        scope.defaultSearchUrl = defaultSearchUrl;

        var openAuthorizationModal = function () {
          var modalInstance = $uibModal.open({
            templateUrl: templateUrl,
            controller: 'SubjectAuthorizationModalCtrl',
            windowClass: 'authorization-modal',
            scope: scope
          });

          modalInstance.result.then(function (result) {
            //call provided onClose function
            scope.onClose({result: result});
          });
        };
        element.on('click', openAuthorizationModal);
      }
    };
  };

  return utils;
});
