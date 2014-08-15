/*******************************************************************************
 * @license
 * Copyright (c) 2014 Pivotal Software Inc. and others.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html).
 *
 * Contributors: Pivotal Software Inc. - initial API and implementation
 ******************************************************************************/

/*global define console */

define(function (require) {

var io = require('lib/socket.io');
var Deferred = require('orion/Deferred');
var authorize = require('authorize');

var callbacksCache = {};

var counter = 1;
function generateCallbackId() {
	return counter++;
}

/**
 * An implementation of the navigate to definition action
 */
var OpenDeclaration = (function() {
	/**
	 * @class Provides operations on files, folders, and projects.
	 * @name FileServiceImpl
	 */
	function OpenDeclaration(host, port, root) {
		this._rootLocation = root;
		this._host = host;
		this._port = port;
	}

	OpenDeclaration.prototype = /**@lends flux.NavigateAction.prototype */
	{
		_createSocket: function (user) {
			this.socket = io.connect(this._host, {
				port: this._port
			});

			this._resourceUrl = null;

			var self = this;

			this.socket.on('connect', function() {
	//			while (user && !self._connectedToChannel) {
					self.socket.emit('connectToChannel', {
						'channel' : user
					}, function(answer) {
						if (answer.connectedToChannel) {
							self._connectedToChannel = true;
							console.log("OpenDeclaration connected to FLUX channel: " + user);
						}
					});
	//			}
			});

			this.socket.on('navigationresponse', function(data) {
				self._handleMessage(data);
			});
		},
		_normalizeLocation : function(location) {
			if (!location) {
				location = "/";
			} else {
				location = location.replace(this._rootLocation, "");
			}
			var indexOfDelimiter = location.indexOf('/');
			var project = indexOfDelimiter < 0 ? location : location.substr(0, indexOfDelimiter);
			location = indexOfDelimiter < 0 ? undefined : location.substr(indexOfDelimiter + 1);
			return { 'project' : project, 'path' : location };
		},
		sendMessage : function(type, message, callbacks) {
//			if (this._connectedToChannel) {
				if (callbacks) {
					message.callback_id = generateCallbackId();
					callbacksCache[message.callback_id] = callbacks;
				} else if (!message.callback_id) {
					message.callback_id = 0;
				}
				this.socket.emit(type, message);
				return true;
//			} else {
//				return false;
//			}
		},
		_handleMessage: function(data) {
			var callbacks = callbacksCache[data.callback_id];
			if (callbacks) {
				if (Array.isArray(callbacks)) {
					var fn = callbacks[0];
					fn.call(this, data);
					callbacks.shift();
					if (callbacks.length === 0) {
						delete callbacksCache[data.callback_id];
					}
					return true;
				} else if (callbacks.call) {
					callbacks.call(this, data);
					delete callbacksCache[data.callback_id];
					return true;
				}
			}
			return false;
		},

		_isFluxResource: function(resourceUrl) {
			return resourceUrl && resourceUrl.indexOf(this._rootLocation) === 0;
		},

		execute: authorize(function(editorContext, options) {
			var self = this;
			var request = new Deferred();
			var normalizedLocation = self._normalizeLocation(options.input);
			editorContext.getSelection().then(function(selection) {
				self.sendMessage("navigationrequest", {
					'username': self.user,
					'project': normalizedLocation.project,
					'resource': normalizedLocation.path,
					'offset': selection.start,
					'length': selection.start - selection.end
				}, function(data) {
					if (data.navigation) {
						if (data.navigation.project === data.project
							&& data.navigation.resource === data.resource) {

							if (data.navigation.offset) {
								editorContext.setSelection(data.navigation.offset,
									data.navigation.offset + (data.navigation.length ? data.navigation.length : 0),
									true).then(function() {
										request.resolve();
								});
							}
						} else {
							console.log("Declaration: " + JSON.stringify(data.navigation));
							request.resolve();
						}
					} else {
						request.resolve();
					}
				});
			});
			return request;
		})

	};

	return OpenDeclaration;
}());

return OpenDeclaration;

}); // define