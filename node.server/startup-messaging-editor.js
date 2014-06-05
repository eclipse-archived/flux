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

// create and configure express
var express = require('express');
var app = express();

app.use("/client", express.static(__dirname + '/web-editor'));

var host = process.env.VCAP_APP_HOST || 'localhost';
var port = process.env.VCAP_APP_PORT || '3000';

var server = app.listen(port, host);
console.log('Express server started on port ' + port);

// create and configure socket.io
var io = require('socket.io').listen(server);
io.set('transports', ['websocket']);

// create and configure services
var MessageCore = require('./messages-core.js').MessageCore;
var messageSync = new MessageCore();

io.sockets.on('connection', function (socket) {
	messageSync.initialize(socket, io.sockets);
});
