module.exports = function(grunt) {

  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    
    debian_package: {
      options: {
        maintainer: {
            name: "Distributed Deflect Packaging",
            email: "sysops@equalit.ie"
        },
        //prefix: "prefix-",
        name: "bundler",
        //postfix: "-postfix",
        short_description: "The bundler.",
        long_description: "The bundler described in a verbose fashion.",
        version: "2.0.0",
        build_number: "1"
      },
      files: {
        src: [
            'src/**'
        ],
        dest: '/usr/bin'
      }
    },
    jshint: {
      files: ['Gruntfile.js', 'src/**/*.js'],
      options: {
        // options here to override JSHint defaults
        globals: {
          jQuery: true,
          console: true,
          module: true,
          document: true
        }
      }
    },
    watch: {
      files: ['<%= jshint.files %>'],
      tasks: ['jshint', 'qunit']
    }
  });

  grunt.loadNpmTasks('grunt-debian-package');
  grunt.loadNpmTasks('grunt-contrib-jshint');
  grunt.loadNpmTasks('grunt-contrib-watch');

  grunt.registerTask('test', ['jshint']);
  grunt.registerTask('debian', ['debian_package']);
  grunt.registerTask('default', ['jshint', 'debian_package']);

};
