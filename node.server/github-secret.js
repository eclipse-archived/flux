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

var id = process.env.FLUX_GITHUB_CLIENT_ID;
var secret = process.env.FLUX_GITHUB_CLIENT_SECRET;

var haveSecrets = secret && id;

if (haveSecrets) {
	//Don't try to catch errors. This is deliberate.
	// If the file exists we will not allow the server to
	// startup if there are issues reading / parsing it.
	exports.id = id;
	exports.secret = secret;
} else {
	console.log(
		"User Authentication is DISABLED\n"+
		"set GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET env variables to enable"
	);
	//Null credentials tell the server to run without authentication:
	exports.id = null;
	exports.secret = null;
}
