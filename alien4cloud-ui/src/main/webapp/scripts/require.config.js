require.config({
  baseUrl: './',
  waitSeconds: 120,
  paths: {
    'a4c-bootstrap': 'scripts/alien4cloud-bootstrap',
    'alien4cloud': 'scripts/alien4cloud',
    'a4c-native': 'scripts/a4c-native',
    'states': 'scripts/utils/states_manager',
    'modules': 'scripts/utils/modules_manager',
    'plugins': 'scripts/utils/plugins_manager',
    'lodash': 'scripts/utils/alien_lodash',

    // bower_components
    'lodash-base': 'bower_components/lodash/lodash.min',
    'jquery': 'bower_components/jquery/dist/jquery.min',
    'jquery-ui-resizable': 'bower_components/jquery-ui/ui/resizable',
    'angular': 'bower_components/angular/angular.min',
    'angular-cookies': 'bower_components/angular-cookies/angular-cookies.min',
    'angular-bootstrap': 'bower_components/angular-bootstrap/ui-bootstrap-tpls.min',
    'angular-bootstrap-datetimepicker': 'bower_components/angular-bootstrap-datetimepicker/src/js/datetimepicker',
    'moment': 'bower_components/moment/min/moment.min',
    'angular-resource': 'bower_components/angular-resource/angular-resource.min',
    'angular-sanitize': 'bower_components/angular-sanitize/angular-sanitize.min',
    'angular-ui-router': 'bower_components/angular-ui-router/release/angular-ui-router.min',
    'angular-translate-base': 'bower_components/angular-translate/angular-translate.min',
    'angular-translate': 'bower_components/angular-translate-loader-static-files/angular-translate-loader-static-files.min',
    'angular-animate': 'bower_components/angular-animate/angular-animate.min',
    'angular-xeditable': 'bower_components/angular-xeditable/dist/js/xeditable.min',
    'angular-ui-select': 'bower_components/angular-ui-select/dist/select.min',
    'angular-tree-control': 'bower_components/angular-tree-control/angular-tree-control',
    'ng-table': 'bower_components/ng-table/dist/ng-table.min',
    'autofill-event': 'bower_components/autofill-event/src/autofill-event',
    'toaster': 'bower_components/angularjs-toaster/toaster',
    'hopscotch': 'bower_components/hopscotch/dist/js/hopscotch.min',
    'angular-file-upload-shim': 'bower_components/ng-file-upload/angular-file-upload-shim.min',
    'angular-file-upload': 'bower_components/ng-file-upload/angular-file-upload.min',
    'angular-ui-ace': 'bower_components/angular-ui-ace/ui-ace.min',
    'angular-hotkeys': 'bower_components/angular-hotkeys/build/hotkeys.min',
    'ace': 'bower_components/ace-builds/src-min-noconflict/ace',
    'sockjs': 'bower_components/sockjs/sockjs.min',
    'stomp': 'bower_components/stomp-websocket/lib/stomp.min',
    'd3': 'bower_components/d3/d3.min',
    'd3-tip': 'bower_components/d3-tip/index',
    'd3-pie': 'bower_components/d3pie/d3pie/d3pie.min',
    'dagre': 'bower_components/dagre/dist/dagre.core.min',
    'graphlib': 'bower_components/graphlib/dist/graphlib.core.min'
  },
  shim: {
    'jquery-ui-resizable': {deps: ['jquery']},
    'angular': {
      deps: ['jquery'],
      exports: 'angular'
    },
    'angular-cookies': { deps: ['angular'] },
    'angular-bootstrap': { deps: ['angular'] },
    'angular-bootstrap-datetimepicker': { deps: ['angular-bootstrap', 'moment'] },
    'angular-resource': { deps: ['angular'] },
    'angular-sanitize': { deps: ['angular'] },
    'angular-ui-router': { deps: ['angular'] },
    'angular-translate-base': { deps: ['angular'] },
    'angular-translate': { deps: ['angular-translate-base'] },
    'angular-hotkeys': { deps: ['angular'] },
    'autofill-event': { deps: ['angular'] },
    'angular-all': { deps: ['angular-cookies', 'angular-translate', 'angular-ui-router', 'angular-sanitize', 'angular-resource', 'angular-bootstrap', 'angular-bootstrap-datetimepicker', 'angular-cookies'] },
    'ng-table': { deps: ['angular'] },
    'toaster': { deps: ['angular-animate'] },
    'angular-animate': { deps: ['angular'] },
    'angular-xeditable': { deps: ['angular'] },
    'angular-ui-select': { deps: ['angular'] },
    'angular-file-upload': { deps: ['angular', 'angular-file-upload-shim'] },
    'angular-ui-ace': { deps: ['angular', 'ace'] },
    'angular-tree-control': { deps: ['angular'] },
    'stomp':  { deps: ['sockjs'] },
    'graphlib': { deps: ['lodash-base'] },
    'dagre': { deps: ['graphlib'] },
    'd3-pie': { deps: ['d3'] }
  },
  onNodeCreated: function(node, config, moduleName, url) {
    'use strict';
    if(window.alienLoadingBar) {
      window.alienLoadingCount += 1;
      window.alienLoadingBar.style.width = window.alienLoadingCount * 100 / window.alienLoadingExpected + '%';
      window.alienLoadingFile.innerHTML = url;
    }
  }
});
