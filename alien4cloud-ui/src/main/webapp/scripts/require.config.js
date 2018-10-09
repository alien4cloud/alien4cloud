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

    // views packaging file
    'a4c-templates': 'views/alien4cloud-templates',

    // dependencies loading file (used to package all dependencies).
    'a4c-dependencies': 'scripts/alien4cloud-dependencies',
    // bower_components
    'lodash-base': 'bower_components/lodash/lodash.min',
    'jquery': 'bower_components/jquery/dist/jquery.min',
    'angular': 'bower_components/angular/angular.min',
    'angular-cookies': 'bower_components/angular-cookies/angular-cookies.min',
    'angular-bootstrap': 'bower_components/angular-bootstrap/ui-bootstrap-tpls',
    'angular-bootstrap-datetimepicker-template': 'bower_components/angular-bootstrap-datetimepicker/src/js/datetimepicker',
    'angular-bootstrap-datetimepicker': 'bower_components/angular-bootstrap-datetimepicker/src/js/datetimepicker.templates',
    'moment': 'bower_components/moment/min/moment.min',
    'angular-moment': 'bower_components/angular-moment/angular-moment.min',
    'angular-resource': 'bower_components/angular-resource/angular-resource.min',
    'angular-sanitize': 'bower_components/angular-sanitize/angular-sanitize.min',
    'angular-ui-router': 'bower_components/angular-ui-router/release/angular-ui-router.min',
    'angular-translate-base': 'bower_components/angular-translate/angular-translate.min',
    'angular-translate': 'bower_components/angular-translate-loader-static-files/angular-translate-loader-static-files.min',
    'angular-translate-storage-cookie' : 'bower_components/angular-translate-storage-cookie/angular-translate-storage-cookie.min',
    'angular-animate': 'bower_components/angular-animate/angular-animate.min',
    'angular-xeditable': 'bower_components/angular-xeditable/dist/js/xeditable.min',
    'angular-ui-select': 'bower_components/angular-ui-select/dist/select.min',
    'angular-tree-control': 'bower_components/angular-tree-control/angular-tree-control',
    'ng-table': 'bower_components/ng-table/dist/ng-table.min',
    'autofill-event': 'bower_components/autofill-event/src/autofill-event',
    'toaster': 'bower_components/angularjs-toaster/toaster',
    'hopscotch': 'bower_components/hopscotch/dist/js/hopscotch.min',
    'angular-file-upload-shim': 'bower_components/ng-file-upload/ng-file-upload-shim.min',
    'angular-file-upload': 'bower_components/ng-file-upload/ng-file-upload.min',
    'angular-ui-ace': 'bower_components/angular-ui-ace/ui-ace.min',
    'angular-hotkeys': 'bower_components/angular-hotkeys/build/hotkeys.min',
    'ace': 'bower_components/ace-builds/src-min-noconflict/ace',
    'sockjs': 'bower_components/sockjs/sockjs.min',
    'stomp': 'bower_components/stomp-websocket/lib/stomp.min',
    'd3': 'bower_components/d3/d3.min',
    'd3-tip': 'bower_components/d3-tip/index',
    'd3-pie': 'bower_components/d3pie/d3pie/d3pie.min',
    'dagre': 'bower_components/dagre/dist/dagre.core.min',
    'graphlib': 'bower_components/graphlib/dist/graphlib.core.min',
    'js-yaml': 'bower_components/js-yaml/js-yaml.min',
    'clipboard' : 'bower_components/clipboard/dist/clipboard.min',
  },
  shim: {
    'angular': {
      deps: ['jquery'],
      exports: 'angular'
    },
    'angular-cookies': { deps: ['angular'] },
    'angular-bootstrap': { deps: ['angular'] },
    'angular-bootstrap-datetimepicker': { deps: ['angular-bootstrap-datetimepicker-template', 'angular-bootstrap', 'moment'] },
    'angular-resource': { deps: ['angular'] },
    'angular-sanitize': { deps: ['angular'] },
    'angular-ui-router': { deps: ['angular'] },
    'angular-translate-base': { deps: ['angular'] },
    'angular-translate-storage-cookie' : { deps: ['angular'] },
    'angular-translate': { deps: ['angular-translate-base' , 'angular-translate-storage-cookie'] },
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
    'd3-pie': { deps: ['d3'] },
    'clipboard': {
      exports: 'ClipboardJS',
      deps: ['jquery']
    },
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
