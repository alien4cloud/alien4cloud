// Generated on 2014-01-13 using generator-angular 0.7.1
'use strict';

// # Globbing
// for performance reasons we're only matching one level down:
// 'test/spec/{,*/}*.js'
// use this if you want to recursively match all subfolders:
// 'test/spec/**/*.js'

module.exports = function(grunt) {

  // Load grunt tasks automatically
  require('load-grunt-tasks')(grunt);

  // Load grunt-connect-proxy module
  grunt.loadNpmTasks('grunt-connect-proxy');

  // Load grunt-protractor module
  grunt.loadNpmTasks('grunt-protractor-runner');

  // Load protractor packaged selenium server
  grunt.loadNpmTasks('grunt-protractor_webdriver');

  // load grunt-ng-annotate module
  grunt.loadNpmTasks('grunt-ng-annotate');

  // Time how long tasks take. Can help when optimizing build times
  require('time-grunt')(grunt);

  // Define the configuration for all the tasks
  grunt.initConfig({

    // Project settings
    yeoman: {
      // configurable paths
      app: require('./bower.json').appPath || 'app',
      dist: 'dist'
    },

    // Watches files for changes and runs tasks based on the changed files
    watch: {
      js: {
        files: ['<%= yeoman.app %>/scripts/**/*.js'],
        tasks: ['newer:jshint:all'],
        options: {
          livereload: true
        }
      },
      jsTest: {
        files: ['test/spec/{,*/}*.js'],
        tasks: ['newer:jshint:test', 'karma']
      },
      compass: {
        files: ['<%= yeoman.app %>/styles/{,*/}*.{scss,sass}'],
        tasks: ['compass:server', 'autoprefixer']
      },
      gruntfile: {
        files: ['Gruntfile.js']
      },
      livereload: {
        options: {
          livereload: '<%= connect.options.livereload %>'
        },
        files: ['<%= yeoman.app %>/bower_components/**/*.js', '<%= yeoman.app %>/views/**/*.html', '.tmp/styles/{,*/}*.css', '<%= yeoman.app %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}']
      }
    },

    // The actual grunt server settings
    connect: {
      options: {
        port: 9999,
        // Change this to'0.0.0.0' to access the server from outside.
        hostname: 'localhost',
        livereload: 35729
      },
      proxies: [
        /*
         * proxy all /rest* request to tomcat domain with specific spring security
         * requests
         */
        {
          context: ['/rest', '/api-docs', '/login', '/logout', '/img', '/csarrepository', '/version.json'],
          host: 'localhost',
          port: 8088
        }
      ],
      livereload: {
        options: {
          open: true,
          base: ['.tmp', '<%= yeoman.app %>'],
          middleware: function(connect, options) {
            if (!Array.isArray(options.base)) {
              options.base = [options.base];
            }

            // Setup the proxy
            var middlewares = [require('grunt-connect-proxy/lib/utils').proxyRequest];

            // Serve static files.
            options.base.forEach(function(base) {
              middlewares.push(connect.static(base));
            });

            // Make directory browse-able.
            var directory = options.directory || options.base[options.base.length - 1];
            middlewares.push(connect.directory(directory));

            return middlewares;
          }
        }
      },
      test: {
        options: {
          port: 9998,
          base: ['.tmp', 'test', '<%= yeoman.app %>']
        }
      },
      dist: {
        options: {
          base: '<%= yeoman.dist %>'
        }
      }
    },

    // Make sure code styles are up to par and there are no obvious mistakes
    jshint: {
      options: {
        jshintrc: '.jshintrc',
        reporter: require('jshint-stylish'),
        force: true
      },
      all: ['Gruntfile.js', '<%= yeoman.app %>/scripts/{,*/}*.js'],
      test: {
        options: {
          jshintrc: 'test/.jshintrc'
        },
        src: ['test/spec/{,*/}*.js']
      }
    },

    // Empties folders to start fresh
    clean: {
      dist: {
        files: [{
          dot: true,
          src: ['.tmp', '<%= yeoman.dist %>/*', '!<%= yeoman.dist %>/.git*']
        }]
      },
      server: '.tmp'
    },

    // Add vendor prefixed styles
    autoprefixer: {
      options: {
        browsers: ['last 1 version']
      },
      dist: {
        files: [{
          expand: true,
          cwd: '.tmp/styles/',
          src: '{,*/}*.css',
          dest: '.tmp/styles/'
        }]
      }
    },

    // Automatically inject Bower components into the app
    'bower-install': {
      app: {
        html: '<%= yeoman.app %>/index.html',
        ignorePath: '<%= yeoman.app %>/'
      }
    },

    // Compiles Sass to CSS and generates necessary files if requested
    compass: {
      options: {
        sassDir: '<%= yeoman.app %>/styles',
        cssDir: '.tmp/styles',
        generatedImagesDir: '.tmp/images/generated',
        imagesDir: '<%= yeoman.app %>/images',
        javascriptsDir: '<%= yeoman.app %>/scripts',
        fontsDir: '<%= yeoman.app %>/styles/fonts',
        importPath: '<%= yeoman.app %>/bower_components',
        httpImagesPath: '/images',
        httpGeneratedImagesPath: '/images/generated',
        httpFontsPath: '/styles/fonts',
        relativeAssets: false,
        assetCacheBuster: false,
        raw: 'Sass::Script::Number.precision = 10\n'
      },
      dist: {
        options: {
          generatedImagesDir: '<%= yeoman.dist %>/images/generated'
        }
      },
      server: {
        options: {
          debugInfo: true
        }
      }
    },

    // Renames files for browser caching purposes
    rev: {
      dist: {
        files: {
          src: ['<%= yeoman.dist %>/scripts/{,*/}*.js', '<%= yeoman.dist %>/styles/{,*/}*.css',
            // '<%= yeoman.dist %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}',
            '<%= yeoman.dist %>/styles/fonts/*'
          ]
        }
      }
    },

    // Reads HTML for usemin blocks to enable smart builds that automatically
    // concat, minify and revision files. Creates configurations in memory so
    // additional tasks can operate on them
    useminPrepare: {
      html: '<%= yeoman.app %>/index.html',
      options: {
        dest: '<%= yeoman.dist %>'
      }
    },

    // Performs rewrites based on rev and the useminPrepare configuration
    usemin: {
      html: ['<%= yeoman.dist %>/{,*/}*.html'],
      css: ['<%= yeoman.dist %>/styles/{,*/}*.css'],
      options: {
        assetsDirs: ['<%= yeoman.dist %>']
      }
    },

    // The following *-min tasks produce minified files in the dist folder
    imagemin: {
      dist: {
        files: [{
          expand: true,
          cwd: '<%= yeoman.app %>/images',
          src: '{,*/}*.{png,jpg,jpeg,gif}',
          dest: '<%= yeoman.dist %>/images'
        }]
      }
    },
    svgmin: {
      dist: {
        files: [{
          expand: true,
          cwd: '<%= yeoman.app %>/images',
          src: '{,*/}*.svg',
          dest: '<%= yeoman.dist %>/images'
        }]
      }
    },
    htmlmin: {
      dist: {
        options: {
          collapseWhitespace: true,
          collapseBooleanAttributes: false,
          removeCommentsFromCDATA: true,
          removeOptionalTags: false
        },
        files: [{
          expand: true,
          cwd: '<%= yeoman.dist %>',
          // src: ['*.html', 'views/{,*/}*.html'],
          src: ['*.html', 'views/**/*.html'],
          dest: '<%= yeoman.dist %>'
        }]
      }
    },

    // Allow the use of non-minsafe AngularJS files. Automatically makes it
    // minsafe compatible so Uglify does not destroy the ng references
    ngAnnotate: {
      dist: {
        files: [{
          expand: true,
          cwd: '.tmp/concat/scripts',
          src: '*.js',
          dest: '.tmp/concat/scripts'
        }]
      }
    },

    // Replace Google CDN references
    cdnify: {
      dist: {
        html: ['<%= yeoman.dist %>/*.html']
      }
    },

    // Copies remaining files to places other tasks can use
    copy: {
      dist: {
        files: [{
          expand: true,
          dot: true,
          cwd: '<%= yeoman.app %>',
          dest: '<%= yeoman.dist %>',
          src: ['*.{ico,png,txt}', '.htaccess', '*.html',
            // 'views/{,*/}*.html',
            'views/**/*.html', 'bower_components/**/*', 'js-lib/**/*', 'images/**/*', 'data/**/*', 'api-doc/**/*', 'version.json',
            // 'images/{,*/}*.{webp}',
            // 'images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}',
            'fonts/*'
          ]
        }, {
          expand: true,
          cwd: '.tmp/images',
          dest: '<%= yeoman.dist %>/images',
          src: ['generated/*']
        }, {
          expand: true,
          flatten: true,
          cwd: '<%= yeoman.app %>',
          dest: '<%= yeoman.dist %>/images',
          src: ['bower_components/angular-tree-control/images/*']
        }]
      },
      styles: {
        expand: true,
        cwd: '<%= yeoman.app %>/styles',
        dest: '.tmp/styles/',
        src: '{,*/}*.css'
      }
    },

    // Run some tasks in parallel to speed up the build process
    concurrent: {
      server: ['compass:server'],
      test: ['compass'],
      dist: ['compass:dist',
        // 'imagemin',
        'svgmin'
      ]
    },

    // By default, your `index.html`'s <!-- Usemin block --> will take care of
    // minification. These next options are pre-configured if you do not wish
    // to use the Usemin blocks.
    // cssmin:{
    // dist:{
    // files:{
    // '<%= yeoman.dist %>/styles/main.css':[
    // '.tmp/styles/{,*/}*.css',
    // '<%= yeoman.app %>/styles/{,*/}*.css'
    // ]
    // }
    // }
    // },
    // uglify:{
    // dist:{
    // files:{
    // '<%= yeoman.dist %>/scripts/scripts.js':[
    // '<%= yeoman.dist %>/scripts/scripts.js'
    // ]
    // }
    // }
    // },
    // concat:{
    // dist:{}
    // },

    // Test settings
    karma: {

      unit: {
        configFile: 'karma.conf.js',
        singleRun: true
      },
      e2e: {
        configFile: 'karma-e2e.conf.js',
        singleRun: true
      },
      jenkins: {
        configFile: 'karma.conf.js',
        singleRun: true,
        runnerPort: 9999,
        browsers: ['PhantomJS']
      }
    },

    protractor: {

      // Options :shared configuration with other target
      options: {
        configFile: 'protractor.conf.js', // Default config file
        keepAlive: false, // If false, the grunt process stops when the test
        // fails.
        noColor: false, // If true, protractor will not use colors in its
        // output.
        singleRun: false,
        args: {
          // Arguments passed to the command
        }
      },
      runAdmin: {
        options: {
          args: {
            browser: grunt.option('browser'),
            baseUrl: 'http://localhost:8088',
            specs: [
              'test/e2e/setup-scenario/before-all.js',
              'test/e2e/scenarios/admin/**/*.js'
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
            'test/e2e/setup-scenario/before-all.js',
            'test/e2e/scenarios/application/**/*.js'
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
            'test/e2e/setup-scenario/before-all.js',
            'test/e2e/scenarios/application_topology/**/*.js'
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
            'test/e2e/setup-scenario/before-all.js',
            'test/e2e/scenarios/deployment/**/*.js',
            'test/e2e/scenarios/security/**/*.js'
            ]
          }
        }
      },
      runOtherTests: {
        options: {
          args: {
            browser: grunt.option('browser'),
            baseUrl: 'http://localhost:8088',
            specs: [
            'test/e2e/setup-scenario/before-all.js',
            'test/e2e/scenarios/*.js'            ]
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
      },
      runLocalserver: {
        options: {
          args: {
            capabilities: {
              'browserName': 'chrome'
            },
            // baseUrl: 'http://localhost:9999',
            baseUrl: 'http://localhost:8088',
            specs: [
              'test/e2e/setup-scenario/before-all.js',
              //              'test/e2e/scenarios/admin/admin_cloud.js',
              //              'test/e2e/scenarios/admin/admin_cloud_image.js',
              //              'test/e2e/scenarios/admin/admin_groups_management.js',
              //              'test/e2e/scenarios/admin/admin_metaprops_configuration.js',
              //              'test/e2e/scenarios/admin/admin_users_management.js',
              //              'test/e2e/scenarios/application/application.js',
              //              'test/e2e/scenarios/application/application_metaprops.js',
              //              'test/e2e/scenarios/application/application_environments.js',
              //              'test/e2e/scenarios/application/application_versions.js',
              //              'test/e2e/scenarios/application/application_security.js',
              //              'test/e2e/scenarios/application/application_security_role_check.js',
              //              'test/e2e/scenarios/application/application_tags.js',
              //              'test/e2e/scenarios/application_topology/application_topology_editor_editrelationshipname.js',
              //              'test/e2e/scenarios/application_topology/application_topology_editor_editrequiredprops.js',
              //              'test/e2e/scenarios/application_topology/application_topology_editor_input_output.js',
              //              'test/e2e/scenarios/application_topology/application_topology_editor_multiplenodeversions.js',
              //              'test/e2e/scenarios/application_topology/application_topology_editor_nodetemplate.js',
              //              'test/e2e/scenarios/application_topology/application_topology_editor_plan.js',
              //              'test/e2e/scenarios/application_topology/application_topology_editor_relationships.js',
              //              'test/e2e/scenarios/application_topology/application_topology_editor_replacenode.js',
              //              'test/e2e/scenarios/application_topology/application_topology_runtime.js',
                            'test/e2e/scenarios/application_topology/application_topology_scaling.js',
              //              'test/e2e/scenarios/deployment/deployment.js',
              //              'test/e2e/scenarios/deployment/deployment_matcher.js',
              //              'test/e2e/scenarios/deployment/deployment_manual_match_resources.js',
              //              'test/e2e/scenarios/security/security_cloud.js',
              //              'test/e2e/scenarios/security/security_groups.js',
              //              'test/e2e/scenarios/security/security_users.js',
              //              'test/e2e/scenarios/authentication.js',
              //              'test/e2e/scenarios/component_details.js',
              //              'test/e2e/scenarios/component_details_tags.js',
              //              'test/e2e/scenarios/csar.js',
              //              'test/e2e/scenarios/deployment.js',
              //              'test/e2e/scenarios/deployment_matcher.js',
              //              'test/e2e/scenarios/deployment_manual_match_resources.js',
              //              'test/e2e/scenarios/homepage.js',
              //              'test/e2e/scenarios/language_test.js',
              //              'test/e2e/scenarios/plugins.js',
              //              'test/e2e/scenarios/quick_search.js',
              //              'test/e2e/scenarios/security_cloud.js',
              //              'test/e2e/scenarios/security_groups.js',
              //              'test/e2e/scenarios/security_users.js',
              //              'test/e2e/scenarios/topology_template.js',
              //              'test/e2e/scenarios/*'
            ]
          }
        }
      }
    },
    protractor_webdriver: {
      start: {
        options: {
          // default webdriver packaged with protractor
          // `webdriver-manager update` done by the calm-yeoman-maven-plugin
          path: 'node_modules/.bin/',
          command: 'webdriver-manager start'
        }
      }
    }

  });

  grunt.registerTask('serve', function(target) {
    if (target === 'dist') {
      return grunt.task.run(['build', 'connect:dist:keepalive']);
    }

    grunt.task.run(['clean:server', 'bower-install', 'concurrent:server', 'autoprefixer', 'configureProxies:server', 'connect:livereload', 'watch']);
  });

  grunt.registerTask('server', function() {
    grunt.log.warn('The` server` task has been deprecated.Use` grunt serve` to start a server.');
    grunt.task.run(['serve']);
  });

  grunt.registerTask('test', ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'karma:unit']);

  grunt.registerTask('chrome-ittest', '', function() {
    var tasks = ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'protractor_webdriver:start',
      'protractor:runChrome'
    ];
    grunt.option('force', true);
    grunt.task.run(tasks);
  });

  grunt.registerTask('firefox-ittest', ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'protractor_webdriver:start',
    'protractor:runFirefox'
  ]);

  grunt.registerTask('iexplore-ittest', ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'protractor_webdriver:start',
    'protractor:runIexplore'
  ]);

  grunt.registerTask('local-ittest', ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'protractor_webdriver:start',
    'protractor:runLocalserver'
  ]);

  grunt.registerTask('ittest-admin', ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'protractor_webdriver:start',
  'protractor:runAdmin'
  ]);

  grunt.registerTask('ittest-application', ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'protractor_webdriver:start',
  'protractor:runApplication'
  ]);

  grunt.registerTask('ittest-applicationTopology', ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'protractor_webdriver:start',
  'protractor:runApplicationTopology'
  ]);

  grunt.registerTask('ittest-deploymentAndSecurity', ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'protractor_webdriver:start',
  'protractor:runDeploymentAndSecurity'
  ]);

  grunt.registerTask('ittest-otherTests', ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'protractor_webdriver:start',
  'protractor:runOtherTests'
  ]);

  grunt.registerTask('continuoustest', ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test', 'karma:jenkins']);

  grunt.registerTask('build', ['clean:dist', 'bower-install', 'useminPrepare', 'concurrent:dist', 'autoprefixer', 'concat', 'ngAnnotate', 'copy:dist', 'cdnify',
    'cssmin', 'uglify', 'rev', 'usemin', 'htmlmin'
  ]);

  grunt.registerTask('default', ['newer:jshint', 'test', 'build']);

};
