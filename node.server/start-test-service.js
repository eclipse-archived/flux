/*******************************************************************************
 * @license
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html).
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/

/*
 Standalone 'service' that connects to the flux bus and doesn't realy do
 anything.

 The main purpose of this service is to figure out how we define a sound
 protocol that allows to negotiate between a client and service provider
 so that a service provider can be assigned to a client
 */

var SERVICE_TYPE_ID = 'foo.bar.test';

var authentication = require('./authentication');
var when = require('when');

var host = process.env.VCAP_APP_HOST || 'localhost';
var port = process.env.VCAP_APP_PORT || '3000';

var messagingHost = 'localhost'; //Careful not to use real host name here as that
                                 // won't work on CF deployments.
                                 //The real host name for 'outside' connections
                                 //doesn't expose the port it is actually running on
                                 //but instead remaps that to standard http / https ports.
                                 //so to talk directly to 'ourselves' we use localhost.
var messagingPort = port;

var SUPER_USER = authentication.SUPER_USER;

var client_io = require('socket.io-client');

/**
 * Create a client socket, returns a promise that resolves when the socket
 * connection is established and ready for use.
 */
function createClientSocket() {
	var deferred = when.defer();

	var client_socket = client_io.connect(messagingHost, authentication.asSuperUser({
		port : messagingPort
	}));

	client_socket.on('connect', function() {

		console.log('client socket connected to '+messagingHost);

		client_socket.emit('connectToChannel', {
			'channel' : SUPER_USER
		}, function(answer) {
			console.log('connectToChannel answer', answer);
			if (answer.connectedToChannel) {
				deferred.resolve(client_socket);
			} else {
				deferred.reject(answer.error);
			}
		});
	});
	return deferred.promise;
}

/**
 * This service manager is capable only of instantiating a single
 * Service instance. This variable contains that instance once it has been
 * created. The variable is nulled again when the service shuts down.
 */
var service = null;

function Service(socket, startServiceRequest) {
	var self = this;
	self.startRequest = startServiceRequest;
	self.username = startServiceRequest.username;
	self.socket = socket;
	self.setStatus('starting');
	setTimeout(function () {
		self.setStatus('ready');
		setTimeout(function () {
			self.shutdown();
		}, 60000);
	}, 20000);
}

/**
 * Change the service status. When switching to 'unavailable' status an
 * error message can optionally be provided as an explanation to be
 * sent out with the statusChange event.
 */
Service.prototype.setStatus = function (status, error) {
	console.log('setStatus', status, error);
	var self = this;
	if (self.status===status) {
		//no change
		return;
	}
	var oldStatus = self.status;
	self.status = status;
	console.log('send status message');
	self.socket.emit('serviceStatusChange', {
		username: self.username,
		status: self.status,
		oldStatus: oldStatus,
		service: SERVICE_TYPE_ID,
		error: error
	});
};

Service.prototype.shutdown = function () {
	var self = this;
	self.setStatus('unavailable', 'Shutdown');
	service = null;
};

createClientSocket().done(function (socket) {
	socket.on('discoverServiceRequest', function (msg) {
		if (msg.service===SERVICE_TYPE_ID) {
			var requestingUser = msg.username;
			if (service) {
				//service instance already created
				//service is either starting or ready.
				if (service.username === requestingUser) {
					socket.emit('discoverServiceResponse', {
						username: msg.username,
						service: SERVICE_TYPE_ID,
						requestSenderID: msg.requestSenderID,
						status: service.status
					});
				} else {
					socket.emit('discoverServiceResponse', {
						username: msg.username,
						service: SERVICE_TYPE_ID,
						requestSenderID: msg.requestSenderID,
						status: 'unavailable',
						error: 'Service provider is already assigned to another user'
					});
				}
			} else {
				//no service instance yet (or already shutdown).
				socket.emit('discoverServiceResponse', {
						username: msg.username,
						service: SERVICE_TYPE_ID,
						requestSenderID: msg.requestSenderID,
						status: 'available'
				});
				//possible issue: service is available now, but what if multiple users
				// are trying to grab the service at the same time?
				//In that case the fastest one to actually 'grab' it will win.
				//So client-side the 'loosers' must be able to handle the fact that service
				//may no longer be available by the time they attempt to 'grab' it.
			}
		}
	});

	socket.on('startServiceRequest', function (msg) {
		if (msg.service===SERVICE_TYPE_ID) {
			if (service) {
				//This could happen in a scenario where two clients simultaneously try
				// to establish a connection after they both discovered this available
				// instance before either one could request to start the service.
				socket.emit('startServiceResponse', {
					requestSenderID: msg.requestSenderID,
					username: msg.username,
					service: msg.service,
					status: 'unavailable',
					error: 'Request to start received but service is already busy'
				});
			} else {
				service = new Service(socket, msg);
				socket.emit('startServiceResponse', {
					requestSenderID: msg.requestSenderID,
					username: msg.username,
					service: msg.service,
					status: service.status
				});
			}
		}
	});
});


