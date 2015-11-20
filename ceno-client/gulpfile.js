'use strict';

let fs = require('fs');
let gulp = require('gulp');
let jshint = require('gulp-jshint');
let stylish = require('jshint-stylish');
let concat = require('gulp-concat');
let csslint = require('gulp-csslint');
let babel = require('gulp-babel');
let jsoncombine = require('gulp-jsoncombine');

gulp.task('lintjs', () =>
  gulp.src('./portal/js/*.js')
    .pipe(jshint('.jshintrc'))
    .pipe(jshint.reporter(stylish))
);

gulp.task('convertjs', () =>
  gulp.src('./portal/js/*.js')
    .pipe(babel({
      presets: ['es2015']
    }))
    .pipe(gulp.dest('./static/javascript'))
);

gulp.task('concatcss', () =>
  gulp.src('./portal/css/*.css')
    .pipe(concat('main.css'))
    .pipe(gulp.dest('./static/stylesheets/'))
);

gulp.task('copyhtml', () =>
  gulp.src('./portal/html/*.html')
    .pipe(gulp.dest('./views/'))
);

gulp.task('translations', () =>
  gulp.src('./portal/locale/*.json')
    .pipe(jsoncombine('all.json', (data) => new Buffer(JSON.stringify(data))))
    .pipe(gulp.dest('./locale/'))
);

// Shorthand tasks for applying changes to single resources
gulp.task('js', ['lintjs', 'convertjs']);
gulp.task('css', ['concatcss']);
gulp.task('html', ['copyhtml']);

gulp.task('build', ['lintjs', 'convertjs', 'concatcss', 'copyhtml', 'translations']);
