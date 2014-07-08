/*global io alert $*/
define(function (require) {

	var socketio = require("socketio");
	//Map from projectName -> ProjectView
	var projects = {};

	var URI = require('URIjs/URI');

	console.log('location = ', window.location);

	var queryString  = window.location.search;

	console.log('parseQuery', URI.parseQuery(queryString));

	var username = URI.parseQuery(queryString).user || 'defaultuser';
	$(".username").text(username);


	//console.log('query=', query);


	var socket = io.connect();

	function ProjectView(projectName) {
		this.name = projectName;
		this.element = $('<div>').addClass('project');
		this.element.append(
			$('<span>').text(projectName).addClass('name')
		);
		var oldView = projects[name];
		projects[projectName] = this;
		if (oldView) {
			oldView.dispose();
		}
		$("#projectlist").append(this.element);
		this.resources = {}; //Map resource path to ResourceView
	}
	ProjectView.prototype.dispose = function () {
		$(this.element).remove();
	};
	ProjectView.prototype.addResource = function (resource) {
		console.log('addResource', resource);
		var type = resource.type;
		var path = resource.path;
		//funny resource with path=='' and no type exists. Ignore it
		if (type==='file' && path) {
			new ResourceView(this, resource);
		}
	};
	ProjectView.prototype.removeResource = function (path) {
		var r = this.resources[path];
		if (r) {
			r.dispose();
			delete this.resources[path];
		}
	};

	function editorLink(projectName, resource) {
		//Example:
		//  http://localhost:3000/client/html/editor.html#defaultuser/Hello-World/src/santa/HoHoHo.java
		return "html/editor.html#"+username+"/"+projectName + "/" +resource.path;
	}

	function ResourceView(parent, resource) {
		var resources = parent.resources;
		var old = resources[resource.path];
		this.element = $('<a>')
			.text(resource.path)
			.attr('href', editorLink(parent.name, resource) )
			.addClass('resource');
		if (old) {
			//Insert new element in same place as the old one
			old.element.after(this.element);
			old.dispose();
		} else {
			parent.element.append(this.element);
		}
		resources[resource.path] = this;
	}
	ResourceView.prototype.dispose = function () {
		$(this.element).remove();
	};

	function projectCreated(projectName) {
		var projectView = new ProjectView(projectName);
		socket.emit('getProjectRequest', {
			'callback_id' : 0,
			'username' : username,
			'project' : projectName,
			'includeDeleted' : false
		});
	}

	function projectRemoved(projectName) {
		var p = projects[projectName];
		if (p) {
			p.dispose();
			delete projects[projectName];
		}
	}

	function resourceCreated(username, projectName, file) {
		var projectView = projects[projectName];
		if (projectView) {
			projectView.addResource(file);
		}
	}

	function resourceDeleted(username, projectName, path) {
		var project = projects[projectName];
		if (project) {
			project.removeResource(path);
		}
	}

	socket.on('connect', function() {
		if (username) {
			socket.emit('connectToChannel', {
				'channel' : username
			}, function(answer) {
				console.log('connectToChannel', answer);
				if (answer.connectedToChannel) {
					return socket.emit('getProjectsRequest', {
						username: username,
						callback_id: 0
					});
				} else {
					if (answer.error) {
						alert("Flux connection couldn't be established. \n"+answer.error);
					}
				}
			});
		}
	});

	socket.on('getProjectsResponse', function (data) {
		console.log('getProjectsResponse', data);
		var target = $("#projectlist");
		target.empty();
		console.log('data', data);
		console.log('data.projects', data.projects);
		data.projects.forEach(function (p) {
			projectCreated(p.name);
		});
	});

	socket.on('getProjectResponse', function (data) {
		console.log('getProjectResponse', data);
		var username = data.username;
		var projectName = data.project;
		data.files.forEach(function (file) {
			resourceCreated(username, projectName, file);
		});
	});

	socket.on('projectConnected', function (data) {
		projectCreated(data.project);
	});
	socket.on('projectDisconnected', function (data) {
		projectRemoved(data.project);
	});
	socket.on('resourceCreated', function (data) {
		resourceCreated(data.username, data.project, {
			path: data.resource,
			type: data.type
		});
		console.log('resourceCreated', data);
	});
	socket.on('resourceDeleted', function (data) {
		resourceDeleted(data.username, data.project, data.resource);
	});

});