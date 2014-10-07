
angular.module('alienUiApp').directive('uploadDirective', function($compile) {
  return {
    templateUrl : 'views/fragments/upload_template.html',
    restrict : 'E',
    scope : {
      'targetUrl' : '=',
      'dragAndDropMessage' : '=',
      'buttonMessage' : '=',
      'uploadSuccessCallback': '&'
    }
  };
});