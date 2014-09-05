/*global fluxUser fluxHost require console alert $ */
require(['socket', 'jquery'], function (socket) {

	console.log('Lift off!');
	
	console.log('fluxUser = ', fluxUser);
	console.log('fluxHost = ', fluxHost);

	var username = fluxUser;
	
	socket.on('connect', function() {
		if (username) {
			socket.emit('connectToChannel', {
				'channel' : username
			}, function(answer) {
				console.log('connectToChannel', answer);
				if (answer.connectedToChannel) {
					return socket.emit('getProjectsRequest', {
						username: username,
						callback_id: 0
					});
				} else {
					if (answer.error) {
						alert("Flux connection couldn't be established. \n"+answer.error);
					}
				}
			});
		}
	});
	
	$(".cfAppLog").empty();
	
	function logAppend(stream, msg) {
		var formatted = $('<div/>')
				.addClass(stream)
				.text(msg);

		$(".cfAppLog").append(formatted);
	}
	
	socket.on('resourceChanged', function (msg) {
		console.log('resourceChanged', msg);
		logAppend("STDOUT", "Changed ["+msg.timestamp+"] "+msg.project+"/"+msg.resource);
	});
	
});