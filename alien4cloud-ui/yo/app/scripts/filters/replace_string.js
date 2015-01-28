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

/**
 * Filter that eventually add a prefix if the text is not blank.
 */
alienApp.filter('prefix', function() {
  return function(text, prefixString) {
    if (UTILS.isUndefinedOrNull(text) || text.length == 0) {
      return '';
    }
    return prefixString + text;
  };
});

/**
 * Filter that eventually add a suffix if the text is not blank.
 */
alienApp.filter('suffix', function() {
  return function(text, suffixString) {
    if (UTILS.isUndefinedOrNull(text) || text.length == 0) {
      return '';
    }
    return text + suffixString;
  };
});