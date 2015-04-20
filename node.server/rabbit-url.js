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

function rabbitUrl() {
	if (process.env.VCAP_SERVICES) {
		var svcInfo = JSON.parse(process.env.VCAP_SERVICES);
		console.log('VCAP_SERVICES = ', svcInfo);
		for (var label in svcInfo) {
			var svcs = svcInfo[label];
			for (var index in svcs) {
				var uri = svcs[index].credentials.uri;
				if (uri.lastIndexOf("amqp", 0) === 0) {
					console.log('rabbit url = ', uri);
					return uri;
				}
			}
		}
		throw new Error('Running on CF requires that you bind a amqp service to this app');
	} else {
		var uri = "amqp://localhost";
		console.log('rabbit url = ', uri);
		return uri;
	}
}

module.exports = rabbitUrl();