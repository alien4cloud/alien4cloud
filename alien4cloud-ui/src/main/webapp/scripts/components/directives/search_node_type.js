define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('scripts/components/controllers/component_search');

  modules.get('a4c-components').directive('alienSearchNodeType', ['$interval', function ($interval) {
    return {
      templateUrl: 'views/components/search_node_type_template.html',
      restrict: 'E',
      controller: 'alienSearchComponentCtrl',
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
      link: {
        pre: function (scope, element) {
          scope.queryComponentType = 'NODE_TYPE';
          function resize() {
            if(_.undefined(scope.heightInfo) ){
              return;
            }
            scope.searchHeight = scope.heightInfo.height - element.position().top - 2;
            if (scope.globalContext) {
              scope.searchStyle = 'overflow: auto; margin-bottom: 0px; height: ' + scope.searchHeight + 'px;';
              scope.resultStyle = 'overflow: auto; margin-bottom: 0px; height: ' + (scope.searchHeight - 75) + 'px;';
              // scope.listHeight = scope.searchHeight;
            } else {
              scope.searchStyle = '';
              scope.resultStyle = 'overflow: auto; margin-bottom: 0px; max-height: ' + (scope.searchHeight) + 'px;';
              // scope.listHeight = scope.searchHeight - 42;
            }
          }

          scope.$watch('heightInfo', function () {
            resize();
          });
          scope.$watch('widthInfo', function () {
            resize();
          });

          $interval(resize, 1000, 1);
        }
      }
    };
  }]);
});
