'use strict';

module.exports = function(grunt) {
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
            //'browserName': 'firefox'
          },
          baseUrl: 'http://localhost:8088',
          // baseUrl: 'http://127.0.0.1:9999',
          specs: [
            '<%= yeoman.test %>/e2e/setup-scenario/before-all.js',
            // '<%= yeoman.test %>/e2e/setup-scenario/setup.js',
            // '<%= yeoman.test %>/e2e/scenarios/admin/groups.js',
            // '<%= yeoman.test %>/e2e/scenarios/admin/metaprops_configuration.js',
            // '<%= yeoman.test %>/e2e/scenarios/admin/orchestrators.js',
            // '<%= yeoman.test %>/e2e/scenarios/admin/plugins.js',
            // '<%= yeoman.test %>/e2e/scenarios/admin/users.js',
            // '<%= yeoman.test %>/e2e/scenarios/application/application_details.js',
            // '<%= yeoman.test %>/e2e/scenarios/application/application_environments.js',
            // '<%= yeoman.test %>/e2e/scenarios/application/application_list.js',
            // '<%= yeoman.test %>/e2e/scenarios/application/application_metaprops.js',
            // '<%= yeoman.test %>/e2e/scenarios/application/application_roles.js',
            // '<%= yeoman.test %>/e2e/scenarios/application/application_versions.js',
            // '<%= yeoman.test %>/e2e/scenarios/application_topology/application_topology_editor_capabilities.js',
            // '<%= yeoman.test %>/e2e/scenarios/application_topology/application_topology_editor_editrelationship.js',
            // '<%= yeoman.test %>/e2e/scenarios/application_topology/application_topology_editor_editrequiredprops.js',
            // '<%= yeoman.test %>/e2e/scenarios/application_topology/application_topology_editor_groups.js',
            // '<%= yeoman.test %>/e2e/scenarios/application_topology/application_topology_editor_input_management.js',
            // '<%= yeoman.test %>/e2e/scenarios/application_topology/application_topology_editor_input_output.js',
            // '<%= yeoman.test %>/e2e/scenarios/application_topology/application_topology_editor_nodetemplate.js',
            // '<%= yeoman.test %>/e2e/scenarios/application_topology/application_topology_editor_relationships.js',
            // '<%= yeoman.test %>/e2e/scenarios/application_topology/application_topology_editor_replacenode.js',
            // '<%= yeoman.test %>/e2e/scenarios/application_topology/application_topology_editor_reset.js',
            // '<%= yeoman.test %>/e2e/scenarios/application_topology/application_topology_suggestions_property.js',
            // '<%= yeoman.test %>/e2e/scenarios/common/homepage.js',
            // '<%= yeoman.test %>/e2e/scenarios/common/language.js',
            // '<%= yeoman.test %>/e2e/scenarios/components/component_detail_recommend.js',
            // '<%= yeoman.test %>/e2e/scenarios/components/component_details.js',
            // '<%= yeoman.test %>/e2e/scenarios/components/component_details_tags.js',
            // '<%= yeoman.test %>/e2e/scenarios/components/component_list.js',
            // '<%= yeoman.test %>/e2e/scenarios/components/csar_deletion.js',
            // '<%= yeoman.test %>/e2e/scenarios/components/csar_details.js',
            // '<%= yeoman.test %>/e2e/scenarios/components/csar_git_crud.js',
            // '<%= yeoman.test %>/e2e/scenarios/components/csar_git_list.js',
            // '<%= yeoman.test %>/e2e/scenarios/topology_templates/topology_template_details.js',
            // '<%= yeoman.test %>/e2e/scenarios/topology_templates/topology_template_list.js',
            '<%= yeoman.test %>/e2e/scenarios/**/*.js'
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
            '<%= yeoman.test %>/e2e/scenarios/application_topology/**/*.js',
            '<%= yeoman.test %>/e2e/scenarios/topology_templates/**/*.js'
          ]
        }
      }
    },
    runDeployment: {
      options: {
        args: {
          browser: grunt.option('browser'),
          baseUrl: 'http://localhost:8088',
          specs: [
            '<%= yeoman.test %>/e2e/setup-scenario/before-all.js',
            '<%= yeoman.test %>/e2e/scenarios/deployment/**/*.js'
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
    runCommonAndSecurity: {
      options: {
        args: {
          browser: grunt.option('browser'),
          baseUrl: 'http://localhost:8088',
          specs: [
            '<%= yeoman.test %>/e2e/setup-scenario/before-all.js',
            '<%= yeoman.test %>/e2e/scenarios/common/**/*.js',
            '<%= yeoman.test %>/e2e/scenarios/security/**/*.js'
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
  };
};
