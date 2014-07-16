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

/*global require exports console*/

var fs = require('fs');
var path = require('path');

var SECRETS_FILE = path.resolve('github-secret.json');

var haveSecrets = fs.statSync(SECRETS_FILE).isFile();

if (haveSecrets) {
	//Don't try to catch errors. This is deliberate.
	// If the file exists we will not allow the server to
	// startup if there are issues reading / parsing it.
	var data = JSON.parse(fs.readFileSync(SECRETS_FILE, {encoding: 'UTF-8'}));
	if (!data.id) {
		throw "No github client-id found in "+SECRETS_FILE;
	}
	if (!data.secret) {
		throw "No github secret found in "+SECRETS_FILE;
	}
	exports.id = data.id;
	exports.secret = data.secret;
} else {
	console.log("User Authentication is DISABLED: file not found: "+SECRETS_FILE);
	//Null credentials tell the server to run without authentication:
	exports.id = null;
	exports.secret = null;
}

