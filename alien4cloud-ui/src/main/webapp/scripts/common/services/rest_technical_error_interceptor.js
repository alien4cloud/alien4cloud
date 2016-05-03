// Helper object to manage modules
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('alien4cloud', ['toaster', 'pascalprecht.translate'])
    .factory('restTechnicalErrorInterceptor',
    ['$rootScope', '$q', '$window', 'toaster', '$translate', '$timeout', '$location',
    function ($rootScope, $q, $window, toaster, $translate, $timeout, $location) {

      var extractErrorMessage = function (rejection) {
        if (_.defined(rejection.data)) {
          if (_.defined(rejection.data.error)) {
            var error = rejection.data.error;
            // Redirect to homepage when the user is not authenticated
            if (error.code === 100) {
              $window.location.href = '/';
            } else {
              return {
                status: rejection.status,
                data: _.defined(error.code) ? 'ERRORS.' + error.code : 'ERRORS.UNKNOWN',
                message: error.message,
                stacktrace: rejection.data.data,
                code: error.code
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
        'responseError': function (rejection) {

          var error = extractErrorMessage(rejection);

          // case: authentication rejection error
          // NOTE: separate simple unauthorized from authentication failure (wrong login provided)
          if (rejection.status === 401 && error.code !== 101) {
            // full page reload (not only url change)
            $location.path('/restricted');
            toaster.pop('error', $translate.instant('ERRORS.100'), $translate.instant('ERRORS.' + rejection.status), 6000, 'trustedHtml');
            $timeout(function redirect() {
              $window.location.href = '/';
            }, 6000);
          } else {
            // case : backend specific error
            // Display the toaster message on top with 4000 ms display timeout
            // Don't shot toaster for "tour" guides
            if (rejection.config.url.indexOf('data/guides') < 0) {
              var toasterBody;
              if(_.defined(error.message)) {
                toasterBody = '<div>'+$translate.instant(error.data)+
                  '</div><div>'+error.message+'</div>';
                if(_.defined(error.stacktrace)) {
                  console.error('Server error details', error.message, error.stacktrace);
                }
              } else {
                toasterBody = $translate.instant(error.data);
              }
              toaster.pop('error', $translate.instant('ERRORS.INTERNAL') + ' - ' + error.status, toasterBody, 4000, 'trustedHtml');
            }
          }
          return $q.reject(rejection);
        }
      };
    }
  ]);
});
