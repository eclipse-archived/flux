/*******************************************************************************
 * @license
 * Copyright (c) 2013 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
define("editor/javaContentAssist", ['orion/Deferred'], function(Deferred) {
	
	var currentCallbackId = 0;
	var callbacks = {};
		
	function JavaContentAssistProvider(socket) {
		socket.on('contentassistresponse', function (data) {
			if(callbacks.hasOwnProperty(data.callback_id)) {
				console.log(callbacks[data.callback_id]);
				callbacks[data.callback_id].cb.resolve(data.proposals);
				delete callbacks[data.callback_id];
			}
		});
	}
	
	// This creates a new callback ID for a request
	function getCallbackId() {
		currentCallbackId += 1;
		if(currentCallbackId > 10000) {
			currentCallbackId = 0;
		}
		return currentCallbackId;
	}

    function sendContentAssistRequest(request, socket) {
		var deferred = new Deferred();

		var callbackId = getCallbackId();
		callbacks[callbackId] = {
			time : new Date(),
			cb : deferred
		};

		request.callback_id = callbackId;
		socket.emit('contentassistrequest', request);

		return deferred.promise;
    }
	
	JavaContentAssistProvider.prototype =
	{
		computeProposals: function(buffer, offset, context) {
			var request = {
				'username' : this.username,
				'project' : this.project,
				'resource' : this.resourcePath,
				'offset' : offset,
				'prefix' : context.prefix
			};
			
			var deferred = sendContentAssistRequest(request, this.socket);
			return deferred;
		},
		
		setProject: function(project) {
			this.project = project;
		},
		
		setResourcePath: function(resourcePath) {
			this.resourcePath = resourcePath;
		},
		
		setUsername: function(username) {
			this.username = username;
		},
		
		setSocket: function(socket) {
			this.socket = socket;
		}
		
	}
	
	return {
		JavaContentAssistProvider: JavaContentAssistProvider
	};
});