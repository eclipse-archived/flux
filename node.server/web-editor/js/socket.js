define(function(require) {

	var io = require('socketio');

	var host = window.location.host;
	var isCloudFoundry = host.indexOf('cfapps.io')>=0;

	var socket = isCloudFoundry?io.connect('https:'+host+':4443'):io.connect();

	return socket;
});
