/*global require window location console*/

require(["orion/Deferred", "orion/plugin", "FluxEditor", "FluxFileSystem", "OpenDeclaration", "lib/domReady!" ],
function(Deferred,         PluginProvider, FluxEditor,   FluxFileSystem,   OpenDeclaration) {

//We used 'domReady' so don't need to use window.onload. When we get here it may in
// fact already be too late to capture the window.onLoad event.

	var user = 'defaultuser'; //TODO: can't refer to user anywhere in this code!
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

	var fileService = new FluxFileSystem(host, port, user, base);
	provider.registerService("orion.core.file", fileService, headers);

	var editorService = new FluxEditor(host, port, user, base);

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

	var openDeclaration = new OpenDeclaration(host, port, user, base);
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
