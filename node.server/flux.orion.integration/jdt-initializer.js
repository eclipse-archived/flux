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

define(function(require) {

function intializeJDT(msgService, socket, username) {

	var disposed = false;

	function dispose() {
		disposed = true;
	}

	function mockInit() {
		msgService.showProgressMessage("Looking for JDT Service...");

		setTimeout(function () {
			if (!disposed) {
				//msgService.showProgressResult("JDT Service ready for user '"+username);
				msgService.showProgressError("Sorry, JDT Service Currently Unavailable");
			}
		}, 5000);
	}

	mockInit();

	return {
		dispose: dispose
	};
}

return intializeJDT;
});