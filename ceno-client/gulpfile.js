'use strict';

let gulp = require('gulp');
let jshint = require('gulp-jshint');
let stylish = require('jshint-stylish');

gulp.task('lintjs', () => {
  return gulp.src('./portal/js/*.js')
    .pipe(jshint('.jshintrc'))
    .pipe(jshint.reporter(stylish));
});

gulp.task('build', ['lintjs']);
