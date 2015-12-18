// Helper object to manage modules
define(function (require) {
  'use strict';

  var _ = require('lodash-base');
  var $ = require('jquery');

  _.mixin({
    undefined : function(val) {
      return _.isUndefined(val) || _.isNull(val);
    },
    defined: function(val) {
      return !this.undefined(val);
    },
    concat: function(arrayLeft, arrayRight) {
      if (this.defined(arrayLeft) && this.defined(arrayRight)) {
        return arrayLeft.concat(arrayRight);
      } else if (this.defined(arrayRight) && this.undefined(arrayLeft)) {
        return arrayRight;
      } else if (this.defined(arrayLeft) && this.undefined(arrayRight)) {
        return arrayLeft;
      } else {
        return [];
      }
    },
    findByFieldValue: function(array, nameValueEntries) {
      for (var i = 0; i < array.length; i++) {
        var found;
        for ( var fieldName in nameValueEntries) {
          if (nameValueEntries.hasOwnProperty(fieldName)) {
            found = array[i].hasOwnProperty(fieldName) && array[i][fieldName] === nameValueEntries[fieldName];
            if (!found) {
              break;
            }
          }
        }
        if (found) {
          return i;
        }
      }
      return -1;
    },
    splitString: function(string, size) {
      var re = new RegExp('.{1,' + size + '}', 'g');
      return string.match(re);
    },
    safePush: function(object, fieldName, value) {
      if (object.hasOwnProperty(fieldName)) {
        object[fieldName].push(value);
      } else {
        object[fieldName] = [value];
      }
    },
    isNotEmpty: function(object) {
      return !_.isEmpty(object);
    },
    isImage: function(src) {
      var deferred = $.Deferred();
      var image = new Image();
      image.onerror = function() {
        deferred.resolve(false);
      };
      image.onload = function() {
        deferred.resolve(true);
      };
      image.src = src;
      return deferred;
    },
    // a very simplified fork of _.trunc to trunc the beginning of a string
    startTrunc: function(string, length) {
      var omission = '...';
      if (length >= string.length) {
        return string;
      }
      var end = length - omission.length;
      if (end < 1) {
        return omission;
      }
      var result = string.slice(string.length - end);
      return omission + result;
    },
    undefinedPath: function (object, path){
      return this.undefined(_.get(object, path));
    },
    definedPath: function (object, path){
      return !this.undefinedPath(object, path);
    }
  });
  return _;
});
