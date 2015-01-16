/* global UTILS */

'use strict';

var alienApp = angular.module('alienUiApp');
/**
 * Filter which will transform the word 'unbounded' into infinite html code
 */
alienApp.filter('replaceAll', function() {
  return function(text, toReplaceString, replacementString) {
    if (UTILS.isUndefinedOrNull(toReplaceString)) {
      return text;
    }

    if (UTILS.isUndefinedOrNull(replacementString)) {
      replacementString = '';
    }

    if (UTILS.isUndefinedOrNull(text)) {
      return '';
    }

    var re = new RegExp(toReplaceString, 'g');
    return String(text).replace(re, replacementString);
  };
});
