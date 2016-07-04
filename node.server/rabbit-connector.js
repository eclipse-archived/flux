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
/*global require console exports process __dirname Buffer*/

var when = require('when');
var amqp = require('amqplib');

var authentication = require('./authentication');
var rabbitUrl = require('./rabbit-url');

var connection = null;

/**
 * Special user name routing key to deliver messages to all users.
 * This name is internal only, client code uses '*' in username
 */
var EVERYONE = '$all$';
var SUPER_USER = '$super$';

function addLogging(prefix, eventSource) {
	eventSource.on('error', function (err) {
		console.error(prefix, err);
	});
	eventSource.on('close', function () {
		console.log(prefix, "closed");
	});
}

//For more focussed logging / debugging of flux messages
var blacklisted = {
	liveMetadataChanged: true,
	liveResourceStarted: true,
	getResourceResponse: true,
	getResourceRequest: true,
	getProjectRequest: true,
	getProjectResponse: true,
	getProjectsRequest: false,
	getProjectsResponse: false,
	serviceRequiredRequest: true,
	serviceRequiredResponse: true
};
function logMsg(pre, type, data) {
	if (blacklisted[type]) {return; }
	console.log(pre, type, data);
}

/**
 * Get connection to AMQP message broker. This returns a promise.
 * The same connection is shared by everyone calling this function.
 */
function getConnection() {
	if (!connection) {
		connection = when(amqp.connect(rabbitUrl));
		//Add some logging to the connection
		connection.catch(function (error) {
			console.log("error connecting to AMQP: " + error);
		})
		connection.done(function (connection) {
			addLogging('AMQP connection: ', connection);
		});
	}
	return connection;
}

/**
 * Create a AMQP channel object. This contains most of the API
 * for the amqp library. It is best thought of as a session object.
 * I.e. each 'client' uses their own instance.
 * Though this is called a 'channel' in amqplib it has no relationship
 * with what is called a 'channel' in flux or libs such as socketio.
 */
function createChannel() {
	return getConnection().then(function (connection) {
		return connection.createChannel();
	});
}

function channelNameToTopicPattern(name) {
	if (name===authentication.SUPER_USER) {
		return '*';
	}
	//TODO: we assume for now that channel names do not contain special characters
	// (i.e. '.', '*' or '#' which have special meaning in AMQP routing patterns)
	return name;
}

function usernameToRoutingKey(name) {
	if (name==='*') {
		return EVERYONE;
	}
	return name;
}

/**
 * Called by Flux web server when an incoming web socket connection is
 * established. Returns a promise that resolves when the corresponding
 * RabbitConnector is fully configured.
 */
function connectWebSocket(socket) {
	var rc = new RabbitConnector(socket);
	socket.on('disconnect', function () {rc.dispose();});
	return rc.initialize();
}

function RabbitConnector(socket) {
	console.log("Creating rabbit connector");
	this.socket = socket;
	this.inbox = null; //name of queue where this connector receives messages from rabbit mq,
						// generated later during initialization.
	this.outbox = null; // name of the exchange to which we send outgoing broadcast style messages.
}

