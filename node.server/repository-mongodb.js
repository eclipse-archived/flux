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
/*global require console Buffer exports*/

var mongo = require('mongodb');
var crypto = require('crypto');

var MongoClient = mongo.MongoClient;
var GridStore = mongo.GridStore;

var MongoDBRepository = function() {
	this.mongodb = null;
	this.notificationSender = null;

	MongoClient.connect("mongodb://localhost:27017/flight-db", function(err, db) {

		if (err) {
			this.mongodb = undefined;
			return console.dir(err);
		}

		this.mongodb = db;
	}.bind(this));
};

exports.Repository = MongoDBRepository;

MongoDBRepository.prototype.setNotificationSender = function(notificationSender) {
	this.notificationSender = notificationSender;
};

MongoDBRepository.prototype.getProjects = function(username, callback) {
	if (this.mongodb === undefined) {
		callback(404);
	}

	var projectCollection = this.mongodb.collection('projects');

	projectCollection.find({
		'username' : username
	}).toArray(function(err, items) {
		if (err) {
			callback(404);
		}
		else {
			var projects = [];
			var i;
			for (i = 0; i < items.length; i++) {
				projects.push({
					'name' : items[i].name
				});
			}

		    callback(null, projects);
		}
	});
};

MongoDBRepository.prototype.hasProject = function(username, projectName, callback) {
	if (this.mongodb === undefined) {
		callback(404);
	}

	var projectCollection = this.mongodb.collection('projects');

	projectCollection.find({
		'username' : username,
		'name' : projectName
	}).toArray(function(err, items) {
		if (err) {
			callback(404);
		}
		else {
			var projectExists = items !== undefined && items.length === 1;
			callback(null, projectExists);
		}
	});
};

MongoDBRepository.prototype.getProject = function(username, projectName, includeDeleted, callback) {
	if (this.mongodb === undefined) {
		callback(404);
	}

	var resourcesCollection = this.mongodb.collection('resources');

	resourcesCollection.find({
		'username' : username,
		'projectName' : projectName
	}).toArray(function(err, items) {
		if (err) {
			callback(404);
		}
		else {
			if (items !== undefined) {
				var resources = [];
				var deleted = [];

				var i;
				for (i = 0; i < items.length; i++) {
					if (!items[i].deleted) {
						var resourceDescription = {};
						resourceDescription.path = items[i].path;
						resourceDescription.type = items[i].type;
						resourceDescription.timestamp = items[i].timestamp;
						resourceDescription.hash = items[i].hash;

						resources.push(resourceDescription);
					}
					else if (includeDeleted) {
						var deleteDescription = {};
						deleteDescription.path = items[i].path;
						deleteDescription.timestamp = items[i].timestamp;
						deleted.push(deleteDescription);
					}
				}

				if (includeDeleted) {
					callback(null, resources, deleted);
				}
				else {
				    callback(null, resources);
				}
			}
			else {
				callback(404);
			}
		}
	});
};

MongoDBRepository.prototype.createProject = function(username, projectName, callback) {
	if (this.mongodb === undefined) {
		callback(404);
	}

	var projectCollection = this.mongodb.collection('projects');
	projectCollection.insert({
		'username' : username,
		'name' : projectName
	}, function(err, insertedDocs) {
		if (err) {
			callback(err);
		}
		else {
			console.log(insertedDocs);
			callback(null, {'project': projectName});

			this.notificationSender.emit('projectCreated', {
				'username' : username,
				'project' : projectName
			});
		}
	}.bind(this));
};

MongoDBRepository.prototype.createResource = function(username, projectName, resourcePath, data, hash, timestamp, type, callback) {
	if (this.mongodb === undefined) {
		callback(404);
	}

	if (type === 'file') {
		var fileName = username + '#' + projectName + '#' + resourcePath;
		new GridStore(this.mongodb, fileName, "w").open(function(err, gridStore) {
			if (!err) {
				gridStore.write(data, function(err, gridStore) {
					if (!err) {
						this._createResourceCollectionEntry(username, projectName, resourcePath, hash, timestamp, type, gridStore.fileId, callback);
					}
					gridStore.close(function(err, result) {});
				}.bind(this));
			}
		}.bind(this));
	}
	else {
		this._createResourceCollectionEntry(username, projectName, resourcePath, hash, timestamp, type, null, callback);
	}
};

