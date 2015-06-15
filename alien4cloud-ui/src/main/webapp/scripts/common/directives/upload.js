'use strict';

angular.module('alienUiApp').directive('uploadDirective', function() {
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