RabbitConnector.prototype.initialize = function () {
	var self = this;
	var socket = self.socket;
	if (self.initialized) {
		return self.initialized;
	}
	console.log('Initializing rabbit connector');

	var initialized = createChannel().then(function (channel) {
		self.channel = channel;
		console.log('Got AMQP channel');
		return channel.assertQueue('', {durable: false, autoDelete: true})
		.then(function (inboxInfo) {
			console.log('Inbox created: ',inboxInfo);
			self.inbox = inboxInfo.queue;
			return channel.consume(self.inbox, self.messageReceived.bind(self), {noAck: true});
		})
		.then(function () {
			self.outbox = 'flux';
			return channel.assertExchange(self.outbox, 'topic', {durable: false})
			.then(function (outboxInfo) {
				console.log('Outbox created');
			});
		}).then(function () {
			//Subscribe to messages intended for 'everyone'
			return channel.bindQueue(self.inbox, self.outbox, EVERYONE).then(function () {
				console.log('Connected to topic '+EVERYONE);
			});
		}).then(function () {
			return self.configure();
		});
	});

	socket.on('message', function (msg) {
		console.log("websock "+socket.id+"("+authentication.getUser(socket)+") => ", msg);
	});

	//Install some logging bits so we can see messages coming in from the websocket.
	// Hack alert! $emit is an implementation detail of socketio lib
	//var _emit = socket.$emit;
	//socket.$emit = function (msgType, data, ack) {
	//	console.log("websock "+socket.id+"("+authentication.getUser(socket)+") => ", msgType, data);
	//	return _emit.apply(this, arguments);
	//};

	//Tricky: connectToChannel listener must be called immediately (i.e. not in some 'deferred' thing)
	//Otherwise we are loosing some emitted 'connectToChannel' messages.
	//Yet its handler code depends on the initialization to already be complete!
	socket.on('connectToChannel', function (data, fn) {
		//Note: a channel in 'flux | socket.io' is not the same thing as a channel in AMQP.
		//  What is called a 'channel' in flux websockets is really more like a
		//  'routing key'.
		initialized.then(function () {
			var sub = self.sub;
			console.log('connectToChannel', data);
			//TODO: for cleaner code don't mix promise and callback style.
			//      Should convert 'checkChannelJoin' to promise style.
			return authentication.checkChannelJoin(socket, data, function (err) {
				var topic = channelNameToTopicPattern(data.channel);
				console.log('checkChannelJoin => ', err);
				if (err) {
					return fn({
						error: err,
						connectedToChannel: false
					});
				} else {
					return self.channel.bindQueue(self.inbox, self.outbox, topic)
					.then(function() {
						console.log('Connected '+self.inbox+' to topic ' + topic);
						//send test message
						// self.channel.publish(self.outbox, topic, self.encode({type: 'test', data: "Test message"}));
						fn({
							'connectedToChannel' : true
						});
					}).otherwise(function (err) {
						return fn({
							error: err,
							connectedToChannel: false
						});
					});
				}
			});
		});
	});
	
	socket.on('disconnectFromChannel', function (data, fn) {
		//Note: a channel in 'flux | socket.io' is not the same thing as a channel in AMQP.
		//  What is called a 'channel' in flux websockets is really more like a
		//  'routing key'.
		initialized.then(function () {
			var sub = self.sub;
			console.log('disconnectFromChannel', data);
			
			var topic = channelNameToTopicPattern(data.channel);
			return self.channel.unbindQueue(self.inbox, self.outbox, topic)
			.then(function() {
				console.log('Disconnected '+self.inbox+' from topic ' + topic);
				//send test message
				// self.channel.publish(self.outbox, topic, self.encode({type: 'test', data: "Test message"}));
				fn({
					'disconnectedFromChannel' : true
				});
			}).otherwise(function (err) {
				return fn({
					error: err,
					connectedToChannel: false
				});
			});
			
		});
	});
	
	self.initialized = initialized;
	return initialized;
};

RabbitConnector.prototype.encode = function (msg) {
	return new Buffer(JSON.stringify(msg));
};
RabbitConnector.prototype.decode = function (buffer) {
	return JSON.parse(buffer.toString());
};

/**
 * This method is called whenever a message is received from rabbitmq in
 * our 'inbox' queue.
 */
RabbitConnector.prototype.messageReceived = function (msg) {
	var self = this;
	msg = self.decode(msg.content);
	if (msg.origin===self.inbox) {
		//Don't deliver messages back to the websocket that originated them
		//This mimicks how socketio works.
		return;
	}
	//console.log('rabbit ['+self.inbox+'] => ', msg);

	var socket = self.socket;
	socket.emit(msg.type, msg.data);
};

RabbitConnector.prototype.configure = function() {

	this.configureRequest('discoverServiceRequest');
	/*
	  'disoverServiceRequest' is sent when a process wants to discover
	  providers that are available for a given service.
	  info in this message: {
	      username: 'kdvolder',
          service: 'org.eclipse.flux.jdt'
          ... calback id etc...
	   }
     */
     this.configureResponse('discoverServiceResponse');
	/* {
	    username: 'kdvolder',
	    service: 'org.eclipse.flux.jdt'
	    status: 'available' | 'starting' | 'ready' | 'unavailable',
	    responseSenderID: <id-of-flux-client-who-sent-the-response>
	}

	These status codes have the following meaning:
	 - available: A reply sent by a service provider that has the capability
	              to provide the requested service but is not yet ready
	              to do so. It is the responsibility of the client
	              to decide whether they want this particular provider
	              start providing the service by sending a 'startService' request
	              to them.
	 - starting:  Sent by a service provider that has already begun the process
	              of intializing itself to provide the requested service but
	              is not yet ready to do so.
	 - ready:     A reply sent by a service provider that is ready
	              to respond to requests right away.
	 - unavailable: Provider is not able to provide the requested service.
	              The message may contain an additional 'error' field explaining
	              why the service is not available.
	              Service providers may elect not to respond at all rather than
	              explicitly explain their unavailability.
    */
    
    this.configureBroadcast('serviceStatusChange');
    /* {
	    username: 'kdvolder',
	    service: 'org.eclipse.flux.jdt'
	    status: 'available' | 'starting' | 'ready' | 'unavailable',
	    senderID: <id-of-flux-client-who-sent-the-response>
    } */
    
	this.configureRequest('serviceRequiredRequest');
    this.configureResponse('serviceRequiredResponse');

	this.configureServiceBroadcast('serviceReady');
	this.configureDirectRequest('startServiceRequest');
	this.configureDirectResponse('startServiceResponse');
	this.configureDirectRequest('shutdownService');

	this.configureBroadcast('projectConnected');
	this.configureBroadcast('projectDisconnected');

	this.configureBroadcast('resourceCreated');
	this.configureBroadcast('resourceChanged');
	this.configureBroadcast('resourceDeleted');
	this.configureBroadcast('resourceStored');

	this.configureBroadcast('metadataChanged');

	this.configureRequest('getProjectRequest');
	this.configureRequest('getProjectsRequest');
	this.configureRequest('getResourceRequest');
	this.configureRequest('getMetadataRequest');

	this.configureResponse('getProjectsResponse');
	this.configureResponse('getProjectResponse');
	this.configureResponse('getResourceResponse');
	this.configureResponse('getMetadataResponse');

	this.configureRequest('getLiveResourcesRequest');
	this.configureResponse('getLiveResourcesResponse');

	this.configureRequest('liveResourceStarted');
	this.configureResponse('liveResourceStartedResponse');

	this.configureBroadcast('liveResourceChanged');
	this.configureBroadcast('liveMetadataChanged');

    this.configureBroadcast('liveCursorOffsetChanged');

	this.configureRequest('contentassistrequest');
	this.configureResponse('contentassistresponse');

	this.configureRequest('navigationrequest');
	this.configureResponse('navigationresponse');

	this.configureRequest('renameinfilerequest');
	this.configureResponse('renameinfileresponse');
	
	this.configureRequest('javadocrequest');
	this.configureResponse('javadocresponse');
	
	this.configureRequest('quickfixrequest');
	this.configureResponse('quickfixresponse');
	
	this.configureRequest('cfLoginRequest');
	this.configureResponse('cfLoginResponse');

	this.configureRequest('cfSpacesRequest');
	this.configureResponse('cfSpacesResponse');
	
	this.configureRequest('cfPushRequest');
	this.configureResponse('cfPushResponse');
	
	this.configureBroadcast('cfAppLog');

};