MongoDBRepository.prototype._createResourceCollectionEntry = function(username, projectName, resourcePath, hash, timestamp, type, contentID, callback) {
	var resourcesCollection = this.mongodb.collection('resources');
	resourcesCollection.insert({
		'username' : username,
		'projectName' : projectName,
		'path' : resourcePath,
		'type' : type,
		'hash' : hash,
		'timestamp' : timestamp,
		'deleted' : false,
		'contentID' : contentID
	}, function(err, insertedDocs) {
		if (err) {
			callback(err);
		}
		else {
			console.log(insertedDocs);
		    callback(null, {'project': projectName});

			this.notificationSender.emit('resourceCreated', {
				'username' : username,
				'project' : projectName,
				'resource' : resourcePath,
				'hash' : hash,
				'timestamp' : timestamp,
				'type' : type
			});

			this.notificationSender.emit('resourceStored', {
				'username' : username,
				'project' : projectName,
				'resource' : resourcePath,
				'hash' : hash,
				'timestamp' : timestamp,
				'type' : type
			});
		}
	}.bind(this));
};

MongoDBRepository.prototype.updateResource = function(username, projectName, resourcePath, data, hash, timestamp, callback) {
	if (this.mongodb === undefined) {
		callback(404);
	}

	var resourcesCollection = this.mongodb.collection('resources');

	resourcesCollection.find({
		'username' : username,
		'projectName' : projectName,
		'path' : resourcePath,
		'deleted' : false
	}).toArray(function(err, items) {
		if (err) {
			callback(404);
		}
		else {
			if (items !== undefined && items.length === 1 && timestamp > items[0].timestamp) {
				if (items[0].type === 'file') {
					var fileName = username + '#' + projectName + '#' + resourcePath;
					new GridStore(this.mongodb, fileName, "w").open(function(err, gridStore) {
						if (!err) {
							gridStore.write(data, function(err, gridStore) {
								if (!err) {
									this._updateResourceCollectionEntry(username, projectName, resourcePath, hash, timestamp, items[0].type, callback);
								}
								gridStore.close(function(err, result) {});
							}.bind(this));
						}
					}.bind(this));
				}
				else {
					this._updateResourceCollectionEntry(username, projectName, resourcePath, hash, timestamp, items[0].type, callback);
				}
			}
			else {
				callback(404);
			}
		}
	}.bind(this));
};

MongoDBRepository.prototype._updateResourceCollectionEntry = function(username, projectName, resourcePath, hash, timestamp, type, callback) {
	var resourcesCollection = this.mongodb.collection('resources');

	resourcesCollection.update({
		'username' : username,
		'projectName' : projectName,
		'path' : resourcePath,
		'deleted' : false
	}, {
		$set: {
			'hash' : hash,
			'timestamp' : timestamp
	}}, {w:1}, function(err) {
		if (err) {
			callback(404);
		}
		else {
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
				'hash' : hash
			});

			this.notificationSender.emit('resourceStored', {
				'username' : username,
				'project' : projectName,
				'resource' : resourcePath,
				'hash' : hash,
				'timestamp' : timestamp,
				'type' : type
			});
		}
	}.bind(this));
};

MongoDBRepository.prototype.hasResource = function(username, projectName, resourcePath, callback) {
	if (this.mongodb === undefined) {
		callback(404);
	}

	var resourcesCollection = this.mongodb.collection('resources');

	resourcesCollection.find({
		'username' : username,
		'projectName' : projectName,
		'path' : resourcePath,
		'deleted' : false
	}).toArray(function(err, items) {
		if (err) {
			callback(404);
		}
		else {
			var resourceExists = items !== undefined && items.length === 1;
			callback(null, resourceExists);
		}
	});
};

MongoDBRepository.prototype.needsUpdate = function(username, projectName, resourcePath, type, timestamp, hash, callback) {
	if (this.mongodb === undefined) {
		callback(404);
	}

	var resourcesCollection = this.mongodb.collection('resources');

	resourcesCollection.find({
		'username' : username,
		'projectName' : projectName,
		'path' : resourcePath,
		'deleted' : false
	}).toArray(function(err, items) {
		if (err) {
			callback(404);
		}
		else {
			if (items !== undefined && items.length === 1) {
				var needsUpdate = items[0].type === type && items[0].timestamp < timestamp;
				callback(null, needsUpdate);
			}
			else {
				callback(404);
			}
		}
	});
};

