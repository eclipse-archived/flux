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



/*global define CryptoJS */
/*jslint forin:true devel:true*/

define(function (require) {

require('lib/sha1'); //Not AMD defines 'CryptoJS global variable.

var io = require('lib/socket.io');
var Deferred = require('orion/Deferred');
var authorize = require('authorize');

var SERVICE_TO_REGEXP = {
	"org.eclipse.flux.jdt": new RegExp(".*\\.java|.*\\.class")
};

var editSession; //Does this belong here? should be a propery of FluxEditor object.

var callbacksCache = {};

var counter = 1;
function generateCallbackId() {
	return counter++;
}

function createResourceMetadata(data) {
	var resourceMetadata = {};
	for (var key in data) {
		resourceMetadata[key] = data[key];
	}
	resourceMetadata.liveMarkers = [];
	resourceMetadata.markers = [];
	resourceMetadata._muteRequests = 0;
	resourceMetadata._queueMuteRequest = function() {
		this._muteRequests++;
	};
	resourceMetadata._dequeueMuteRequest = function() {
		this._muteRequests--;
	};
	resourceMetadata._canLiveEdit = function() {
		return this._muteRequests === 0;
	};
	return resourceMetadata;
}

/**
 * An implementation of the file service that understands the Orion
 * server file API. This implementation is suitable for invocation by a remote plugin.
 */
var FluxEditor = (function() {
	/**
	 * @class Provides operations on files, folders, and projects.
	 * @name FileServiceImpl
	 */
	function FluxEditor(host, port, root) {
		this._rootLocation = root;
		this._port = port;
		this._host = host;
	}

	FluxEditor.prototype = /**@lends eclipse.FluxEditor.prototype */
	{
		_createSocket: function (user) {
			this.socket = io.connect(
				this._host, {
				port: this._port
			});

			this._resourceUrl = null;

			var self = this;
			self.username = user;

			this.socket.on('connect', function() {
				self.socket.emit('connectToChannel', {
					'channel' : user
				}, function(answer) {
					if (answer.connectedToChannel) {
						self._connectedToChannel = true;
						console.log("Editor connected to FLUX channel: " + user);
					}
				});
			});

			this.socket.on('getResourceResponse', function(data) {
				self._handleMessage(data);
			});

			this.socket.on('getMetadataResponse', function(data) {
				self._handleMessage(data);
			});

			this.socket.on('contentassistresponse', function(data) {
				self._handleMessage(data);
			});

			this.socket.on('liveResourceStartedResponse', function(data) {
				self._getResourceData().then(function(resourceMetadata) {
					if (data.username === resourceMetadata.username &&
						data.project === resourceMetadata.project &&
						data.resource === resourceMetadata.resource &&
						data.callback_id !== undefined &&
						resourceMetadata.timestamp === data.savePointTimestamp &&
						resourceMetadata.hash === data.savePointHash
					) {
						resourceMetadata._queueMuteRequest();
						self._editorContext.setText(data.liveContent).then(function() {
							resourceMetadata._dequeueMuteRequest();
						}, function() {
							resourceMetadata._dequeueMuteRequest();
						});
					}
				}, function(err) {
					console.log(err);
				});
			});

			this.socket.on('liveResourceStarted', function(data) {
				Deferred.all([self._getResourceData(), self._editorContext.getText()]).then(function(results) {
					var resourceMetadata = results[0];
					var contents = results[1];
					if (resourceMetadata &&
						data.username === resourceMetadata.username &&
						data.project === resourceMetadata.project &&
						data.resource === resourceMetadata.resource &&
						data.callback_id !== undefined &&
						data.hash === resourceMetadata.hash &&
						data.timestamp === resourceMetadata.timestamp
					) {
						var livehash = CryptoJS.SHA1(contents).toString(CryptoJS.enc.Hex);

						if (livehash !== data.hash) {
							self.sendMessage('liveResourceStartedResponse', {
								'callback_id'        : data.callback_id,
								'requestSenderID'    : data.requestSenderID,
								'username'           : data.username,
								'project'            : data.project,
								'resource'           : data.resource,
								'savePointTimestamp' : resourceMetadata.timestamp,
								'savePointHash'      : resourceMetadata.hash,
								'liveContent'        : contents
							});
						}
					}
				});
			});

			this.socket.on('getLiveResourcesRequest', function(data) {
				self._getResourceData().then(function(resourceMetadata) {
					if ((!data.projectRegEx || new RegExp(data.projectRegEx).test(resourceMetadata.project))
						&& (!data.resourceRegEx || new RegExp(data.resourceRegEx).test(resourceMetadata.resource))) {

						var liveEditUnits = {};
						liveEditUnits[resourceMetadata.project] = [{
							'resource'          	: resourceMetadata.resource,
							'savePointHash' 		: resourceMetadata.hash,
							'savePointTimestamp'	: resourceMetadata.timestamp
						}];

						self.sendMessage('getLiveResourcesResponse', {
							'callback_id'        : data.callback_id,
							'requestSenderID'    : data.requestSenderID,
							'username'           : resourceMetadata.username,
							'liveEditUnits'      : liveEditUnits
						});
					}
				});
			});

			this.socket.on('resourceStored', function(data) {
				var location = self._rootLocation + data.project + '/' + data.resource;
				if (self._resourceUrl === location) {
					self._resourceMetadata = createResourceMetadata(data);
					self._editorContext.markClean();
				}
			});

			this.socket.on('serviceRequiredRequest', function(data) {
				self._getResourceData().then(function(resourceMetadata) {
					if (data.username === resourceMetadata.username
							&& SERVICE_TO_REGEXP[data.service] 
							&& SERVICE_TO_REGEXP[data.service].test(resourceMetadata.resource)) {

						self.sendMessage('serviceRequiredResponse', {
							'requestSenderID'    : data.requestSenderID,
							'username'           : resourceMetadata.username,
							'service'			 : data.service
						});
					}
				});
			});
			
			this.socket.on('liveResourceChanged', function(data) {
				self._getResourceData().then(function(resourceMetadata) {
					if (data.username === resourceMetadata.username
						&& data.project === resourceMetadata.project
						&& data.resource === resourceMetadata.resource
						&& self._editorContext) {

						var text = data.addedCharacters !== undefined ? data.addedCharacters : "";

						resourceMetadata._queueMuteRequest();
						self._editorContext.setText(text, data.offset, data.offset + data.removedCharCount).then(function() {
							resourceMetadata._dequeueMuteRequest();
						}, function() {
							resourceMetadata._dequeueMuteRequest();
						});
					}
				});
			});

			this.socket.on('liveMetadataChanged', function (data) {
				self._getResourceData().then(function(resourceMetadata) {
					if (resourceMetadata.username === data.username
						&& resourceMetadata.project === data.project
						&& resourceMetadata.resource === data.resource
						&& data.problems !== undefined) {

						resourceMetadata.liveMarkers = [];
						var i;
						for(i = 0; i < data.problems.length; i++) {
	//						var lineOffset = editor.getModel().getLineStart(data.problems[i].line - 1);

	//						console.log(lineOffset);

							resourceMetadata.liveMarkers[i] = {
								'description' : data.problems[i].description,
	//							'line' : data.problems[i].line,
								'severity' : data.problems[i].severity,
								'start' : /*(data.problems[i].start - lineOffset) + 1*/ data.problems[i].start,
								'end' : /*data.problems[i].end - lineOffset*/ data.problems[i].end
							};
						}
						if (self._editorContext) {
							self._editorContext.showMarkers(resourceMetadata.liveMarkers);
						}
					}
					self._handleMessage(data);
				});
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
			console.log('sendMessage: ', type, message);
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

		_getResourceData: function() {
			var request = new Deferred();
			var self = this;
			if (self._resourceMetadata) {
				request.resolve(self._resourceMetadata);
			} else if (this._resourceUrl) {
				var normalizedLocation = this._normalizeLocation(this._resourceUrl);
				this.sendMessage("getResourceRequest", {
					'username' : this.user,
					'project' : normalizedLocation.project,
					'resource' : normalizedLocation.path
				}, function(data) {
					var location = self._rootLocation + data.project + '/' + data.resource;
					if (self._resourceUrl === location) {
						self._resourceMetadata = createResourceMetadata(data);
						request.resolve(self._resourceMetadata);
					}
				});
			} else {
				request.reject("No resource URL!");
			}
			return request;
		},

		_setEditorInput: function(resourceUrl, editorContext) {
			var self = this;
			if (this._resourceUrl !== resourceUrl) {
				this._resourceUrl = null;
				this._editorContext = null;
				this._resourceMetadata = null;
				if (editSession) {
					editSession.resolve();
				}
				if (this._isFluxResource(resourceUrl)) {
					this._resourceUrl = resourceUrl;
					editSession = new Deferred();
					this._editorContext = editorContext;

					this._getResourceData().then(function(resourceMetadata) {
						self.sendMessage('liveResourceStarted', {
							'callback_id' : 0,
							'username' : resourceMetadata.username,
							'project' : resourceMetadata.project,
							'resource' : resourceMetadata.resource,
							'hash' : resourceMetadata.hash,
							'timestamp' : resourceMetadata.timestamp
						});
					});
				}
			}
			return editSession;
		},

		onModelChanging: function(evt) {
			console.log("Editor changing: " + JSON.stringify(evt));
			var self = this;
			this._getResourceData().then(function(resourceMetadata) {
				if (resourceMetadata._canLiveEdit()) {
					var changeData = {
						'username' : resourceMetadata.username,
						'project' : resourceMetadata.project,
						'resource' : resourceMetadata.resource,
						'offset' : evt.start,
						'removedCharCount' : evt.removedCharCount,
						'addedCharacters' : evt.text
					};

					self.sendMessage('liveResourceChanged', changeData);
				}
			});
		},

		computeContentAssist: function(editorContext, options) {
			var request = new Deferred();
			var self = this;
			this._getResourceData().then(function(resourceMetadata) {
				self.sendMessage("contentassistrequest", {
					'username' : resourceMetadata.username,
					'project' : resourceMetadata.project,
					'resource' : resourceMetadata.resource,
					'offset' : options.offset,
					'prefix' : options.prefix,
					'selection' : options.selection
				}, function(data) {
					var proposals = [];
					if (data.proposals) {
						data.proposals.forEach(function(proposal) {
							var name;
							var description;
							if (proposal.description
								&& proposal.description.segments
								&& (Array.isArray && Array.isArray(proposal.description.segments) || proposal.description.segments instanceof Array)) {

								if (proposal.description.segments.length >= 2) {
									name = proposal.description.segments[0].value;
									description = proposal.description.segments[1].value;
								} else {
									description = proposal.description.segments[0].value;
								}
							} else {
								description = proposal.description;
							}
							if (!description) {
								description = proposal.proposal;
							}
							if (description) {
								proposals.push({
									'description' : description,
									'name' : name,
									'overwrite' : proposal.replace,
									'positions' : proposal.positions,
									'proposal' : proposal.proposal,
									'additionalEdits' : proposal.additionalEdits,
									'style' : "emphasis",
									'escapePosition' : proposal.escapePosition
								});
							}
						});
					}
					console.log("Editor content assist: " + JSON.stringify(proposals));
					request.resolve(proposals);
				});
			});
			return request;
		},

		computeProblems: function(editorContext, options) {
			console.log("Validator (Problems): " + JSON.stringify(options));
			var self = this;
			var problemsRequest = new Deferred();
//			this._setEditorInput(options.title, editorContext);

			this._getResourceData().then(function(resourceMetadata) {
				if (self._resourceUrl === options.title) {
					self.sendMessage("getMetadataRequest", {
						'username' : resourceMetadata.username,
						'project' : resourceMetadata.project,
						'resource' : resourceMetadata.resource
					}, function(data) {
						if (data.username === resourceMetadata.username
							&& data.project === resourceMetadata.project
							&& data.resource === resourceMetadata.resource) {

							resourceMetadata.markers = [];
							for(var i = 0; i < data.metadata.length; i++) {
								resourceMetadata.markers[i] = {
									'description' : data.metadata[i].description,
									'severity' : data.metadata[i].severity,
									'start' : data.metadata[i].start,
									'end' : data.metadata[i].end
								};
							}
						}
						problemsRequest.resolve(resourceMetadata.markers);
					});
				} else {
					problemsRequest.reject();
				}
			});

			return problemsRequest;
		},

		startEdit: authorize(function(editorContext, options) {
			this.jdtInitializer = this._initializeJDT(editorContext);
			var url = options ? options.title : null;
			return this._setEditorInput(url, editorContext);
		}),

		endEdit: function(resourceUrl) {
			if (this.jdtInitializer) {
				this.jdtInitializer.dispose();
				delete this.jdtInitializer;
			}
			this._setEditorInput(null, null);
		},

		/**
		 * This function ensures the JDT service is started and shows a status message when
		 * it is ready (or an error if it failed).
		 */
		_initializeJDT: function (editorContext) {
			return require('jdt-initializer')(editorContext, this.socket, this.username);
		}

	};



	return FluxEditor;
}());

return FluxEditor;

}); //define