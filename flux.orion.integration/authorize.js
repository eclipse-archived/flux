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
/*global define*/
define(function(require) {

var when = require('when');
var rest = require('rest');
var mime = require('rest/interceptor/mime'); //JSON support
var client = rest.wrap(mime);

/**
 * Wraps a orion plugin request handler method with authentication logic.
 * Any method that uses this.socket or this.user must be
 * wrapped to ensure socket and user are set before its
 * method body executes.
 *
 * Expectations:
 *
 *   - methodBody returns a Deferred/Promise.
 *   - 'this' object provides a _createSocket(user) method to be
 *     called to setup flux socket connection after obtaining authenticated
 *     user.
 *
 * Guarantees that:
 *  - If autenhentication is succesful:
 *      - the user will be stored in 'this.user'.
 *      - _createSocket method is called before methodBody
 *      - methodBody is called and its result is returned.
 *  - If authentication fails:
 *      - returns a rejected promise with {status: 401}
 *      - (methodBody and _createSocket are not called.
 */
function authorize(methodBody) {
	return function () {
		var self = this;
		var params = arguments;
		if (self.user) {
			//already authententicated
			return methodBody.apply(self, arguments);
		} else {
			return getUser().then(function (user) {
				self.user = user;
				self._createSocket(self.user);
				return methodBody.apply(self, params);
			});
		}
	};
}

/**
 * Obtain id of currently authenticated user from rest endpoint on
 * flux server (i.e. same server as where we got this plugin from).
 */
function getUser() {
	return client('/user').then(function (response) {
		if (response.status.code!=200) {
			//TODO: could check status code more precisely.
			// Here we treat anything that's not ok as
			// authentication failure and signal it as such
			// to orion.
			return when.reject({status: 401});
		}
		return response.entity.username;
	});
}

return authorize;

});