
angular.module('alienUiApp').directive('paginationDirective', function() {
  return {
    templateUrl : 'views/fragments/pagination_template.html',
    restrict : 'E',
    scope : {
      'paginationInfo' : '='
    }
  };
});