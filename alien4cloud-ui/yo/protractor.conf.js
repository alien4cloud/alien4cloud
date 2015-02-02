// A reference configuration file.
exports.config = {
  // ----- How to setup Selenium -----
  //
  // There are three ways to specify how to use Selenium. Specify one of the
  // following:
  //
  // 1. seleniumServerJar - to start Selenium Standalone locally.
  // 2. seleniumAddress - to connect to a Selenium server which is already
  //    running.
  // 3. sauceUser/sauceKey - to use remote Selenium servers via SauceLabs.
  //
  // If the chromeOnly option is specified, no Selenium server will be started,
  // and chromeDriver will be used directly (from the location specified in
  // chromeDriver)

  // The location of the selenium standalone server .jar file, relative
  // to the location of this config. If no other method of starting selenium
  // is found, this will default to
  // node_modules/protractor/selenium/selenium-server...
  seleniumServerJar: null,
  // The port to start the selenium server on, or null if the server should
  // find its own unused port.
  seleniumPort: null,
  // Chromedriver location is used to help the selenium standalone server
  // find chromedriver. This will be passed to the selenium jar as
  // the system property webdriver.chrome.driver. If null, selenium will
  // attempt to find chromedriver using PATH.
  // chromeDriver: '/usr/local/lib/node_modules/protractor/selenium/chromedriver',
  // If true, only chromedriver will be started, not a standalone selenium.
  // Tests for browsers other than chrome will not run.
  chromeOnly: false,
  // Additional command line options to pass to selenium. For example,
  // if you need to change the browser timeout, use
  // seleniumArgs: ['-browserTimeout=60'],
  seleniumArgs: [],

  // If sauceUser and sauceKey are specified, seleniumServerJar will be ignored.
  // The tests will be run remotely using SauceLabs.
  sauceUser: null,
  sauceKey: null,

  // The address of a running selenium server. If specified, Protractor will
  // connect to an already running instance of selenium. This usually looks like

  // Depends on grunt-protractor-webdriver (default setting for selenium server)
  seleniumAddress: 'http://127.0.0.1:4444/wd/hub',
  // seleniumAddress: null,

  // The timeout for each script run on the browser. This should be longer
  // than the maximum time your application needs to stabilize between tasks.
  allScriptsTimeout: 110000,

  // ----- What tests to run -----
  //
  // Spec patterns are relative to the location of this config.
  specs: [
    // No data in ALIEN at this point, only default admin user should exist.
    'test/e2e/setup-scenario/before-all.js',
    'test/e2e/scenarios/**/*.js'

    // 'test/e2e/scenarios/homepage.js',
    // 'test/e2e/scenarios/language_test.js',
    // 'test/e2e/scenarios/authentication.js',
    // 'test/e2e/scenarios/admin_users_management.js',
    // 'test/e2e/scenarios/security_users.js',
    // 'test/e2e/scenarios/admin_groups_management.js',
    // 'test/e2e/scenarios/plugins.js',
    // 'test/e2e/scenarios/admin_metaprops_configuration.js',
    // 'test/e2e/scenarios/admin_cloud.js'
    // 'test/e2e/scenarios/security_clouds.js',

    // 'test/e2e/scenarios/csar_upload.js',
    // 'test/e2e/scenarios/component_details.js',
    // 'test/e2e/scenarios/component_details_tags.js',

    // 'test/e2e/scenarios/topology_template.js',

    // 'test/e2e/scenarios/application.js',
    // 'test/e2e/scenarios/application_metaprops.js',
    // 'test/e2e/scenarios/application_tags.js'

    // 'test/e2e/scenarios/application_topology_editor_nodetemplate.js',
    // 'test/e2e/scenarios/application_topology_editor_relationships.js',
    // 'test/e2e/scenarios/application_topology_editor_replacenode.js',
    // 'test/e2e/scenarios/application_topology_editor_editrelationshipname.js',
    // 'test/e2e/scenarios/application_topology_editor_editrequiredprops.js',
    // 'test/e2e/scenarios/application_topology_editor_multiplenodeversions.js',
    // 'test/e2e/scenarios/application_topology_editor_input_output.js',
    // 'test/e2e/scenarios/application_topology_runtime.js',
    // 'test/e2e/scenarios/application_security.js',

    // 'test/e2e/scenarios/deployment.js',

    // 'test/e2e/scenarios/quick_search.js',
  ],

  // Patterns to exclude.
  exclude: [],

  // ----- Capabilities to be passed to the webdriver instance ----
  //
  // For a full list of available capabilities, see
  // https://code.google.com/p/selenium/wiki/DesiredCapabilities
  // and
  // https://code.google.com/p/selenium/source/browse/javascript/webdriver/capabilities.js
  capabilities: {
    'browserName': 'chrome',
    'chromeOptions': {
      'args': ['--no-sandbox']
    }
  },

  // ----- More information for your tests ----
  //
  // A base URL for your application under test. Calls to protractor.get()
  // with relative paths will be prepended with this.
  // Depends on the cargo maven configuration
  baseUrl: 'http://localhost:8088/',

  // Selector for the element housing the angular app - this defaults to
  // body, but is necessary if ng-app is on a descendant of <body>
  rootElement: 'body',

  // A callback function called once protractor is ready and available, and
  // before the specs are executed
  // You can specify a file containing code to run by setting onPrepare to
  // the filename string.
  onPrepare: function() {
    // At this point, global 'protractor' object will be set up, and jasmine
    // will be available. For example, you can add a Jasmine reporter with:
    require('jasmine-reporters');
    jasmine.getEnv().addReporter(new jasmine.JUnitXmlReporter('outputdir/', true, true));
  },

  // ----- The test framework -----
  //
  // Jasmine is fully supported as a test and assertion framework.
  framework: 'jasmine',

  // ----- Options to be passed to minijasminenode -----
  //
  // See the full list at https://github.com/juliemr/minijasminenode
  jasmineNodeOpts: {
    // onComplete will be called just before the driver quits.
    onComplete: null,
    // If true, display spec names.
    isVerbose: true,
    // If true, print colors to the terminal.
    showColors: true,
    // If true, include stack traces in failures.
    includeStackTrace: true,
    // Default time to wait in ms before a test fails.
    defaultTimeoutInterval: 110000
  },

  // ----- The cleanup step -----
  //
  // A callback function called once the tests have finished running and
  // the webdriver instance has been shut down. It is passed the exit code
  // (0 if the tests passed or 1 if not).
  onCleanUp: function() {}
};
