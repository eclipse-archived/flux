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

var sys = require('sys');
var crypto = require('crypto');

var InMemoryRepository = function() {
	this.storage = {};
};

exports.Repository = InMemoryRepository;

InMemoryRepository.prototype._getProjectStorage = function(username, projectName) {
	var userStorage = this.storage[username];
	if (userStorage !== undefined) {
		return this.storage[username].projects[projectName];
	}
	else {
		return undefined;
	}
};

InMemoryRepository.prototype.setNotificationSender = function(notificationSender) {
	this.notificationSender = notificationSender;
};

InMemoryRepository.prototype.getProjects = function(username, callback) {
	var projects = [];

	if (this.storage[username] !== undefined) {
		for (var projectName in this.storage[username].projects) {
			if (typeof this.storage[username].projects[projectName] !== 'function') {
				var project = {
					'name' : projectName
				};
				projects.push(project);
			}
		}
	}

    callback(null, projects);
};

InMemoryRepository.prototype.hasProject = function(username, projectName, callback) {
	callback(null, this.storage[username] !== undefined && this.storage[username].projects[projectName] !== undefined);
};

InMemoryRepository.prototype.getProject = function(username, projectName, includeDeleted, callback) {
	var project = this._getProjectStorage(username, projectName);
	if (project !== undefined) {
		var resources = [];
		for (var resourcePath in project.resources) {
			if (typeof project.resources[resourcePath] !== 'function') {
				var resourceDescription = {};
				resourceDescription.path = resourcePath;
				resourceDescription.type = project.resources[resourcePath].type;
				resourceDescription.timestamp = project.resources[resourcePath].timestamp;
				resourceDescription.hash = project.resources[resourcePath].hash;

				resources.push(resourceDescription);
			}
		}

		if (includeDeleted) {
			var deleted = [];
			for (resourcePath in project.deleted) {
				if (typeof project.deleted[resourcePath] !== 'function') {
					var deletedResourceDescription = {};
					deletedResourceDescription.path = resourcePath;
					deletedResourceDescription.timestamp = project.deleted[resourcePath].timestamp;
					deleted.push(deletedResourceDescription);
				}
			}
		    callback(null, resources, deleted);
		}
		else {
		    callback(null, resources);
		}

	}
	else {
	    callback(404);
	}
};

InMemoryRepository.prototype.createProject = function(username, projectName, callback) {
	if (this._getProjectStorage(username, projectName) === undefined) {
		if (this.storage[username] === undefined) {
			this.storage[username] = {'projects' : {}};
		}

		this.storage[username].projects[projectName] = {
			'name' : projectName,
			'resources' : {},
			'deleted' : {}
		};

	    callback(null, {
			'project': projectName
	    });

		this.notificationSender.emit('projectCreated', {
			'username' : username,
			'project' : projectName
		});
	}
	else {
		callback(404);
	}
};

InMemoryRepository.prototype.createResource = function(username, projectName, resourcePath, data, hash, timestamp, type, callback) {
	var project = this._getProjectStorage(username, projectName);
	if (project !== undefined) {
		console.log('putResource ' + username + " : "+ resourcePath);
		project.resources[resourcePath] = {
			'data' : data,
			'type' : type,
			'hash' : hash,
			'timestamp' : timestamp,
			'metadata' : {}
		};

		if (project.deleted[resourcePath] !== undefined) {
			delete project.deleted[resourcePath];
		}

	    callback(null, {'project': projectName});

		this.notificationSender.emit('resourceCreated', {
			'username' : username,
			'project' : projectName,
			'resource' : resourcePath,
			'hash' : hash,
			'timestamp' : timestamp,
			'type' : type
		});
	}
	else {
		callback(404);
	}
};

InMemoryRepository.prototype.updateResource = function(username, projectName, resourcePath, data, hash, timestamp, callback) {
	var project = this._getProjectStorage(username, projectName);
	if (project !== undefined) {
		console.log('updateResource ' + username + " : " + resourcePath);
		var resource = project.resources[resourcePath];

		if (resource !== undefined && timestamp > resource.timestamp) {
			resource.data = data;
			resource.hash = hash;
			resource.timestamp = timestamp;

		    callback(null, {
				'username' : username,
				'project' : projectName,
				'hash' : hash
			});

			this.notificationSender.emit('resourceChanged', {
				'username' : username,
				'project' : projectName,
				'resource' : resourcePath,
				'timestamp' : timestamp,
				'hash' : hash});

			this.notificationSender.emit('resourceStored', {
				'username' : username,
				'project' : projectName,
				'resource' : resourcePath,
				'timestamp' : timestamp,
				'hash' : hash});

		}
		else {
			callback(404);
		}
	}
	else {
		callback(404);
	}
};

