'use strict';

let fs = require('fs');
let gulp = require('gulp');
let jshint = require('gulp-jshint');
let stylish = require('jshint-stylish');
let concat = require('gulp-concat');

gulp.task('lintjs', () => {
  return gulp.src('./portal/js/*.js')
    .pipe(jshint('.jshintrc'))
    .pipe(jshint.reporter(stylish));
});

gulp.task('concatjs', () => {
  let jsFiles = fs.readdirSync('./portal/js').map((filename) => `./portal/js/${filename}`);
  return gulp.src(jsFiles)
    .pipe(concat('main.js'))
    .pipe(gulp.dest('./static/javascript/'));
});

gulp.task('build', ['lintjs']);
