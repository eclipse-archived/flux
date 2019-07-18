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
	paths: {
		socketio: '/socket.io/socket.io'
	},
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
	var wsport = port;
	if (host.indexOf("cfapps.io")>0) {
		wsport = 4443; // Cloudfoundry weirdness: all websocket traffic re-routed on this port.
	}
	var base = "flux://" + host + ":" + wsport + "/";

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
//		nameKey: "Flux",
//		nls: "orion-plugin/nls/messages",
		imageDataURI: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAABuwAAAbsBOuzj4gAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAEqSURBVDiNpdMxS9thEAbwX/6Gbm6CkrVjoYuDhygoWfwEpW4dikLngpMgguDqEopjp4JfIHMHc5CWTlla2kkEhwwuBRFNhyQQ038SxZvu3ueeu/e5971Kr9fzHCuexX5KgcyslJ1XZknIzB18RA3n2I6IbmmBzJzHKjoRcZGZb/AFo92/ox4R1w8kZOYifqCJ35n5CZ/HyLCMD8OgOgIc4uXAf4HdKcpWhs7oEOtTCNMLZObGSPfH2FJmvoZKq9Waw1f94Y3bH/05HJRgHWxWsTeBfIu3+IZ1/0t8hf0CWxOueRQR7Yjo4T3+luS8K7BQAlziNDOHr9RFoyRvvkCrBKgNiqwN4rb+bxy3nwXOcDdBxqxVbRQR0dQf1hXuxxLKFugGv3AcESf/AFmNUKHs4+bxAAAAAElFTkSuQmCC",
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
		],
		editorService,
		{
			'pattern' : base + ".*",
			'contentType' : [ "text/x-java-source" ]
		}
	);
	provider.registerService([
			"orion.edit.model",
			"orion.edit.live"
		],
		editorService,
		{
			'contentType' : [ "text/plain" ]
		}
	);
	
	provider.registerService([
   			"orion.edit.contentAssist",
   			"orion.edit.hover"
		],
	    editorService,
	    {
	    	'contentType' : [ "text/x-java-source" ]
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

	var quickfixImpl = {
	        execute: function(editorContext, context) {
				if (!context.annotation)
					return null;
				if (context.annotation.id) {
					editorService.applyQuickfix(editorContext, context);
	        	}
	        }
	};
	
	var quickfixProp = {
			id: "orion.css.quickfix.zeroQualifier",
			image: "../images/compare-addition.gif",
			scopeId: "orion.edit.quickfix",
			name: "Apply quickfix",
			contentType: ["text/x-java-source"],
			tooltip: "Apply Quick Fix",
			validationProperties: []
		};
	
	provider.registerService("orion.edit.command", quickfixImpl, quickfixProp);
	provider.connect();

}); //define
