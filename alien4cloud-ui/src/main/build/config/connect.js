'use strict';

// Grunt server settings (for grunt serve)
module.exports = {
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
      context: ['/rest', '/api-docs', '/login', '/logout', '/img', '/static', '/version.json'],
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
};
