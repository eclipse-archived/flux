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

	function addCss(cssText) {
		var document = window.top.document;
		var css = document.createElement("style");
		css.type = "text/css";
		css.innerHTML = cssText;
		document.head.appendChild(css);
	}
		
	addCss(".flux-sprite {\n"+
		"    background: url('/images/cloud_icon_16.png') 12px;\n"+
		"    background-repeat: no-repeat;\n"+
		"    width:16px;height:16px;\n" +
		"}\n"
	);

	var host = location.hostname;
	var port = location.port || 80;
	var wsport = port;
	if (host.indexOf("cfapps.io")>0) {
		wsport = 4443; // Cloudfoundry weirdness: all websocket traffic re-routed on this port.
	}
	var base = "flux://" + host + ":" + wsport + "/";

	var contentTypes = ["application/javascript", "text/plain" ];

	var headers = {
		'Name' : "Flux",
		'Version' : "0.1",
		'Description' : "Flux Integration",
		'top' : base,
		'pattern' : base,
		'login' : 'http://'+host+':'+port+'/auth/github'
	};

	var provider = new PluginProvider(headers);

	var fileService = new FluxFileSystem(host, wsport, base);
	provider.registerService("orion.core.file", fileService, headers);

	var editorService = new FluxEditor(host, wsport, base);

   provider.registerServiceProvider("orion.page.link.category", null, {
		id: "flux",
		name: "Flux",
		nameKey: "Flux",
		nls: "orion-plugin/nls/messages",
		imageClass: "flux-sprite",
		order: 5
	});
	provider.registerServiceProvider("orion.page.link", null, {
		id: "flux.deployer",
		name: "CF Deployer",
		category: "flux",
		order: 10,     // Make this the first link in the 'sites' category.
		uriTemplate: "https://flux-cf-deployer.cfapps.io"
	});
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
	var openDeclaration = new OpenDeclaration(host, wsport, base);
	provider.registerService("orion.edit.command",
		openDeclaration,
		{
			'name': "Navigate to Definition",
			'id': "org.eclipse.flux.navigateToDefintion",
			/* 'img': "", */
			'key': [ 114 ], /* F3 key */
			/* 'validationProperties': [], */
			'contentType': [ "text/x-java-source" ]
			/* 'nls' : "", */
			/* 'nameKey' : "", */
			/* 'tooltipKey': "" */
		}
	);

	provider.connect();

}); //define
