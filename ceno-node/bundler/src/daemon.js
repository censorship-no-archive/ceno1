var daemon = require("daemonize2").setup({
    main: "/usr/bin/bundler",
    name: "bundler",
    pidfile: "/var/tmp/bundler/bundler.pid"
});

switch (process.argv[2]) {

    case "start":
        daemon.start();
        break;

    case "stop":
        console.log('here i am');
        daemon.stop();
        break;

    case "restart":
        daemon.stop();
        daemon.start();
        break;

    default:
        console.log("Usage: [start|stop|restart]");
}
