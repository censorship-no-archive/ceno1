var fs = require('fs');
var gulp = require('gulp');
var jshint = require('gulp-jshint');
var stylish = require('jshint-stylish');
var concat = require('gulp-concat');
var babel = require('gulp-babel');
var jsoncombine = require('gulp-jsoncombine');

gulp.task('lintjs', function() {
  return gulp.src('./portal/js/*.js')
    .pipe(jshint('.jshintrc'))
    .pipe(jshint.reporter(stylish));
});

gulp.task('convertjs', function() {
  return gulp.src('./portal/js/*.js')
    .pipe(babel({
      presets: ['es2015']
    }))
    .pipe(gulp.dest('./static/javascript'));
});

gulp.task('concatcss', function() {
  return gulp.src('./portal/css/*.css')
    .pipe(concat('main.css'))
    .pipe(gulp.dest('./static/stylesheets/'));
});

gulp.task('copyhtml', function() {
  return gulp.src('./portal/html/*.html')
    .pipe(gulp.dest('./views/'));
});

gulp.task('translations', function() {
  return gulp.src('./portal/locale/*.json')
    .pipe(jsoncombine('all.json', function(data) {
      return new Buffer(JSON.stringify(data));
    }))
    .pipe(gulp.dest('./locale/'));
});

// Shorthand tasks for applying changes to single resources
gulp.task('js', ['lintjs', 'convertjs']);
gulp.task('css', ['concatcss']);
gulp.task('html', ['copyhtml']);

gulp.task('build', ['lintjs', 'convertjs', 'concatcss', 'copyhtml', 'translations']);