MongoDBRepository.prototype.gotDeleted = function(username, projectName, resourcePath, timestamp, callback) {
	if (this.mongodb === undefined) {
		callback(404);
	}

	var resourcesCollection = this.mongodb.collection('resources');

	resourcesCollection.find({
		'username' : username,
		'projectName' : projectName,
		'path' : resourcePath,
		'deleted' : true
	}).toArray(function(err, items) {
		if (err) {
			callback(404);
		}
		else {
			if (items !== undefined && items.length === 1) {
				var deleted = items[0].timestamp > timestamp;
				callback(null, deleted);
			}
			else {
				callback(404);
			}
		}
	});
};

MongoDBRepository.prototype.getResourceInfo = function(username, projectName, resourcePath, type, timestamp, hash, callback) {
	if (this.mongodb === undefined) {
		callback(404);
	}

	var resourcesCollection = this.mongodb.collection('resources');

	resourcesCollection.find({
		'username' : username,
		'projectName' : projectName,
		'path' : resourcePath
	}).toArray(function(err, items) {
		if (err) {
			callback(404);
		}
		else {
			var error = null;

			var result = {
				'username' : username,
				'project' : projectName,
				'resource' : resourcePath,
				'timestamp' : timestamp,
				'hash' : hash
			};

			if (items !== undefined && items.length === 1) {
				result.exists = !items[0].deleted;
				result.needsUpdate = !items[0].deleted && (items[0].type != type || items[0].timestamp < timestamp);
				result.deleted = items[0].deleted && items[0].timestamp > timestamp;
			}
			else if (items !== undefined && items.length === 0) {
				result.exists = false;
				result.deleted = false;
				result.needsUpdate = false;
			}
			else {
				error = 404;
			}

			callback(error, result);
		}
	});
};

MongoDBRepository.prototype.updateMetadata = function(username, projectName, resourcePath, metadata, type, callback) {
    callback(null, {
		'project' : projectName
	});

	var metadataMessage = {
		'username' : username,
		'project' : projectName,
		'resource' : resourcePath,
		'type' : type,
		'metadata' : metadata
	};
	this.notificationSender.emit('metadataChanged', metadataMessage);
};

MongoDBRepository.prototype.getResource = function(username, projectName, resourcePath, timestamp, hash, callback) {
	if (this.mongodb === undefined) {
		callback(404);
	}

	var resourcesCollection = this.mongodb.collection('resources');

	resourcesCollection.find({
		'username' : username,
		'projectName' : projectName,
		'path' : resourcePath,
		'deleted' : false
	}).toArray(function(err, items) {
		if (err) {
			callback(404);
		}
		else {
			if (items !== undefined && items.length === 1) {
				var resource = items[0];
				if (timestamp !== undefined && timestamp !== resource.timestamp) {
					callback(404);
				}
				else if (hash !== undefined && hash !== resource.hash) {
					callback(404);
				}
				else if (items[0].type === 'file') {
					var fileName = username + '#' + projectName + '#' + resourcePath;
					new GridStore(this.mongodb, fileName, "r").open(function(err, gridStore) {
						if (!err) {
							gridStore.read(function(err, data) {
								if (!err) {
									callback(null, data.toString(), resource.timestamp, resource.hash);
								}
								else {
									callback(404);
								}
								gridStore.close(function(err, result) {});
							}.bind(this));
						}
					}.bind(this));
				}

			}
			else {
				callback(404);
			}
		}
	}.bind(this));
};

MongoDBRepository.prototype.deleteResource = function(username, projectName, resourcePath, timestamp, callback) {
	if (this.mongodb === undefined) {
		callback(404);
	}

	var resourcesCollection = this.mongodb.collection('resources');

	resourcesCollection.find({
		'username' : username,
		'projectName' : projectName,
		'path' : resourcePath,
		'deleted' : false
	}).toArray(function(err, items) {
		if (err) {
			callback(404);
		}
		else {
			var result = {
				'projectName' : projectName,
				'deletedResource' : resourcePath,
				'deletedTimestamp' : timestamp
			};

			if (items !== undefined && items.length === 1 && timestamp > items[0].timestamp) {
				resourcesCollection.update({
					'username' : username,
					'projectName' : projectName,
					'path' : resourcePath,
					'deleted' : false
				}, {
					$set: {
						'timestamp' : timestamp,
						'deleted' : true
				}}, {w:1}, function(err) {
					if (err) {
						callback(404);
					}
					else {
						callback(null, result);

						this.notificationSender.emit('resourceDeleted', {
							'username' : username,
							'project' : projectName,
							'resource' : resourcePath,
							'timestamp' : timestamp
						});
					}
				}.bind(this));
			}
			else {
				callback(404, result);
			}
		}
	}.bind(this));
};
