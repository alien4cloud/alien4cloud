'use strict';

var alienApp = angular.module('alienUiApp');
/**
 * Filter which will truncate long text until defined length, append by default
 * '...' at the end of the text
 */
alienApp.filter('truncate', function() {
  return function(text, length, end) {
    if (isNaN(length)) {
      length = 10;
    }

    if (UTILS.isUndefinedOrNull(end)) {
      end = "...";
    }

    if (UTILS.isUndefinedOrNull(text)) {
      return '';
    }

    if (!angular.isString(text)) {
      text = text.toString();
    }

    if (text.length <= length) {
      return text;
    } else {
      return String(text).substring(0, length - end.length) + end;
    }
  };
});

/**
 * Filter which will split a text by chunks of a given size to display it
 * and "join" all chunks by a "space" caracter : "chunk1 chunk2 chunk3"
 * Default split size : UTILS.maxNodeNameSplitSize
 */
alienApp.filter('split', function() {
  return function(text, length) {
    if (isNaN(length)) {
      length = UTILS.maxNodeNameSplitSize;
    }
    if (!UTILS.isUndefinedOrNull(text)) {
      if (!angular.isString(text)) {
        text = text.toString();
      }
      if (text.length <= length) {
        return text;
      } else {
        text = UTILS.splitString(text, length);
        return text.join('\n');
      }
    } else {
      return '';
    }

  };
});
