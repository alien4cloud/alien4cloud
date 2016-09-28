define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/components/controllers/component_search');

  modules.get('a4c-components').directive('alienSearchNodeType', ['$interval', function($interval) {
    return {
      templateUrl: 'views/components/search_node_type_template.html',
      restrict: 'E',
      scope: {
        'refresh': '=',
        'displayDetailPannel': '=',
        'onSelectItem': '&',
        'globalContext': '=',
        'dragAndDropEnabled': '=',
        'heightInfo': '=',
        'widthInfo': '=',
        'defaultFilters': '=',
        'staticFacets': '=',
        'badges': '='
      },
      link: function(scope, element) {
        scope.queryComponentType = 'NODE_TYPE';

        function resize() {
          scope.searchHeight = scope.heightInfo.height - element.offset().top -2;
          if(scope.globalContext) {
            scope.searchStyle = 'overflow: auto; margin-bottom: 0px; height: ' + scope.searchHeight + 'px;';
          } else {
            scope.searchStyle = '';
          }
          scope.listHeight = scope.searchHeight - 72;
        }
        scope.$watch('heightInfo', function() {
          resize();
        });
        scope.$watch('widthInfo', function() {
          resize();
        });

        $interval(resize, 10, 1);
      }
    };
  }]);
});
