'use strict';

let fs = require('fs');
let gulp = require('gulp');
let jshint = require('gulp-jshint');
let stylish = require('jshint-stylish');
let concat = require('gulp-concat');
let csslint = require('gulp-csslint');
let babel = require('gulp-babel');

gulp.task('lintjs', () => {
  return gulp.src('./portal/js/*.js')
    .pipe(jshint('.jshintrc'))
    .pipe(jshint.reporter(stylish));
});

gulp.task('concatjs', () => {
  return gulp.src('./portal/js/*.js')
    .pipe(babel({
      presets: ['es2015']
    }))
    .pipe(concat('main.js'))
    .pipe(gulp.dest('./static/javascript/'));
});

gulp.task('concatcss', () => {
  return gulp.src('./portal/css/*.css')
    .pipe(concat('main.css'))
    .pipe(gulp.dest('./static/stylesheets/'));
});

gulp.task('copyhtml', () => {
  return gulp.src('./portal/html/*.html')
    .pipe(gulp.dest('./views/'));
});

gulp.task('build', ['lintjs', 'concatjs', 'concatcss', 'copyhtml']);
