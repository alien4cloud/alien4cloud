define(function (require) {
  'use strict';


  require('jquery');
  require('bower_components/jquery-ui/ui/resizable');
  require('lodash-base');
  require('angular');
  require('angular-cookies');
  require('angular-bootstrap');
  require('moment');
  require('angular-moment');
  require('angular-bootstrap-datetimepicker-template');
  require('angular-bootstrap-datetimepicker');
  require('angular-resource');
  require('angular-sanitize');
  require('angular-ui-router');
  require('angular-translate-base');
  require('angular-translate');
  require('angular-translate-storage-cookie');
  require('angular-animate');
  require('angular-xeditable');
  require('angular-ui-select');
  require('angular-tree-control');
  require('ng-table');
  require('autofill-event');
  require('toaster');
  require('hopscotch');
  require('angular-file-upload-shim');
  require('angular-file-upload');
  require('angular-ui-ace');
  require('angular-hotkeys');
  require('ace');
  require('sockjs');
  require('stomp');
  require('d3');
  require('d3-tip');
  require('d3-pie');
  require('dagre');
  require('graphlib');
  require('clipboard');

  return function() {
    console.debug('Dependencies loaded');
  };
});
