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

var RestRepository = function(expressapp, repository) {
	this.repository = repository;
	
	expressapp.get('/api/:username', this.getProjects.bind(this));

	expressapp.get('/api/:username/:project', this.getProject.bind(this));
	expressapp.post('/api/:username/:project', this.createProject.bind(this));

	expressapp.get('/api/:username/:project/:resource(*)', this.getResource.bind(this));
	expressapp.put('/api/:username/:project/:resource(*)', this.putResource.bind(this));
	expressapp.post('/api/:username/:project/:resource(*)', this.postResource.bind(this));
};

exports.RestRepository = RestRepository;

RestRepository.prototype.getProjects = function(req, res) {
    this.repository.getProjects(req.params.username, function(error, result) {
        res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
    });
};

RestRepository.prototype.getProject = function(req, res) {
	var includeDeleted = req.query.includeDeleted;
	
    this.repository.getProject(req.params.username, req.params.project, includeDeleted, function(error, content, deleted) {
		if (error === null) {
			if (includeDeleted) {
				res.send(JSON.stringify({
					'content' : content,
					'deleted' : deleted
				}), { 'Content-Type': 'application/json' }, 200);
			}
			else {
				res.send(JSON.stringify({
					'content' : content
				}), { 'Content-Type': 'application/json' }, 200);
			}
		}
		else {
			res.send(error);
		}
    });
};

RestRepository.prototype.createProject = function(req, res) {
    this.repository.createProject(req.params.username, req.params.project, function(error, result) {
		if (error === null) {
			res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
		}
		else {
			res.send(error);
		}
    });
};

RestRepository.prototype.postResource = function(req, res) {
	var body = '';
	req.on('data', function(buffer) {
		console.log("Chunk:", buffer.length );
		body += buffer;
	});

	req.on('end', function() {
	    this.repository.createResource(req.params.username, req.params.project, req.params.resource, body, req.headers['resource-sha1'],
				req.headers['resource-timestamp'], req.headers['resource-type'], function(error, result) {

			if (error === null) {
				res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
			}
			else {
				res.send(error);
			}
	    });
	}.bind(this));
};

RestRepository.prototype.putResource = function(req, res) {
	var body = '';
	req.on('data', function(buffer) {
		console.log("Chunk:", buffer.length );
		body += buffer;
	});

	req.on('end', function() {
		if (req.param('meta') !== undefined) {
			var metadata = JSON.parse(body);
			var type = req.param('meta');
		    this.repository.updateMetadata(req.params.username, req.params.project, req.params.resource, metadata, type, function(error, result) {
				if (error === null) {
					res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
				}
				else {
					res.send(error);
				}
		    });
		}
		else {
		    this.repository.updateResource(req.params.username, req.params.project, req.params.resource, body, req.headers['resource-sha1'],
					req.headers['resource-timestamp'], function(error, result) {
				if (error === null) {
					res.send(JSON.stringify(result), { 'Content-Type': 'application/json' }, 200);
				}
				else {
					res.send(error);
				}
		    });
		}

	}.bind(this));

	req.on('error', function(error) {
		console.log('Error: ' + error);
	});
};

RestRepository.prototype.getResource = function(req, res) {
    this.repository.getResource(req.params.username, req.params.project, req.params.resource, undefined, undefined, function(error, result) {
		if (error === null) {
			res.send(result, 200);
		}
		else {
			res.send(error);
		}
    });
};
