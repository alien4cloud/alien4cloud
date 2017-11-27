// Display the modal of credentias if necessary
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/_ref/applications/controllers/secret_display_modal');

  modules.get('a4c-applications', ['ngResource']).factory('secretDisplayModal', ['toaster', '$uibModal', '$q',
    function(toaster, $uibModal, $q) {

      var open = function (secretCredentialInfos) {
        // credentialDescriptor
        var promise;
        switch (_.size(secretCredentialInfos)) {
          case 1:
            return $uibModal.open({
              templateUrl: 'views/_ref/applications/secret_display_modal.html',
              controller: 'SecretCredentialsController',
              resolve: {
                secretCredentialInfos : function() {
                  return secretCredentialInfos;
                }
              }
            }).result;
          case 0:
            promise = $q.defer();
            promise.resolve();
            return promise.promise;
          default:
            toaster.pop('error', 'Multi locations', 'is not yet supported', 0, 'trustedHtml', null);
            promise = $q.defer();
            promise.resolve();
            return promise.promise;
        }
      };

      return open;
    }
  ]);
});
