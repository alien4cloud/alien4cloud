/** Highlight some part of a text and apply the given css class */
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common').filter('a4cHighlight', ['$sce', function($sce) {
    return function(text, highlighted, cssClass) {
      if(highlighted) {
        var pattern = new RegExp(highlighted);
        return $sce.trustAsHtml(text.replace(pattern, '<span class="' + cssClass + '">' + highlighted + '</span>'));
      }
      return text;
    };
  }]);
});