InMemoryRepository.prototype.hasResource = function(username, projectName, resourcePath, callback) {
	var project = this._getProjectStorage(username, projectName);
	if (project !== undefined) {
		var resource = project.resources[resourcePath];
		if (resource !== undefined) {
			callback(null, true);
			return;
		}
	}
	callback(null, false);
};

InMemoryRepository.prototype.needsUpdate = function(username, projectName, resourcePath, type, timestamp, hash, callback) {
	var project = this._getProjectStorage(username, projectName);
	if (project !== undefined) {
		var resource = project.resources[resourcePath];
		if (resource !== undefined) {
			if (resource.type != type || resource.timestamp < timestamp) {
				callback(null, true);
				return;
			}
		}
	}
	callback(null, false);
};

InMemoryRepository.prototype.gotDeleted = function(username, projectName, resourcePath, timestamp, callback) {
	var project = this._getProjectStorage(username, projectName);
	if (project !== undefined) {
		var deleted = project.deleted[resourcePath];
		if (deleted !== undefined) {
			if (deleted.timestamp > timestamp) {
				callback(null, true);
				return;
			}
		}
	}
	callback(null, false);
};

InMemoryRepository.prototype.getResourceInfo = function(username, projectName, resourcePath, type, timestamp, hash, callback) {
	var project = this._getProjectStorage(username, projectName);
	if (project !== undefined) {
		var resource = project.resources[resourcePath];
		var exists = resource !== undefined;
		var needsUpdate = resource !== undefined && (resource.type != type || resource.timestamp < timestamp);

		var deletedResource = project.deleted[resourcePath];
		var deleted = deleted !== undefined && deleted.timestamp > timestamp;

		callback(null, {
			'exists' : exists,
			'deleted' : deleted,
			'needsUpdate' : needsUpdate,
			'username' : username,
			'project' : projectName,
			'resource' : resourcePath,
			'timestamp' : timestamp,
			'hash' : hash
		});
	}
	else {
		callback(404);
	}
};

InMemoryRepository.prototype.updateMetadata = function(username, projectName, resourcePath, metadata, type, callback) {
	var project = this._getProjectStorage(username, projectName);
	if (project !== undefined) {
		console.log('updateMetadata ' + username + " : " + resourcePath);
		var resource = project.resources[resourcePath];

		if (resource !== undefined) {
			resource.metadata[type] = metadata;

		    callback(null, {'project' : projectName
							});

			var metadataMessage = {
				'username' : username,
				'project' : projectName,
				'resource' : resourcePath,
				'type' : type,
				'metadata' : metadata
			};
			this.notificationSender.emit('metadataChanged', metadataMessage);
		}
		else {
			callback(404);
		}
	}
	else {
		callback(404);
	}
};

InMemoryRepository.prototype.getResource = function(username, projectName, resourcePath, timestamp, hash, callback) {
	var project = this._getProjectStorage(username, projectName);
	if (project !== undefined) {
		console.log('getResource ' + username + " : "+resourcePath);
		var resource = project.resources[resourcePath];

		if (resource !== undefined) {
			if (timestamp !== undefined && timestamp !== resource.timestamp) {
				callback(404);
			}
			else if (hash !== undefined && hash !== resource.hash) {
				callback(404);
			}
			else {
				callback(null, resource.data, resource.timestamp, resource.hash);
			}
		}
		else {
			callback(404);
		}
	}
	else {
		callback(404);
	}
};

InMemoryRepository.prototype.deleteResource = function(username, projectName, resourcePath, timestamp, callback) {
	var result = {
		'projectName' : projectName,
		'deletedResource' : resourcePath,
		'deletedTimestamp' : timestamp
	};

	var project = this._getProjectStorage(username, projectName);
	if (project !== undefined) {
		console.log('deleteResource ' + username + " : " +resourcePath);
		var resource = project.resources[resourcePath];

		if (resource !== undefined && resource.timestamp < timestamp) {
			delete project.resources[resourcePath];
			project.deleted[resourcePath] = {'timestamp' : timestamp};
			callback(null, result);

			this.notificationSender.emit('resourceDeleted', {
				'username' : username,
				'project' : projectName,
				'resource' : resourcePath,
				'timestamp' : timestamp
			});
		}
		else {
			callback(404, result);
		}
	}
	else {
		callback(404, result);
	}
};
