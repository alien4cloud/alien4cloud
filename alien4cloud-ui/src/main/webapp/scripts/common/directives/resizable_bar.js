define(function (require) {
  'use strict';

  var modules = require('modules');
  var a4cCommonModule = modules.get('a4c-common', ['ui.bootstrap']);

  /* HTML resize bar */
  var htmlResizeBar =
'<div class="topology-column-menu ui-resizable-handle ui-resizable-{{direction || \'w\'}} resizable-bar {{classes}}">' +
  '<img ng-src="images/splitter.png" class="splitter-icon">'+
'</div>';

  a4cCommonModule.directive('resizableBar', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        id: '@',
        direction: '@',
        classes: '@'
      },
      template: htmlResizeBar
    };
  });
});
