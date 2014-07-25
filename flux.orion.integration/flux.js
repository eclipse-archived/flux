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

/*global require window location console*/
require.config({
    packages: [
        {name: "when", location: "bower_components/when", main: "when"},
        {name: "rest", location: "bower_components/rest", main: "rest"}
    ]
});


require(["orion/Deferred", "orion/plugin", "FluxEditor", "FluxFileSystem", "OpenDeclaration", "lib/domReady!" ],
function(Deferred,         PluginProvider, FluxEditor,   FluxFileSystem,   OpenDeclaration) {

//We used 'domReady' so don't need to use window.onload. (domready! implies window is loaded before
// this code is allwed to run.
//
// If we do not use domReady, then when we get here it may in
// already be too late to capture the window.onLoad event.

	var host = location.hostname;
	var port = location.port || 80;
	var base = "flux://" + host + ":" + port + "/";

	var contentTypes = ["application/javascript", "text/plain" ];

	var headers = {
		'Name' : "Flux",
		'Version' : "0.1",
		'Description' : "Flux Integration",
		'top' : base,
		'pattern' : base,
		'login' : 'http://'+host+':'+port+'/login.html'
	};

	var provider = new PluginProvider(headers);

	var fileService = new FluxFileSystem(host, port, base); //TODO: user removed
	provider.registerService("orion.core.file", fileService, headers);

	var editorService = new FluxEditor(host, port, base); //TODO: user removed

	provider.registerService([
			"orion.edit.validator",
			"orion.edit.live"
		],
		editorService,
		{
			'pattern' : base + ".*"
		}
	);
	provider.registerService([
			"orion.edit.model",
			"orion.edit.contentAssist"
		],
		editorService,
		{
			'contentType' : contentTypes
		}
	);
//TODO: fix these for authentication and put them back in:
//	var openDeclaration = new OpenDeclaration(host, port, base); //TODO: user removed
//	provider.registerService("orion.edit.command",
//		openDeclaration,
//		{
//			'name': "Navigate to Definition",
//			'id': "org.eclipse.flux.navigateToDefintion",
//			/* 'img': "", */
//			'key': [ 114 ], /* F3 key */
//			/* 'validationProperties': [], */
//			'contentType': [ "text/x-java-source" ]
//			/* 'nls' : "", */
//			/* 'nameKey' : "", */
//			/* 'tooltipKey': "" */
//		}
//	);

	provider.connect();

}); //define
