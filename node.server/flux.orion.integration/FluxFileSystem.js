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
/*global define console CryptoJS */
define(function(require) {
var Deferred = require('orion/Deferred');
var io = require('socketio');
require('lib/sha1'); //Not AMD. Defines 'CryptoJS global.

var authorize = require('authorize');

function assignAncestry(parents, childrenDepthMap, depth) {
	var child, parentLocation, parent;
	if (childrenDepthMap[depth]) {
		var newParents = {};
		for (var i in childrenDepthMap[depth]) {
			child = childrenDepthMap[depth][i];
			if (depth > 0) {
				parentLocation = child.Location.substr(0, child.Location.lastIndexOf('/'));
				if (parents[parentLocation]) {
					parent = parents[parentLocation];
					if (!parent.Children) {
						parent.Children = [];
					}
					parent.Children.push(child);
					if (!parent._childrenCache) {
						parent._childrenCache = {};
					}
					parent._childrenCache[child.Name] = child;
					if (!child.Parents) {
						child.Parents = [];
					}
					child.Parents.push(parent);
					if (!parent.ChildrenLocation) {
						parent.ChildrenLocation = parent.Location + '/';
					}
				} else {
					if (parentLocation) {
						throw new Error("Parent is missing!");
					}
				}
			}
			newParents[child.Location] = child;
		}
		assignAncestry(newParents, childrenDepthMap, depth + 1);
	}
}

var counter = 1;
function generateCallbackId() {
	if (counter === Number.MAX_VALUE) {
		counter = 1;
	}
	return counter++;
}

var callbacksCache = {};
var saves = {};

function _cleanSaves(resource) {
	var cachedSave = saves[resource.Location];
	if (cachedSave && (resource.Directory || (!resource.Directory
			&& cachedSave.hash === resource.ETag
			/*&& cachedSave.timestamp === resource.LocalTimeStamp*/))) {
		if (cachedSave.deferred && cachedSave.deferred.resolve && cachedSave.deferred.resolve.call) {
			cachedSave.deferred.resolve(resource);
		}
		delete saves[resource.Location];
	}

}

/**
 * An implementation of the file service that understands the Orion
 * server file API. This implementation is suitable for invocation by a remote plugin.
 */
var FluxFileSystem = (function() {
	/**
	 * @class Provides operations on files, folders, and projects.
	 * @name FileServiceImpl
	 */
	function FluxFileSystem(host, port, root) {
		this._rootLocation = root;
		this._port = port;
		this._host = host;
	}

	FluxFileSystem.prototype = /**@lends eclipse.FluxFileSystem.prototype */
	{
		_createSocket: function (user) {
			if (this._connectedToChannel) {
				//Don't know why, but orion is calling us twice in a row and
				//we end up in here a second time before we are done creating our socket.
				//We should never create more than one socket, though. So bail out of here.
				console.log('STOP! no second socket!');
				return;
			}

			console.log('Create socket for ', user);
			this._connectedToChannel = new Deferred();
			this.socket = io({
			    transports : ['websocket']
            });
			this.socket.connect('http://' + this._host + ':' + this._port);

			var self = this;

			this.socket.on('connect', function() {
	//			while (user && !self._connectedToChannel) {
					self.socket.emit('connectToChannel', {
						'channel' : user
					}, function(answer) {
						if (answer.connectedToChannel) {
							self._connectedToChannel.resolve();
							console.log("FileSystem connected to FLUX channel: " + user);
						} else {
							self._connectedToChannel.reject(answer.error);
						}
					});
	//			}
			});

			this.socket.on('getProjectsResponse', function(data) {
				if (data.username === user) {
					self._handleMessage(data);
				}
			});

			this.socket.on('getProjectResponse', function(data) {
				if (data.username === user) {
					self._handleMessage(data);
				}
			});

			this.socket.on('getResourceResponse', function(data) {
				if (data.username === user) {
					self._handleMessage(data);
				}
			});

			this.socket.on('resourceStored', function(data) {
				if (data.username === user) {
					var resource = self._createOrionResource(data);
					var parentPath = resource.Location.substr(0, resource.Location.lastIndexOf('/'));

					self._findFromLocation(parentPath).then(function(parent) {
						var foundResource = parent._childrenCache ? parent._childrenCache[resource.Name] : null;
						if (foundResource) {
							if (foundResource.LocalTimeStamp < resource.LocalTimeStamp) {
								foundResource.LocalTimeStamp = resource.LocalTimeStamp;
								foundResource.ETag = resource.ETag;
							}
							_cleanSaves(foundResource);
						} else {
							if (!parent.Children) {
								parent.Children = [];
							}
							parent.Children.push(resource);
							if (!parent._childrenCache) {
								parent._childrenCache = {};
							}
							parent._childrenCache[resource.Name] = resource;
							resource.Parents = [ parent ];
							_cleanSaves(resource);
						}
					});
				}
				self._handleMessage(data);
			});

			this.socket.on('getResourceRequest', function(data) {
				if (data.username === user) {
					var resource = self._createOrionResource(data);
					var cachedSave = saves[resource.Location];
					if (cachedSave
						&& cachedSave.hash === resource.ETag
						&& cachedSave.timestamp === resource.LocalTimeStamp
						&& cachedSave.username === data.username) {

						self.sendMessage("getResourceResponse", {
							'callback_id' : data.callback_id,
							'requestSenderID' : data.requestSenderID,
							'username' : cachedSave.username,
							'project' : cachedSave.project,
							'resource' : cachedSave.resource,
							'timestamp' : cachedSave.timestamp,
							'type' : cachedSave.type,
							'hash' : cachedSave.hash,
							'content' : cachedSave.content
						});
					}
					self._handleMessage(data);
				}
			});

			this.socket.on("resourceCreated", function(data) {
	//			console.log("resourceCreated: " + JSON.stringify(data));
			});

		}, //createSocket

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
			var self = this;
			//Avoid loosing messages by sending them before we are properly connected!
			return self._connectedToChannel.then(function() {
				console.log('==>', type, message);
	//			if (this._connectedToChannel) {
					if (callbacks) {
						message.callback_id = generateCallbackId();
						callbacksCache[message.callback_id] = callbacks;
					} else if (!message.callback_id) {
						message.callback_id = 0;
					}
					self.socket.emit(type, message);
					return true;
	//			} else {
	//				return false;
	//			}
			});
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
		_createParents: function(entry) {
			var deferred = new Deferred();
			var result = [];
			var rootFullPath = this._root.fullPath;

			function handleParent(parent) {
				if (parent.fullPath !== rootFullPath) {
					var location = parent.toURL();
					location = location.slice(-1) === "/" ? location : location + "/";
					result.push({
						Name: parent.name,
						Location: location,
						ChildrenLocation: location
					});
					parent.getParent(handleParent, deferred.reject);
				} else {
					deferred.resolve(result);
				}
			}
			if (rootFullPath === entry.fullPath) {
				deferred.resolve(null);
			} else {
				entry.getParent(handleParent, deferred.reject);
			}
			return deferred;
		},

		/**
		 * Obtains the children of a remote resource
		 * @param location The location of the item to obtain children for
		 * @return A deferred that will provide the array of child objects when complete
		 */
		fetchChildren: function(location) {
			if (location.charAt(location.length - 1) === '/') {
				location = location.substr(0, location.length - 1);
			}
			return this._findFromLocation(location).then(function(parent) {
				if (parent && parent.Children) {
					return parent.Children;
				} else {
					return [];
				}
			});
		},


		/**
		 * Loads all the user's workspaces. Returns a deferred that will provide the loaded
		 * workspaces when ready.
		 */
		loadWorkspaces: function() {
			return this.loadWorkspace();
		},

		_createOrionProject: function(data) {
			var file, name, lastIndexOfSlash, isFile, childrenDepthMap = {}, j, depth;
			var result = {
				Attributes: {
					ReadOnly: false,
					SymLink: false,
					Hidden: false,
					Archive: false
				},
				Name: data.project,
				Directory: true,
				ETag: data.hash,
				LocalTimeStamp: data.timestamp,
				Location: data.project,
				Children: [],
				ChildrenLocation: data.project + '/',
				_childrenCache: {},
				Id: data.project
			};
			var entries = [ result ];

			for (j in data.files) {
				file = data.files[j];
				if (!file.path) {
					// project entry is found with empty path fill in the data
					result.ETag = file.hash;
					result.LocalTimeStamp = file.timestamp;
					continue;
				}
				if (file.path) {
					file.path = '/' + file.path;
				}
				file.path = data.project + file.path;
				lastIndexOfSlash = file.path.lastIndexOf('/');
				name = lastIndexOfSlash < 0 ? file.path : file.path.substr(lastIndexOfSlash + 1);
				isFile = file.type === 'file';
				entries.push({
						Attributes: {
						ReadOnly: false,
						SymLink: false,
						Hidden: false,
						Archive: false
					},
					Name: name,
					Directory: !isFile,
					ETag: file.hash,
					LocalTimeStamp: file.timestamp,
					Location: file.path,
					Id: name
//					ContentType: "text/plain",
				});
			}
			for (j in entries) {
				depth = entries[j].Location.split('/').length - 1;
				if (!childrenDepthMap[depth]) {
					childrenDepthMap[depth] = [];
				}
				childrenDepthMap[depth].push(entries[j]);
				if (depth === 0) {
					result = entries[j];
				}
			}
			assignAncestry({}, childrenDepthMap, 0);
			for (j in entries) {
				entries[j].Location = this._rootLocation + entries[j].Location;
				if (entries[j].Directory) {
					entries[j].ChildrenLocation = entries[j].Location + '/';
				}
			}
			return result;
		},

		_getProject: authorize(function(projectName) {
			var projectRequest = new Deferred();
			var self = this;
			this.sendMessage(
				"getProjectRequest",
				{
					'username' : this.user,
					'project': projectName
				}, function(data) {
					var project = self._createOrionProject(data);
					projectRequest.resolve(project);
				}
			);
			return projectRequest;
		}),

		/**
		 * Loads the workspace with the given id and sets it to be the current
		 * workspace for the IDE. The workspace is created if none already exists.
		 * @param {String} location the location of the workspace to load
		 * @param {Function} onLoad the function to invoke when the workspace is loaded
		 */
		loadWorkspace: authorize(function(location) {
			return this.getWorkspace(location);
		}),

		getWorkspace: authorize(function(location) {
			var deferred = new Deferred();

			if (this._workspace) {
				deferred.resolve(this._workspace);
				return deferred;
			}

			var self = this;

			var workspace = {
				Attributes: {
					ReadOnly: false,
					SymLink: false,
					Hidden: false,
					Archive: false
				},
				Name: "",
				Directory: true,
				LocalTimeStamp: Date.now(),
				Location: this._rootLocation,
				ChildrenLocation: this._rootLocation,
				Length: 0,
				Children: [],
				_childrenCache: []
			};

			var projectsResponseHandler = function(data) {
				var requests = [];
				if (data.projects) {
					for (var i in data.projects) {
						var projectRequest = self._getProject(data.projects[i].name);
						requests.push(projectRequest);
					}
				}
				if (requests.length > 0) {
					Deferred.all(requests).then(function(results) {
						workspace.Children = results;
						for (var i in results) {
							results[i].Parents = [];
							results[i].Parents.push(workspace);
							workspace._childrenCache[results[i].Name] = results[i];
						}
						self._workspace = workspace;
						deferred.resolve(self._workspace);

					});
				} else {
					self._workspace = workspace;
					deferred.resolve(self._workspace);
				}
			};

			this.socket.once("getProjectsResponse", projectsResponseHandler);

			this.sendMessage("getProjectsRequest", { 'username' : this.user });

			return deferred;
		}),

		_findFromLocation: function(location) {
			var self = this;
			return this.getWorkspace().then(function(workspace) {
				var result = workspace;
				var relativeLocation = location.replace(self._rootLocation, "");
				if (relativeLocation) {
					var path = relativeLocation.split('/');
					for (var i = 0; i < path.length && result; i++) {
						result = result._childrenCache ? result._childrenCache[path[i]] : null;
					}
				}
				return result;
			});
		},

		createResource: authorize(function(location, type, contents) {
			var deferred = new Deferred();
			var normalizedPath = this._normalizeLocation(location);
			var hash = CryptoJS.SHA1(contents).toString(CryptoJS.enc.Hex);
			var timestamp = Date.now();
			var self = this;

			this._findFromLocation(location).then(function(resource) {
				if (resource) {
					deferred.reject("The resource \'" + location + "\' already exists!");
				} else {
					var data = {
						'username' : self.user,
						'project' : normalizedPath.project,
						'resource' : normalizedPath.path,
						'hash' : hash,
						'type': type,
						'timestamp' : timestamp
					};

					saves[location] = {
						'username' : self.user,
						'project' : normalizedPath.project,
						'resource' : normalizedPath.path,
						'type': type,
						'hash' : hash,
						'timestamp' : timestamp,
						'content' : contents ? contents : "",
						'deferred' : deferred
					};

					self.sendMessage("resourceCreated", data);
					//This deferred is not resolved, but that is intentional.
					// It is resolved later when we get a response back for our message.
				}
			});
			return deferred;
		}),

		/**
		 * Adds a project to a workspace.
		 * @param {String} url The workspace location
		 * @param {String} projectName the human-readable name of the project
		 * @param {String} serverPath The optional path of the project on the server.
		 * @param {Boolean} create If true, the project is created on the server file system if it doesn't already exist
		 */
		createProject: authorize(function(url, projectName, serverPath, create) {
			var self = this;
			var deferred = new Deferred();
			this.getWorkspace(url).then(function(workspace) {
				if (workspace._childrenCache && workspace._childrenCache[projectName]) {
					deferred.reject("Project with name \'" + projectName + "\' already exists!");
				} else {
					var hash = CryptoJS.SHA1(projectName).toString(CryptoJS.enc.Hex);
					var timestamp = Date.now();
					var location = url + projectName;
					var project = {
						Attributes: {
							ReadOnly: false,
							SymLink: false,
							Hidden: false,
							Archive: false
						},
						Name: projectName,
						Directory: true,
						ETag: hash,
						LocalTimeStamp: timestamp,
						Location: location,
						Children: [],
						ChildrenLocation: location + '/',
						Parents: [ workspace ],
						_childrenCache: {},
						Id: projectName
					};
					if (!workspace._childrenCache) {
						workspace._childrenCache = {};
					}
					workspace._childrenCache[projectName] = project;
					if (!workspace.Children) {
						workspace.Children = [];
					}
					workspace.Children.push(project);
					self.sendMessage("projectCreated", {
						'username' : this.user,
						'project' : projectName
					});
					deferred.resolve(project);
				}
			});
			return deferred;
		}),

		/**
		 * Creates a folder.
		 * @param {String} parentLocation The location of the parent folder
		 * @param {String} folderName The name of the folder to create
		 * @return {Object} JSON representation of the created folder
		 */
		createFolder: function(parentLocation, folderName) {
			return this.createResource(parentLocation + '/' + folderName, 'folder');
		},

		/**
		 * Create a new file in a specified location. Returns a deferred that will provide
		 * The new file object when ready.
		 * @param {String} parentLocation The location of the parent folder
		 * @param {String} fileName The name of the file to create
		 * @return {Object} A deferred that will provide the new file object
		 */
		createFile: function(parentLocation, fileName) {
			return this.createResource(parentLocation + '/' + fileName, 'file');
		},

		/**
		 * Deletes a file, directory, or project.
		 * @param {String} location The location of the file or directory to delete.
		 */
		deleteFile: authorize(function(location) {
			var self = this;
			return this._findFromLocation(location).then(function(resource) {
				if (resource) {
					var parent = resource.Parents[0];
					delete parent._childrenCache[resource.Name];
					var idx = parent.Children.indexOf(resource);
					if (idx >= 0) {
						parent.Children.splice(idx, 1);
					}
					var normalizedPath = self._normalizeLocation(location);
					self.sendMessage("resourceDeleted", {
						'username' : self.user,
						'project' : normalizedPath.project,
						'resource' : normalizedPath.path,
						'timestamp' : Date.now(),
						'hash' : resource.ETag
					});
				}
			});
		}),

		/**
		 * Moves a file or directory.
		 * @param {String} sourceLocation The location of the file or directory to move.
		 * @param {String} targetLocation The location of the target folder.
		 * @param {String} [name] The name of the destination file or directory in the case of a rename
		 */
		moveFile: function(sourceLocation, targetLocation, name) {
			throw "Move file not supported";
		},

		/**
		 * Copies a file or directory.
		 * @param {String} sourceLocation The location of the file or directory to copy.
		 * @param {String} targetLocation The location of the target folder.
		 * @param {String} [name] The name of the destination file or directory in the case of a rename
		 */
		copyFile: function(sourceLocation, targetLocation, name) {
			throw "Copy file not supported";
		},

		_createOrionResource: function(data) {
			var resourceUrl = data.resource;
			if (resourceUrl) {
				resourceUrl = '/' + resourceUrl;
			}
			resourceUrl = data.project + resourceUrl;
			var lastIndexOfSlash = resourceUrl.lastIndexOf('/');
			var name = lastIndexOfSlash < 0 ? resourceUrl : resourceUrl.substr(lastIndexOfSlash + 1);
			var isFile = data.type === 'file';
			var entry = {
				Attributes: {
					ReadOnly: false,
					SymLink: false,
					Hidden: false,
					Archive: false
				},
				Name: name,
				Directory: !isFile,
				ETag: data.hash,
				LocalTimeStamp: data.timestamp,
				Location: resourceUrl,
				ContentType: "text/plain"
			};
			entry.Location = this._rootLocation + entry.Location;
			if (entry.Directory) {
				entry.ChildrenLocation = entry.Location + '/';
			}
			return entry;
		},

		/**
		 * Returns the contents or metadata of the file at the given location.
		 *
		 * @param {String} location The location of the file to get contents for
		 * @param {Boolean} [isMetadata] If defined and true, returns the file metadata,
		 *   otherwise file contents are returned
		 * @return A deferred that will be provided with the contents or metadata when available
		 */
		read: authorize(function(location, isMetadata) {
			if (isMetadata) {
				return this._findFromLocation(location);
			}

			var normalizedPath = this._normalizeLocation(location);
			var deferred = new Deferred();
			this.sendMessage(
				"getResourceRequest",
				{
					'username' : this.user,
					'project' : normalizedPath.project,
					'resource' : normalizedPath.path
				}, function(data) {
					deferred.resolve(data.content);
				}
			);

			return deferred;
		}),
		/**
		 * Writes the contents or metadata of the file at the given location.
		 *
		 * @param {String} location The location of the file to set contents for
		 * @param {String|Object} contents The content string, or metadata object to write
		 * @param {String|Object} args Additional arguments used during write operation (i.e. ETag)
		 * @return A deferred for chaining events after the write completes with new metadata object
		 */
		write: authorize(function(location, contents, args) {
			var deferred = new Deferred();
			var normalizedPath = this._normalizeLocation(location);
			var hash = CryptoJS.SHA1(contents).toString(CryptoJS.enc.Hex);
			var timestamp = Date.now();

			saves[location] = {
				'username' : this.user,
				'project' : normalizedPath.project,
				'resource' : normalizedPath.path,
				'hash' : hash,
				'timestamp' : timestamp,
				'content' : contents,
				'deferred' : deferred
			};

			this.sendMessage("resourceChanged", {
				'username' : this.user,
				'project' : normalizedPath.project,
				'resource' : normalizedPath.path,
				'hash' : hash,
				'timestamp' : timestamp
			});

			return deferred;
		}),
		/**
		 * Imports file and directory contents from another server
		 *
		 * @param {String} targetLocation The location of the folder to import into
		 * @param {Object} options An object specifying the import parameters
		 * @return A deferred for chaining events after the import completes
		 */
		remoteImport: function(targetLocation, options) {
			throw "Remote Import not supported";
		},
		/**
		 * Exports file and directory contents to another server
		 *
		 * @param {String} sourceLocation The location of the folder to export from
		 * @param {Object} options An object specifying the export parameters
		 * @return A deferred for chaining events after the export completes
		 */
		remoteExport: function(sourceLocation, options) {
			throw "Remote Export not supported";
		}
	};

	return FluxFileSystem;
}());

return FluxFileSystem;

}); //define