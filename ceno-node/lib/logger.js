var path = require('path'),
    winston = require('winston');

var logPath = process.env.PROXY_LOG || path.join(process.cwd(), 'proxy.log');

var logger = new (winston.Logger)({
  transports: [
    new (winston.transports.File)({
      filename: logPath,
      json: false,
    })
  ],

  // Use syslog levels, but don't use Winston's built-in levels, since those
  // are currently broken: https://github.com/flatiron/winston/issues/249
  // levels: winston.config.syslog.levels,
  levels: {
    debug: 0,
    info: 1,
    notice: 2,
    warning: 3,
    error: 4,
    crit: 5,
    alert: 6,
    emerg: 7
  },
});

module.exports = logger;
