/* global UTILS */
'use strict';

angular.module('alienUiApp').factory('restTechnicalErrorInterceptor', ['$rootScope', '$q', '$window', 'toaster', '$translate', '$timeout', '$location',
  function ($rootScope, $q, $window, toaster, $translate, $timeout, $location) {

    var extractErrorMessage = function (rejection) {
      if (UTILS.isDefinedAndNotNull(rejection.data)) {
        if (UTILS.isDefinedAndNotNull(rejection.data.error)) {
          var error = rejection.data.error;
          // Redirect to homepage when the user is not authenticated
          if (error.code === 100) {
            $window.location.href = '/';
          } else {
            return {
              status: rejection.status,
              data: UTILS.isDefinedAndNotNull(error.code) ? 'ERRORS.' + error.code : 'ERRORS.UNKNOWN',
              code: error.code 
            };
          }
        } else {
          // Error not defined ==> return the data part
          return {
            status: rejection.status,
            data: rejection.data,
          };
        }
      } else {
        // rejection.data not defined ==> unknown error
        return {
          status: rejection.status,
          data: 'ERRORS.UNKNOWN',
        };
      }
    };

    return {

      'responseError': function (rejection) {
        
        var error = extractErrorMessage(rejection);

        // case: authentication rejection error
        // NOTE: separate simple unauthorized from authentication failure (wrong login provided)
        if (rejection.status === 401 && error.code !== 101) {
          // full page reload (not only url change)
          $location.path('/restricted');
          toaster.pop('error', $translate('ERRORS.100'), $translate('ERRORS.' + rejection.status), 6000, 'trustedHtml', null);
          $timeout(function redirect() {
            $window.location.href = '/';
          }, 6000);
        } else {
          // case : backend specific error
          // Display the toaster message on top with 4000 ms display timeout
          // Don't shot toaster for "tour" guides
          if (rejection.config.url.indexOf('data/guides') < 0) {
            toaster.pop('error', $translate('ERRORS.INTERNAL') + ' - ' + error.status, $translate(error.data), 4000, 'trustedHtml', null);
          }
        }
        return $q.reject(rejection);
      }
    };
  }
]);
