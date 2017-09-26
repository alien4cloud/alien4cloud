define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/users/controllers/subjects_authorization_directive_ctrl');


  modules.get('a4c-security').directive('alienGroupAuthorization', function () {
    return {
      templateUrl: 'views/users/groups_authorization_directive.html',
      restrict: 'E',
      windowClass: 'authorization-modal',
      scope: {
        /*The resource to secure*/
        'resource': '=',

        /*the service for authorizations CRUD*/
        'service': '=',

        /*searchConfigBuilder, a function to pass to the add authorisation modal that should return an object like
          {
            url: // the url for searching
            params: // eventually params to complete the url
            useParams: // whether the remaining params after constructing the url shold be passed like param args or into the body of the request
        }
      */
        'searchConfigBuilder': '&',

        'displayCustomSearch': '='
      },
      link: function(scope) {
        scope.authorizeModalTemplateUrl = 'views/users/groups_authorization_popup.html';

        scope.getId = function(group){
          return group.id;
        };

        scope.getRevokeParams = function(group){
          return {
            groupId: group.id
          };
        };
      }
    };
  });
});