RabbitConnector.prototype.configureRequest = function(type) {
	var self = this;
	var outbox = self.outbox;
	this.socket.on(type, function (data) {
		data.requestSenderID = self.inbox;
		logMsg("rabbit ["+ self.inbox +"] <= ", type, data);
		return self.channel.publish(outbox, usernameToRoutingKey(data.username),
			self.encode({type: type, origin: self.inbox, data: data})
		);
	});
};

RabbitConnector.prototype.configureBroadcast = function (type) {
	var self = this;
	var outbox = self.outbox;
	this.socket.on(type, function (data) {
		//'data' from websocket client.
		//Must send it to rabbit mq.
		logMsg("rabbit ["+ self.inbox +"] <= ", type, data);
		data.senderID = self.inbox;
		return self.channel.publish(outbox, usernameToRoutingKey(data.username),
			self.encode({type: type, origin: self.inbox, data: data})
		);
	});
};

RabbitConnector.prototype.configureResponse = function(type) {
	var self = this;
	this.socket.on(type, function (data) {
		if (!data) {
			//Don't crash server with NPE
			console.error("message with no data from ["+self.inbox+"] type = " +type);
			return;
		}
		data.responseSenderID = self.inbox;
		logMsg("rabbit ["+ self.inbox +"] <= ", type, data);
		//Deliver directly to inbox of the requester
		self.channel.publish('', data.requestSenderID,
			self.encode({type: type, origin: self.inbox, data: data})
		);
	});
};

RabbitConnector.prototype.configureDirectRequest = function(type) {
	var self = this;
	var outbox = self.outbox;
	this.socket.on(type, function (data) {
		data.requestSenderID = self.inbox;
		logMsg("rabbit ["+ self.inbox +"] <= ", type, data);
		return self.channel.publish('', data.socketID,
			self.encode({type: type, origin: self.inbox, data: data})
		);
	});
};

RabbitConnector.prototype.configureDirectResponse = function(type) {
	var self = this;
	this.socket.on(type, function (data) {
		data.responseSenderID = self.inbox;
		data.socketID = self.inbox; //Deprecate: use 'responseSenderID instead.
		logMsg("rabbit ["+ self.inbox +"] <= ", type, data);
		//Deliver directly to inbox of the requester
		self.channel.publish('', data.requestSenderID,
			self.encode({type: type, origin: self.inbox, data: data})
		);
	});
};


//TODO: Why do we need that?
RabbitConnector.prototype.configureServiceBroadcast = function(type) {
	var self = this;
	var outbox = self.outbox;
	this.socket.on(type, function (data) {
		data.socketID = self.inbox;
		return self.channel.publish(outbox, usernameToRoutingKey(SUPER_USER),
			self.encode({type: type, origin: self.inbox, data: data})
		);
	});
};


RabbitConnector.prototype.dispose = function () {
	var self = this;
	if (self.channel) {
		console.log("disposing ["+self.inbox+"]");
		this.channel.close(); //Not waiting for promise. This is deliberate.
		delete this.channel;
	}
};

/**
 * Called by Flux web server when an incoming web socket connection is
 * established.
 */
function connectWebSocket(socket) {
	console.log('socket connected for: ', socket.handshake.fluxUser);
	var rc = new RabbitConnector(socket);
	socket.on('disconnect', function () {rc.dispose();});
	return rc.initialize();
}

exports.connectWebSocket = connectWebSocket;
