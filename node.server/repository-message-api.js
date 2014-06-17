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
/*global require console exports*/

var MessagesRepository = function(repository) {
	this.repository = repository;
	this.socket = null;
};

exports.MessagesRepository = MessagesRepository;

MessagesRepository.prototype.setSocket = function(clientsocket) {
	this.socket = clientsocket;
	
	clientsocket.on('getProjectsRequest', this.getProjects.bind(this));
	clientsocket.on('getProjectRequest', this.getProject.bind(this));
	clientsocket.on('getResourceRequest', this.getResource.bind(this));
	
	clientsocket.on('getProjectResponse', this.getProjectResponse.bind(this));
	clientsocket.on('getResourceResponse', this.getResourceResponse.bind(this));
	
	clientsocket.on('projectConnected', this.projectConnected.bind(this));
	clientsocket.on('projectDisconnected', this.projectDisconnected.bind(this));
	
	clientsocket.on('resourceChanged', this.resourceChanged.bind(this));
	clientsocket.on('resourceCreated', this.resourceCreated.bind(this));
	clientsocket.on('resourceDeleted', this.resourceDeleted.bind(this));
};

MessagesRepository.prototype.getProjects = function(data) {
    this.repository.getProjects(data.username, function(error, result) {
		if (error === null) {
			this.socket.emit('getProjectsResponse', {
				'callback_id' : data.callback_id,
				'requestSenderID' : data.requestSenderID,
				'username' : data.username,
				'projects' : result});
		}
    }.bind(this));
};

MessagesRepository.prototype.getProject = function(data) {
    this.repository.getProject(data.username, data.project, data.includeDeleted, function(error, resources, deleted) {
		if (error === null) {
			if (data.includeDeleted) {
				this.socket.emit('getProjectResponse', {
					'callback_id' : data.callback_id,
					'requestSenderID' : data.requestSenderID,
					'username' : data.username,
					'project' : data.project,
					'files' : resources,
					'deleted' : deleted});
			}
			else {
				this.socket.emit('getProjectResponse', {
					'callback_id' : data.callback_id,
					'requestSenderID' : data.requestSenderID,
					'username' : data.username,
					'project' : data.project,
					'files' : resources});
			}
		}
    }.bind(this));
};

MessagesRepository.prototype.getResource = function(data) {
	this.repository.getResource(data.username, data.project, data.resource, data.timestamp, data.hash, function(error, content, timestamp, hash) {
		if (error === null) {
			this.socket.emit('getResourceResponse', {
				'callback_id' : data.callback_id,
				'requestSenderID' : data.requestSenderID,
				'username' : data.username,
				'project' : data.project,
				'resource' : data.resource,
				'timestamp' : timestamp,
				'type' : content ? 'file' : 'folder',
				'hash' : hash,
				'content' : content});
		}
	}.bind(this));
};

MessagesRepository.prototype.projectConnected = function(data) {
	var projectName = data.project;
	var username = data.username;
	
	this.repository.hasProject(username, projectName, function(error, projectExists) {
		if (error === null && !projectExists) {
			this.repository.createProject(username, projectName, function(error, result) {
				if (error === null) {
					this.socket.emit('getProjectRequest', {
						'callback_id' : 0,
						'username' : username,
						'project' : projectName,
						'includeDeleted' : true
					});
				}
			}.bind(this));
		}
		else {
			this.socket.emit('getProjectRequest', {
				'callback_id' : 0,
				'username' : username,
				'project' : projectName,
				'includeDeleted' : true
			});
		}
	}.bind(this));
};

MessagesRepository.prototype.projectDisconnected = function(data) {	
};

MessagesRepository.prototype.getProjectResponse = function(data) {
	var projectName = data.project;
	var username = data.username;
	var files = data.files;
	var deleted = data.deleted;
	
	this.repository.hasProject(username, projectName, function(err, projectExists) {
		if (projectExists) {
			
			var i;
			for (i = 0; i < files.length; i += 1) {
				this.repository.getResourceInfo(username, projectName, files[i].path, files[i].type, files[i].timestamp,
					files[i].hash, this._getProjectResponseCheckResource.bind(this));
			}
		
			if (deleted !== undefined) {
				for (i = 0; i < deleted.length; i += 1) {
					this.repository.deleteResource(username, projectName, deleted[i].path, deleted[i].timestamp,
						this._getProjectResponseDeletedResult.bind(this));
				}
			}
			
		}
	}.bind(this));
};

