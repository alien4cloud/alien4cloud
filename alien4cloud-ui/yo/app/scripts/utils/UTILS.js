'use strict';

var UTILS = {};

// Constants
UTILS.maxNodeNameDrawSize = 17;
UTILS.maxNodeNameSplitSize = 30;
UTILS.ALL_USERS_GROUP = 'ALL_USERS';

UTILS.compareArray = function(left, right) {
  // if the other array is a falsy value, return
  if (!right) {
    return false;
  }
  // compare lengths - can save a lot of time
  if (left.length !== right.length) {
    return false;
  }

  for (var i = 0, l = left.length; i < l; i++) {
    // Check if we have nested arrays
    if (left[i] instanceof Array && right[i] instanceof Array) {
      // recurse into the nested arrays
      if (!UTILS.compareArray(left[i], right[i])) {
        return false;
      }
    } else if (left[i] !== right[i]) {
      // Warning - two different object instances will never be equal: {x:20} !=
      // {x:20}
      return false;
    }
  }
  return true;
};

UTILS.isDefinedAndNotNull = function(val) {
  return angular.isDefined(val) && val !== null;
};

UTILS.isStringDefinedAndNotEmpty = function(val) {
  return UTILS.isDefinedAndNotNull(val) && val !== '';
};

UTILS.arrayContains = function(array, val) {
  return (UTILS.isDefinedAndNotNull(array) && array.indexOf(val) >= 0);
};

UTILS.isNotEmpty = function(array) {
  return (UTILS.isDefinedAndNotNull(array) && array.length > 0);
};

UTILS.isUndefinedOrNull = function(val) {
  return angular.isUndefined(val) || val === null;
};

UTILS.isArrayDefinedAndNotEmpty = function(array) {
  return UTILS.isDefinedAndNotNull(array) && array.length > 0;
};

UTILS.isObjectEmpty = function(object) {
  return Object.getOwnPropertyNames(object).length === 0;
};

UTILS.deepCopy = function(obj) {
  if (obj !== null) {
    var out = [];
    if (Object.prototype.toString.call(obj) === '[object Array]') {
      var len = obj.length;
      for (var i = 0; i < len; i++) {
        out[i] = UTILS.deepCopy(obj[i]);
      }
      return out;
    }
    if (typeof obj === 'object') {
      out = {};
      for ( var j in obj) {
        out[j] = UTILS.deepCopy(obj[j]);
      }
      return out;
    }
  }
  return obj;
};

UTILS.convertNameValueListToMap = function(list) {
  var map = {};
  for (var i = 0; i < list.length; i++) {
    map[list[i].name] = list[i].value;
  }
  return map;
};

UTILS.getIcon = function(tags) {
  for ( var i in tags) {
    var tag = tags[i];
    if (tag.name === 'icon') {
      return tag.value;
    }
  }
};

UTILS.isMapNotNullOrEmpty = function(map) {
  return angular.isDefined(map) && map !== null && Object.keys(map).length > 0;
};

var toLowerCase = function(text) {
  return text.charAt(0).toLowerCase() + text.slice(1);
};

var toUpperCase = function(text) {
  return text.charAt(0).toUpperCase() + text.slice(1);
};

UTILS.relationshipNameFromTypeAndTarget = function(type, targetName) {
  var tokens = type.split('.');
  if (tokens.length > 1) {
    return toLowerCase(tokens[tokens.length - 1]) + toUpperCase(targetName);
  } else {
    return toLowerCase(type) + toUpperCase(targetName);
  }
};

UTILS.getShortName = function(longName) {
  var tokens = longName.split('.');
  if (tokens.length > 0) {
    return tokens[tokens.length - 1];
  } else {
    return longName;
  }
};

UTILS.isFromNodeType = function(nodeType, type) {
  var superTypes = nodeType.derivedFrom;
  return nodeType.elementId === type || (UTILS.isDefinedAndNotNull(superTypes) && superTypes.indexOf(type) >= 0);
};

/* Split a string into chunks of the given size */
UTILS.splitString = function(string, size) {
  var re = new RegExp('.{1,' + size + '}', 'g');
  return string.match(re);
};

UTILS.concat = function(arrayLeft, arrayRight) {
  if (UTILS.isDefinedAndNotNull(arrayLeft) && UTILS.isDefinedAndNotNull(arrayRight)) {
    return arrayLeft.concat(arrayRight);
  } else if (UTILS.isDefinedAndNotNull(arrayRight) && UTILS.isUndefinedOrNull(arrayLeft)) {
    return arrayRight;
  } else if (UTILS.isDefinedAndNotNull(arrayLeft) && UTILS.isUndefinedOrNull(arrayRight)) {
    return arrayLeft;
  } else {
    return [];
  }
};

UTILS.arrayUnique = function(array) {
  var a = array.concat();
  for (var i = 0; i < a.length; ++i) {
    for (var j = i + 1; j < a.length; ++j) {
      if (a[i] === a[j]) {
        a.splice(j--, 1);
      }
    }
  }

  return a;
};

/**
 * Merge all properties from object 'from' into object 'into'
 * 
 * @param into
 * @param from
 * @returns {{}}
 */
UTILS.mergeObjects = function(from, into) {
  var merged = {};
  for ( var intoAttrName in into) {
    if (into.hasOwnProperty(intoAttrName)) {
      merged[intoAttrName] = into[intoAttrName];
    }
  }
  for ( var fromAttrName in from) {
    if (from.hasOwnProperty(fromAttrName)) {
      merged[fromAttrName] = from[fromAttrName];
    }
  }
  return merged;
};

UTILS.findByFieldValue = function(array, fieldName, fieldValue) {
  for (var i = 0; i < array.length; i++) {
    if (array[i].hasOwnProperty(fieldName) && array[i][fieldName] === fieldValue) {
      return i;
    }
  }
  return -1;
};

UTILS.findByFieldValues = function(array, nameValueEntries) {
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
};