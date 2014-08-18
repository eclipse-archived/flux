/*******************************************************************************
 * @license
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html).
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/

/*global require console module exports __dirname*/

var isDir = require('./util/fileutil').isDir;
var pathResolve = require('path').resolve;

var createOrion;
try {
	//In 'development mode' use 'hacked' orionode copy from local
	// filesystem directly.
	createOrion = require('../../orion.client/modules/orionode');
	console.log('Development mode: using local clone of orionode');
} catch (e) {
	console.log("Using 'orion-flux' from 'package.json'");
	//When deployed on CF the above orion.client git clone won't
	//be found so we'll consume a 'released' version installed via package.json
	//dependency:
	createOrion = require('orion-flux');
}

module.exports = function (options) {

	var port = options.port;
	var fluxPlugin = options.fluxPlugin;

	var fsStat= require('fs').statSync;
	var fsMkdir= require('fs').mkdirSync;
	var express = require('express');

	var workspaceDir = pathResolve(__dirname, ".workspace");

	if (!isDir(workspaceDir)) {
		fsMkdir(workspaceDir);
	}

	// set up all parameters for startServer
	var params = {
		port: port,
		workspaceDir: workspaceDir,
		passwordFile: null,
		password: null,
		configParams: {},
		dev: null,
		log: null
	};

	var orion = createOrion(params);

	var app = express();
	app.use(app.router); //our router first so we can override stuff from orion-node

	if (fluxPlugin) {
		var defaultPlugins = {
				//Copied from defaults.pref in orion-node
				"/plugins":{
					//Removed we just want to use our own:
					//"plugins/fileClientPlugin.html":true,
					"plugins/jslintPlugin.html":true,
					"edit/content/imageViewerPlugin.html":true,
					"edit/content/jsonEditorPlugin.html":true,
					"plugins/webEditingPlugin.html":true,
					"plugins/languages/arduino/arduinoPlugin.html":true,
					"plugins/languages/c/cPlugin.html":true,
					"plugins/languages/cpp/cppPlugin.html":true,
					"plugins/languages/java/javaPlugin.html":true,
					"plugins/languages/lua/luaPlugin.html":true,
					"plugins/languages/php/phpPlugin.html":true,
					"plugins/languages/python/pythonPlugin.html":true,
					"plugins/languages/ruby/rubyPlugin.html":true,
					"plugins/languages/xml/xmlPlugin.html":true,
					"plugins/languages/xquery/xqueryPlugin.html":true,
					"plugins/languages/yaml/yamlPlugin.html":true,
					"plugins/pageLinksPlugin.html":true,
					"webtools/plugins/webToolsPlugin.html":true,
					"javascript/plugins/javascriptPlugin.html":true,
					"shell/plugins/shellPagePlugin.html":true,
					// "plugins/nodePlugin.html":true, // doesn't work at the moment needs socket.io added to the server
					"search/plugins/searchPagePlugin.html":true
				},
				"/settingsContainer":{
					"categories":{
						"showUserSettings":false,
						"showGitSettings":false
					}
				}
		};
		//Add the flux plugin to these defaults.
		defaultPlugins["/plugins"][fluxPlugin] = true;
		defaultPlugins = JSON.stringify(defaultPlugins, null, "   ");

		app.get("/defaults.pref", function (req, res) {
			res.send(defaultPlugins);
		});
	}

	app.use(orion);

	//If port is given then start server, otherwise assume someone else is using this 'app'
	// as a kind of servlet. It's up to them to wire things up and start the server.
	if (port) {
		app.listen(port);
	}
	return app;
};