MessagesRepository.prototype._getProjectResponseCheckResource = function(err, resourceInfo) {
	if (err === null) {

		var newResource = !resourceInfo.exists && !resourceInfo.deleted;
		var updatedResource = resourceInfo.needsUpdate;

		if (newResource || updatedResource) {
			this.socket.emit('getResourceRequest', {
				'callback_id' : 0,
				'username' : resourceInfo.username,
				'project' : resourceInfo.project,
				'resource' : resourceInfo.resource,
				'timestamp' : resourceInfo.timestamp,
				'hash' : resourceInfo.hash
			});
		}
	}
};

MessagesRepository.prototype._getProjectResponseDeletedResult = function(err, result) {
	if (err !== null) {
		console.log('did not delete resource: ' + result.projectName + "/" + result.deletedResource + " - deleted at: " + result.deletedTimestamp);
	}	
};

MessagesRepository.prototype.getResourceResponse = function(data) {
	var username = data.username;
	var projectName = data.project;
	var resource = data.resource;
	var type = data.type;
	var timestamp = data.timestamp;
	var hash = data.hash;
	var content = data.content;
	
	this.repository.hasResource(username, projectName, resource, function(err, resourceExists) {
		if (err === null) {
			if (!resourceExists) {
				this.repository.createResource(username, projectName, resource, content, hash, timestamp, type, function(error, result) {
					if (error !== null) {
						console.log('Error creating repository resource: ' + projectName + "/" + resource + " - " + data.timestamp);
					}
				});
			}
			else {
				this.repository.updateResource(username, projectName, resource, content, hash, timestamp, function(error, result) {
					if (error !== null) {
						console.log('Error updating repository resource: ' + projectName + "/" + resource + " - " + timestamp);
					}
				});
			}
		}
	}.bind(this));
};

MessagesRepository.prototype.resourceChanged = function(data) {
	var username = data.username;
	var projectName = data.project;
	var resource = data.resource;
	var timestamp = data.timestamp;
	var hash = data.hash;
	var type = "file";
	
	this.repository.getResourceInfo(username, projectName, resource, type, timestamp, hash, function(err, resourceInfo) {
		if (err === null) {
			if (!resourceInfo.exists || resourceInfo.needsUpdate) {
				this.socket.emit('getResourceRequest', {
					'callback_id' : 0,
					'username' : username,
					'project' : projectName,
					'resource' : resource,
					'timestamp' : timestamp,
					'type' : type,
					'hash' : hash
				});
			}
		}
	}.bind(this));
};

MessagesRepository.prototype.resourceCreated = function(data) {
	var username = data.username;
	var projectName = data.project;
	var resource = data.resource;
	var timestamp = data.timestamp;
	var hash = data.hash;
	var type = data.type;
	
	this.repository.hasResource(username, projectName, resource, function(err, resourceExists) {
		if (err === null && !resourceExists) {
			this.socket.emit('getResourceRequest', {
				'callback_id' : 0,
				'username' : username,
				'project' : projectName,
				'resource' : resource,
				'timestamp' : timestamp,
				'hash' : hash
			});
		}
	}.bind(this));
};

MessagesRepository.prototype.resourceDeleted = function(data) {
	var username = data.username;
	var projectName = data.project;
	var resource = data.resource;
	var timestamp = data.timestamp;
	
	this.repository.hasResource(username, projectName, resource, function(err, resourceExists) {
		if (err === null && resourceExists) {
			this.repository.deleteResource(username, projectName, resource, timestamp, function(error, result) {
				if (error !== null) {
					console.log('Error deleting repository resource: ' + projectName + "/" + resource + " - " + timestamp);
				}
			});
		}
	}.bind(this));
};
