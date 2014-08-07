/*******************************************************************************
 * @license
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html).
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
/*global require console exports process __dirname*/

var authentication = require('./authentication');
var SUPER_USER = authentication.SUPER_USER;

var MessageCore = function() {};
exports.MessageCore = MessageCore;

MessageCore.prototype.initialize = function(socket, sockets) {
	console.log('client connected for update notifications');
	
	this.configureServiceBroadcast(socket, sockets, 'serviceReady');
	this.configureDirectRequest(socket, sockets, 'startServiceRequest');
	this.configureDirectResponse(socket, sockets, 'startServiceResponse');
	this.configureDirectRequest(socket, sockets, 'shutdownService');

	this.configureBroadcast(socket, 'projectConnected');
	this.configureBroadcast(socket, 'projectDisconnected');

	this.configureBroadcast(socket, 'resourceCreated');
	this.configureBroadcast(socket, 'resourceChanged');
	this.configureBroadcast(socket, 'resourceDeleted');
	this.configureBroadcast(socket, 'resourceStored');

	this.configureBroadcast(socket, 'metadataChanged');

	this.configureRequest(socket, 'getProjectRequest');
	this.configureRequest(socket, 'getProjectsRequest');
	this.configureRequest(socket, 'getResourceRequest');
	this.configureRequest(socket, 'getMetadataRequest');

	this.configureResponse(socket, sockets, 'getProjectsResponse');
	this.configureResponse(socket, sockets, 'getProjectResponse');
	this.configureResponse(socket, sockets, 'getResourceResponse');
	this.configureResponse(socket, sockets, 'getMetadataResponse');

	this.configureRequest(socket, 'getLiveResourcesRequest');
	this.configureResponse(socket, sockets, 'getLiveResourcesResponse');

	this.configureRequest(socket, 'liveResourceStarted');
	this.configureResponse(socket, sockets, 'liveResourceStartedResponse');

	this.configureBroadcast(socket, 'liveResourceChanged');
	this.configureBroadcast(socket, 'liveMetadataChanged');

	this.configureRequest(socket, 'contentassistrequest');
	this.configureResponse(socket, sockets, 'contentassistresponse');

	this.configureRequest(socket, 'navigationrequest');
	this.configureResponse(socket, sockets, 'navigationresponse');

	this.configureRequest(socket, 'renameinfilerequest');
	this.configureResponse(socket, sockets, 'renameinfileresponse');

	socket.on('disconnect', function () {
		console.log(arguments.length);
		console.log('client disconnected from update notifications');
	});

	socket.on('error', function (err) { 
		console.log("Socket.IO Error"); 
		console.log(err.stack); // this is changed from your code in last comment
	});
	
	socket.on('connectToChannel', function(data, fn) {
		var channel = data.channel;
		authentication.checkChannelJoin(socket, data, function (err) {
			if (err) {
				return fn({
					error: err,
					connectedToChannel: false
				});
			} else {
				//TODO: we have checked that user is allowed to join channel.
				// but... is it possible for clients to join channel without
				// sending a 'connectToChannel' message? If so they could
				// bypass the check.
				socket.join(channel);
				socket.join('*');
				fn({
					'connectedToChannel' : true
				});
			}
		});
	});

	socket.on('disconnectFromChannel', function(data, fn) {
		socket.leave(data.channel);
		socket.leave('*');
		if (fn) {
			fn({
				'disconnectedFromChannel' : true
			});
		}
	});

};

MessageCore.prototype.configureBroadcast = function(socket, messageName) {
	socket.on(messageName, function(data) {
		authentication.checkMessageSend(socket, data, function (err) {
			if (err) {
				console.log("Message rejected: ", err);
				return;
			}
			if (data.username !== undefined) {
				socket.broadcast.to(data.username).emit(messageName, data);
			}
			if (data.username !== '*') {
				//Everyone including SUPER_USER is connected to '*'.
				//So don't send message again.
				socket.broadcast.to(SUPER_USER).emit(messageName, data);
			}
		});
	});
};

MessageCore.prototype.configureServiceBroadcast = function(socket, sockets, messageName) {
	socket.on(messageName, function(data) {
		authentication.checkMessageSend(socket, data, function (err) {
			if (err) {
				console.log("Message rejected: ", err);
				return;
			}
			data.socketID = socket.id;
			socket.broadcast.to(SUPER_USER).emit(messageName, data);
		});
	});
};

MessageCore.prototype.configureRequest = function(socket, messageName) {
	socket.on(messageName, function(data) {
		authentication.checkMessageSend(socket, data, function (err) {
			if (err) {
				console.log("Message rejected: ", err);
				return;
			}
			data.requestSenderID = socket.id;
			if (data.username !== undefined) {
				socket.broadcast.to(data.username).emit(messageName, data);
			}
			if (data.username !== '*') {
				//Everyone including SUPER_USER is connected to '*'.
				//So don't send message again.
				socket.broadcast.to(SUPER_USER).emit(messageName, data);
			}
		});
	});
};

MessageCore.prototype.configureDirectRequest = function(socket, sockets, messageName) {
	socket.on(messageName, function(data) {
		authentication.checkMessageSend(socket, data, function (err) {
			if (err) {
				console.log("Message rejected: ", err);
				return;
			}
			data.requestSenderID = socket.id;
			sockets.socket(data.socketID).emit(messageName, data);
		});
	});
};

MessageCore.prototype.configureResponse = function(socket, sockets, messageName) {
	//TODO: auth checking of response messages.
	//  As it is, I beleave it might be possible for malicious client to send fake resonses
	//  to any socket they have previously received a message from. This is probably an issue.
	socket.on(messageName, function(data) {
		authentication.checkMessageSend(socket, data, function (err) {
			if (err) {
				console.log("Message rejected: ", err);
				return;
			}
			sockets.socket(data.requestSenderID).emit(messageName, data);
		});
	});
};

MessageCore.prototype.configureDirectResponse = function(socket, sockets, messageName) {
	//TODO: auth checking of response messages.
	//  As it is, I beleave it might be possible for malicious client to send fake resonses
	//  to any socket they have previously received a message from. This is probably an issue.
	socket.on(messageName, function(data) {
		authentication.checkMessageSend(socket, data, function (err) {
			if (err) {
				console.log("Message rejected: ", err);
				return;
			}
			data.socketID = socket.id;
			sockets.socket(data.requestSenderID).emit(messageName, data);
		});
	});
};
