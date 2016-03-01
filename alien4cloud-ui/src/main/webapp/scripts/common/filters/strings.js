define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  var alienCommonModule = modules.get('a4c-common');

  /**
   * Filter which will transform the word 'unbounded' into infinite html code
   */
  alienCommonModule.filter('replaceAll', function() {
    return function(text, toReplaceString, replacementString) {
      if (_.undefined(toReplaceString)) {
        return text;
      }

      if (_.undefined(replacementString)) {
        replacementString = '';
      }

      if (_.undefined(text)) {
        return '';
      }

      var re = new RegExp(toReplaceString, 'g');
      return String(text).replace(re, replacementString);
    };
  }); // filter

  /**
   * Filter that eventually add a prefix if the text is not blank.
   */
  alienCommonModule.filter('prefix', function() {
    return function(text, prefixString) {
      if (_.undefined(text) || text.length === 0) {
        return '';
      }
      return prefixString + text;
    };
  }); // filter

  /**
   * Filter that eventually add a suffix if the text is not blank.
   */
  alienCommonModule.filter('suffix', function() {
    return function(text, suffixString) {
      if (_.undefined(text) || text.length === 0) {
        return '';
      }
      return text + suffixString;
    };
  }); // filter

  /**
   * Filter return a full string with each string element in the array concated
   * a2s : array to string
   */
  alienCommonModule.filter('a2s', function() {
    return function(stringArray, separator) {
      if (!(stringArray instanceof Array)) {
        return stringArray;
      }
      return stringArray.join(separator + ' ');
    };
  });

  /**
   * Filter which will truncate long text until defined length, append by default
   * '...' at the end of the text
   */
  alienCommonModule.filter('truncate', function() {
    return function(text, length, end) {
      if (isNaN(length)) {
        length = 10;
      }

      if (_.undefined(end)) {
        end = '...';
      }

      if (_.undefined(text)) {
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
  }); // filter

  /**
   * Filter which will truncate long text until defined length, append by default
   * '...' at the end of the text
   */
  alienCommonModule.filter('truncateHead', function() {
    return function(text, length) {
      return _.startTrunc(text, length);
    };
  }); // filter

  /**
   * Filter which will split a text by chunks of a given size to display it
   * and "join" all chunks by a "space" caracter : "chunk1 chunk2 chunk3"
   */
  alienCommonModule.filter('split', function() {
    var defaultSplitSize = 30;
    return function(text, length) {
      if (isNaN(length)) {
        length = defaultSplitSize;
      }
      if (_.defined(text)) {
        if (!angular.isString(text)) {
          text = text.toString();
        }
        if (text.length <= length) {
          return text;
        } else {
          text = _.splitString(text, length);
          return text.join('\n');
        }
      } else {
        return '';
      }
    };
  }); // filter

  /**
   * Filter which will explode a text by a given parameter and return
   * one of the chunk identified by its index
   * indexToReturn : starts at 0 for the first element
   */
  alienCommonModule.filter('splitAndGet', function() {
    return function(text, separator, indexToReturn) {
      if (!_.undefined(text)) {
        separator = separator || '.'; // by defult . if not defined
        if (text.indexOf(separator) < 0) {
          return text;
        }
        var res = text.split(separator);
        if(indexToReturn === 'last') {
          indexToReturn = res.length - 1;
        }
        if (parseInt(indexToReturn) && indexToReturn >= 0) {
          return res[indexToReturn];
        }
      } else {
        return;
      }
    };
  }); // filter

  /**
  * Filter that replace all characters of a password text with a replacement character
  */
  alienCommonModule.filter('password', function() {
    return function(password, car) {
      car = car || '*';
      var staredPassword = '';
      if (_.defined(password)) {
        for (var i = 0; i < password.length; i++) {
          staredPassword += car;
        }
      }
      return staredPassword;
    };
  }); // filter

}); // define
