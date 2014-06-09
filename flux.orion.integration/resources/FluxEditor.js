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

/*global window eclipse:true orion FileReader Blob*/
/*jslint forin:true devel:true*/


/** @namespace The global container for eclipse APIs. */
var eclipse = eclipse || {};

var callbacksCache = {};
var user;

var counter = 1;
function generateCallbackId() {
	return counter++;
}

/**
 * An implementation of the file service that understands the Orion 
 * server file API. This implementation is suitable for invocation by a remote plugin.
 */
eclipse.FluxEditor = (function() {
	/**
	 * @class Provides operations on files, folders, and projects.
	 * @name FileServiceImpl
	 */
	function FluxEditor(host, port, userId) {
		this._rootLocation = "flux:http://" + host +":" + port + "/" + userId + "/";
		user = userId;

		this.socket = io.connect(host, {
			port: port
		});
		
		var self = this;
		
		this.socket.on('connect', function() {
//			while (user && !self._connectedToChannel) {
				self.socket.emit('connectToChannel', {
					'channel' : user
				}, function(answer) {
					if (answer.connectedToChannel) {
						self._connectedToChannel = true;
						console.log("EDITOR Connected to FLUX channel: " + user);
					}
				});
//			}
		});
		
	}
	

	FluxEditor.prototype = /**@lends eclipse.FluxEditor.prototype */
	{
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
		
		onModelChanging: function(evt) {
			console.log("Editor changing: " + JSON.stringify(evt));					
		},
		
		computeContentAssist: function(editorContext, options) {
			console("Editor content assist: " + JSON.stringify(editorContext));
		},

	};

	return FluxEditor;
}());