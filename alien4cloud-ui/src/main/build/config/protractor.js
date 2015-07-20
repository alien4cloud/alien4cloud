'use strict';

//
module.exports = function (grunt, options) {
  return {
    // Options :shared configuration with other target
    options: {
      configFile: 'src/test/webapp/protractor.conf.js', // Default config file
      keepAlive: false, // If false, the grunt process stops when the test
      // fails.
      noColor: false, // If true, protractor will not use colors in its
      // output.
      singleRun: false,
      args: {
        // Arguments passed to the command
      }
    },
    runLocalserver: {
      options: {
        args: {
          capabilities: {
            'browserName': 'chrome'
          },
          // baseUrl: 'http://localhost:9999',
          baseUrl: 'http://127.0.0.1:9999',
          specs: [
              '<%= yeoman.test %>/e2e/setup-scenario/before-all.js',
//              '<%= yeoman.test %>/e2e/scenarios/components/csar.js',
//              '<%= yeoman.test %>/e2e/scenarios/components/csargit.js'
            //'<%= yeoman.test %>/e2e/scenarios/security/security_environments.js'
            // '<%= yeoman.test %>/e2e/scenarios/application/application.js'
//            '<%= yeoman.test %>/e2e/scenarios/topology_templates/topology_templates.js'
            '<%= yeoman.test %>/e2e/scenarios/application_topology/application_topology_runtime.js'
  //              '<%= yeoman.test %>/e2e/scenarios/admin/admin_groups_management.js',
  //              '<%= yeoman.test %>/e2e/scenarios/admin/admin_users_management.js',
  //              '<%= yeoman.test %>/e2e/scenarios/security/security_groups.js',
  //              '<%= yeoman.test %>/e2e/scenarios/security/security_users.js',
  //              '<%= yeoman.test %>/e2e/scenarios/authentication.js',
  //              '<%= yeoman.test %>/e2e/scenarios/homepage.js',
  //              '<%= yeoman.test %>/e2e/scenarios/language_test.js',
  //              '<%= yeoman.test %>/e2e/scenarios/plugins.js',
  //              '<%= yeoman.test %>/e2e/scenarios/*'
          ]
        }
      }
    },
    runAdmin: {
      options: {
        args: {
          browser: grunt.option('browser'),
          baseUrl: 'http://localhost:8088',
          specs: [
            '<%= yeoman.test %>/e2e/setup-scenario/before-all.js',
            '<%= yeoman.test %>/e2e/scenarios/admin/**/*.js'
          ]
        }
      }
    },
    runApplication: {
      options: {
        args: {
          browser: grunt.option('browser'),
          baseUrl: 'http://localhost:8088',
          specs: [
            '<%= yeoman.test %>/e2e/setup-scenario/before-all.js',
            '<%= yeoman.test %>/e2e/scenarios/application/**/*.js'
          ]
        }
      }
    },
    runApplicationTopology: {
      options: {
        args: {
          browser: grunt.option('browser'),
          baseUrl: 'http://localhost:8088',
          specs: [
            '<%= yeoman.test %>/e2e/setup-scenario/before-all.js',
            '<%= yeoman.test %>/e2e/scenarios/application_topology/**/*.js'
          ]
        }
      }
    },
    runDeploymentAndSecurity: {
      options: {
        args: {
          browser: grunt.option('browser'),
          baseUrl: 'http://localhost:8088',
          specs: [
            '<%= yeoman.test %>/e2e/setup-scenario/before-all.js',
            '<%= yeoman.test %>/e2e/scenarios/deployment/**/*.js',
            '<%= yeoman.test %>/e2e/scenarios/security/**/*.js'
          ]
        }
      }
    },
    runComponents: {
      options: {
        args: {
          browser: grunt.option('browser'),
          baseUrl: 'http://localhost:8088',
          specs: [
            '<%= yeoman.test %>/e2e/setup-scenario/before-all.js',
            '<%= yeoman.test %>/e2e/scenarios/components/**/*.js'
          ]
        }
      }
    },
    runCommon: {
      options: {
        args: {
          browser: grunt.option('browser'),
          baseUrl: 'http://localhost:8088',
          specs: [
            '<%= yeoman.test %>/e2e/setup-scenario/before-all.js',
            '<%= yeoman.test %>/e2e/scenarios/common/**/*.js'
          ]
        }
      }
    },
    runTopologyTemplates: {
      options: {
        args: {
          browser: grunt.option('browser'),
          baseUrl: 'http://localhost:8088',
          specs: [
            '<%= yeoman.test %>/e2e/setup-scenario/before-all.js',
            '<%= yeoman.test %>/e2e/scenarios/topology_templates/**/*.js'
          ]
        }
      }
    },
    runChrome: {
      options: {
        args: {
          browser: 'chrome'
        }
      }
    },
    runFirefox: {
      options: {
        args: {
          browser: 'firefox'
        }
      }
    },
    runIexplore: {
      options: {
        args: {
          browser: 'iexplore'
        }
      }
    }
  }
};
