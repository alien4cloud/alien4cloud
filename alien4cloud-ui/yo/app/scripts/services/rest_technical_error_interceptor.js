/* global UTILS */

'use strict';

angular.module('alienUiApp').factory('restTechnicalErrorInterceptor', ['$rootScope', '$q', '$window', 'toaster', '$translate',
  function($rootScope, $q, $window, toaster, $translate) {

    var extractErrorMessage = function(rejection) {
      if (UTILS.isDefinedAndNotNull(rejection.data)) {
        if (UTILS.isDefinedAndNotNull(rejection.data.error)) {
          var error = rejection.data.error;
          // Redirect to homepage when the user is not authenticated
          if (error.code === 100) {
            $window.location.href = '/';
          } else {
            return {
              status: rejection.status,
              data: UTILS.isDefinedAndNotNull(error.code)?'ERRORS.' + error.code : 'ERRORS.UNKNOWN'
            };
          }
        } else {
          // Error not defined ==> return the data part
          return {
            status: rejection.status,
            data: rejection.data
          };
        }
      } else {
        // rejection.data not defined ==> unknown error
        return {
          status: rejection.status,
          data: 'ERRORS.UNKNOWN'
        };
      }
    };

    return {
      'request': function(config) {

        // alienAuthService.getStatus().$promise.then(function(status) {
        //   console.log('');
        // });

        // do something on success
        console.log('REQUEST config : ', config);
        console.log('REQUEST config : ', config.cache);
        return config;
      },
      'responseError': function(rejection) {
        var error = extractErrorMessage(rejection);
        // Display the toaster message on top with 4000 ms display timeout
        // Don't shot toaster for "tour" guides
        if (rejection.config.url.indexOf('data/guides') < 0) {
          toaster.pop('error', $translate('ERRORS.INTERNAL') + ' - ' + error.status, $translate(error.data), 4000, 'trustedHtml', null);
        }
        return $q.reject(rejection);
      }
    };
  }
]);
