// Karma configuration
// http://karma-runner.github.io/0.10/config/configuration-file.html

module.exports = function(config) {

  config.set({
    // base path, that will be used to resolve files and exclude
    basePath: '',

    // testing framework to use (jasmine/mocha/qunit/...)
    frameworks: ['jasmine'],

    // list of files / patterns to load in the browser
    files: [
      'app/bower_components/d3/d3.js',
      'app/bower_components/d3-tip/index.js',
      'app/bower_components/dagre-d3/js/dagre-d3.js',
      'app/bower_components/jquery/dist/jquery.js',
      'app/bower_components/bootstrap-sass-official/assets/javascripts/bootstrap.js',
      'app/bower_components/angular/angular.js',
      'app/bower_components/angular-mocks/angular-mocks.js',
      'app/bower_components/ng-file-upload/angular-file-upload-shim.js',

      'app/bower_components/angular-resource/angular-resource.js',
      'app/bower_components/angular-cookies/angular-cookies.js',
      'app/bower_components/angular-sanitize/angular-sanitize.js',
      'app/bower_components/angular-route/angular-route.js',
      'app/bower_components/angular-ui-router/release/angular-ui-router.js',
      'app/bower_components/angular-translate/angular-translate.js',
      'app/bower_components/angular-translate-loader-static-files/angular-translate-loader-static-files.js',
      'app/bower_components/angular-bootstrap/ui-bootstrap-tpls.js',
      'app/bower_components/ng-file-upload/angular-file-upload.js',
      'app/bower_components/angular-xeditable/dist/js/xeditable.js',
      'app/bower_components/ace-builds/src-min-noconflict/ace.js',
      'app/bower_components/angular-ui-ace/ui-ace.js',
      'app/bower_components/js-yaml/js-yaml.js',
      'app/bower_components/angular-animate/angular-animate.min.js',
      'app/bower_components/angularjs-toaster/toaster.js',
      'app/bower_components/sockjs/sockjs.js',
      'app/bower_components/stomp-websocket/lib/stomp.js',

      'app/scripts/app.js',
      'app/scripts/filters/truncate_long_text.js',
      'app/scripts/filters/replace_string.js',
      'app/scripts/filters/inputs.js',

      'app/scripts/controllers/component_details.js',
      'app/scripts/controllers/component_list.js',
      'app/scripts/controllers/component_search.js',
      'app/scripts/controllers/search_relationship.js',
      'app/scripts/controllers/upload.js',
      'app/scripts/controllers/plugin.js',
      'app/scripts/controllers/properties.js',

      'app/scripts/admin/controllers/admin_layout.js',

      'app/scripts/applications/controllers/application.js',
      'app/scripts/applications/controllers/application_deployment.js',
      'app/scripts/applications/controllers/application_info.js',
      'app/scripts/applications/controllers/application_list.js',
      'app/scripts/applications/controllers/application_users.js',
      'app/scripts/applications/controllers/topology_runtime.js',

      'app/scripts/clouds/controllers/new_cloud.js',
      'app/scripts/clouds/controllers/cloud_list.js',
      'app/scripts/clouds/controllers/cloud_detail.js',
      'app/scripts/clouds/services/cloud_services.js',

      'app/scripts/csars/controllers/csar_list.js',
      'app/scripts/csars/controllers/csar_details.js',
      'app/scripts/csars/controllers/csar_component_details.js',

      'app/scripts/meta-props/controllers/tag_configuration_list.js',
      'app/scripts/meta-props/controllers/tag_configuration.js',
      'app/scripts/meta-props/services/tag_configuration_services.js',

      'app/scripts/topology/controllers/topology.js',
      'app/scripts/topology/controllers/topology_plan_graph_controller.js',
      'app/scripts/topology/directives/topology_rendering.js',
      'app/scripts/topology/services/tosca_service.js',
      'app/scripts/topology/services/node_tempalte_service.js',
      'app/scripts/topology/services/relationship_target_matcher_service.js',

      'app/scripts/topologytemplates/controllers/topology_template_list.js',
      'app/scripts/topologytemplates/controllers/topology_template.js',
      'app/scripts/topologytemplates/services/topology_template_service.js',

      'app/scripts/users/controllers/users.js',
      'app/scripts/users/controllers/users_directive_ctrl.js',
      'app/scripts/users/controllers/groups_directive_ctrl.js',

      'app/scripts/services/application_services.js',
      'app/scripts/services/component_services.js',
      'app/scripts/services/search_service_factory.js',
      'app/scripts/services/search_services.js',
      'app/scripts/services/rest_technical_error_interceptor.js',
      'app/scripts/services/quick_search_service.js',
      'app/scripts/services/topology_services.js',
      'app/scripts/services/topology_layout_services.js',
      'app/scripts/services/suggestion_services.js',
      'app/scripts/services/user_service.js',
      'app/scripts/services/group_services.js',
      'app/scripts/services/formdescriptor_services.js',
      'app/scripts/services/plugin_services.js',
      'app/scripts/services/websocket_services.js',
      'app/scripts/services/properties_services.js',

      'app/scripts/services/deployment_services.js',

      'app/scripts/graph_utils/services/coordinates_util_service.js',
      'app/scripts/graph_utils/services/svg_controls_service.js',
      'app/scripts/graph_utils/services/svg_service.js',

      'app/scripts/utils/services/browser_service.js',
      'app/scripts/utils/services/resize_services.js',
      'app/scripts/utils/services/runtime_colors_service.js',

      'app/scripts/directives/drag_drop.js',
      'app/scripts/directives/search.js',
      'app/scripts/directives/generic_form.js',
      'app/scripts/directives/upload.js',
      'app/scripts/directives/pagination.js',
      'app/scripts/directives/property_display.js',
      'app/scripts/directives/delete_confirm.js',

      'app/scripts/utils/UTILS.js',
      'app/scripts/utils/BoundingBox.js',
      'app/scripts/utils/CONNECTORS.Grid.js',
      'app/scripts/utils/CONNECTORS.Point.js',
      'app/scripts/utils/CoordinateUtils.js',
      'app/scripts/utils/SvgControls.js',
      'app/scripts/utils/D3JS_UTILS.js',
      'app/scripts/utils/TopologySvg.js',

      'app/scripts/authentication/services/authservices.js',
      'app/scripts/authentication/controllers/navbar.js',
      'app/scripts/authentication/directives/navbar.js',
      'app/scripts/authentication/services/navbar.js',
      'app/scripts/authentication/services/userServices.js',

      'app/bower_components/jasmine-jquery/lib/jasmine-jquery.js', {
        pattern: 'test/mock/*.json',
        watched: true,
        served: true,
        included: false
      },
      'test/unit/**/*.js'
    ],

    // list of files / patterns to exclude
    exclude: [],

    // web server port
    port: 8080,

    // level of logging
    // possible values: LOG_DISABLE || LOG_ERROR || LOG_WARN || LOG_INFO || LOG_DEBUG
    logLevel: config.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,


    // Start these browsers, currently available:
    // - Chrome
    // - ChromeCanary
    // - Firefox
    // - Opera
    // - Safari (only Mac)
    // - PhantomJS
    // - IE (only Windows)
    browsers: ['Chrome'],


    // Continuous Integration mode
    // if true, it capture browsers, run tests and exit
    singleRun: false
  });
};
