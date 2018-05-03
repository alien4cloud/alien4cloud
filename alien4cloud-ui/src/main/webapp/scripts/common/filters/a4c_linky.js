define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var alienCommonModule = modules.get('a4c-common');
  var angular = require('angular');

  /**
  **
  ** This filter returns a HTML String.
  ** You might want to use this with a4cCompile directive like this:
  ** <span a4c-compile="text | a4cLinky:'clickCallbackName'"></span>
  **
  **/
  alienCommonModule.filter('a4cLinky', [ function() {

    //TODO make this REGEXP overridable via filter params
    var REGEXP = /\$\{([^}]+)\}/;

    var linkyMinErr = angular.$$minErr('a4cLinky');

    /**
    ** @param text: The text to linkify
    ** @param clickCallbackName: name of the ng-click callback. Should take a String representing the matched expression to link
    ** @param attributes: object ==> key:value or attributes to add to the <a></a> links
    ** attributes could also be a function. In that case, will take the matched expression as param
    **
    **/
    return function(text, clickCallbackName, attributes) {
      if (_.undefined(text) || text === '') {return text;}
      if (!_.isString(text)) {throw linkyMinErr('notstring', 'Expected string but received: {0}', text);}

      var attributesFn =
        _.isFunction(attributes) ? attributes :
        _.isObject(attributes) ? function getAttributesObject() {return attributes;} :
        function getEmptyAttributesObject() {return {};};

      var match;
      var raw = text;
      var html = [];
      var i;

      function addText(text) {
        if (!text) {
          return;
        }
        html.push(text);
      }

      function addLink(matched, text) {
        var key, linkAttributes = attributesFn(matched);
        html.push('<a ');

        for (key in linkAttributes) {
          html.push(key + '="' + linkAttributes[key] + '" ');
        }

        html.push('ng-click="',
                  clickCallbackName,
                  '(\'',
                  matched,
                  '\')',
                  '">');
        addText(text);
        html.push('</a>');
      }
      while ((match = raw.match(REGEXP))) {
        // We can not end in these as they are sometimes found at the end of the sentence
        i = match.index;
        addText(raw.substr(0, i));
        addLink(match[1], match[0]);
        raw = raw.substring(i + match[0].length);
      }
      addText(raw);
      return html.join('');
    };
  }]);

  alienCommonModule.directive('a4cCompile', ['$compile', function ($compile) {
    return function(scope, element, attrs) {
      scope.$watch(
        function(scope) {
           // watch the 'a4c-compile' expression for changes
          return scope.$eval(attrs.a4cCompile);
        },
        function(value) {
          // when the 'a4c-compile' expression changes
          // assign it into the current DOM
          element.html(value);

          // compile the new DOM and link it to the current
          // scope.
          // NOTE: we only compile .childNodes so that
          // we don't get into infinite loop compiling ourselves
          $compile(element.contents())(scope);
        }
      );
    };
  }]);
});
