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

var SERVICE_TYPE_ID = 'foo.bar.test';
var SERVICE_NAME = 'Foo Bar Testing Service';

var DISCOVERY_TIMEOUT = 1000;

var when = require('when');

function intializeJDT(msgService, socket, username) {

	//Example code snippets for using 'msgService')
	//msgService.showProgressMessage("Looking for JDT Service...");
	//msgService.showProgressResult("JDT Service ready for user '"+username);
	//msgService.showProgressError("Sorry, JDT Service Currently Unavailable");

	var disposers = []; //things to do on dispose
	function onDispose(todo) {
		disposers.push(todo);
	}
	
	/**
	 * Don't use 'socket.on' directly. Use this instead to make sure we
	 * cleanup all our listeners when we are done.
	 */
	function on(event, fun) {
		socket.on(event, fun);
		onDispose(function () {
			socket.removeListener(event, fun);
		});
	}

	/**
	 * When editor starts a JDT service may or may not already be up and running.
	 * This function quieries flux to discover 'what is out there' re JDT services
	 * for the current user.
	 *
	 * Returns a deferred that resolves within a short time. The resolve value
	 * will be the 'best' response received within the timeframe.
	 */
	function discover() {

		var deferred = when.defer();

		/**
		 * We'll try to keep the 'best' response while listening for a short time.
		 * We may stop listening as soon as we received a response that can't be
		 * improved upon.
		 */
		var discoveryResponse = null;
		var discoveryRank = 0; // 0 = no response

		/**
		 * Maps discoveryResponse.status to a ranking (higher number means a
		 * 'better' response.
		 */
		var disoveryRankMap = {
			unavailable : 1,//An error is better than nothing
			available: 2,	//Available to start.
			starting: 3,	//Already in the process of starting
			ready: 4		//Ready to handle service requests
		};

		var MAX_RANK = 4;	//Largest number used in 'discoveryRanking'

		function handleDiscoveryResponse(data) {
			if (data.service===SERVICE_TYPE_ID && data.username===username) {
				console.log('discovery response: ', data);
				var newRank = disoveryRankMap[data.status];
				if (newRank>discoveryRank) {
					discoveryRank = newRank;
					discoveryResponse = data;
				}
				if (newRank===MAX_RANK) {
					finish();
				}
			}
		}

		function handleTimeout() {
			finish();
		}

		function finish() {
			deferred.resolve(discoveryResponse);
			socket.removeListener('discoverServiceResponse', handleDiscoveryResponse);
		}

		//body of 'discover'
		on('discoverServiceResponse', handleDiscoveryResponse);
		socket.emit('discoverServiceRequest', {
			username: username,
			service: SERVICE_TYPE_ID
		});
		setTimeout(handleTimeout, DISCOVERY_TIMEOUT); //ensures we don't wait forever.

		onDispose(finish); //Ensure finish called even if we get disposed early.

		return deferred.promise;
	} // end of 'discover'

	/**
	 * Send a request to start a particular service to a provider identified by given
	 * discoveryResponse
	 */
	function requestToStart(discoveryResponse) {
		var provider = discoveryResponse.responseSenderID;
		socket.emit({
			username: discoveryResponse.username,
			service: discoveryResponse.service,
			socketID: discoveryResponse.responseSenderID
		});
	}

	function showStatus(fluxMsg) {
		var status = fluxMsg.status;
		switch (status) {
			case 'ready':
				msgService.showProgressResult('JDT Service is Ready!');
				break;
			case 'unavailable':
				var msg = 'JDT Service Unavailable';
				if (fluxMsg.error) {
					msg = msg+": "+fluxMsg.error;
				}
				msgService.showProgressError(msg);
				break;
			default:
				msgService.showProgressMessage('JDT Service is '+status);
		}
	}

	//begin 'intializeJDT' function body
	
	on('serviceReady', function (data) {
		data.status = 'ready';
		showStatus({status: 'ready', service: data.service, username: data.username});
	});
	on('startServiceResponse', function (data) {
		showStatus(data);
		//TODO: In some cases this might fail because someone else grabbed the same
		// instance right at the same time. Should we attempt to discover another provider
		// and try again.
	});
	
	discover().then(function (discoveryResponse) {
		if (!discoveryResponse) {
			msgService.showProgressError("Looking for "+SERVICE_NAME+": Timed Out");
		} else {
			showStatus(discoveryResponse);
		}

		if (discoveryResponse.status === 'available') {
			//instance is available but not yet started. Must ask to start it
			socket.emit('startServiceRequest', {
				service: discoveryResponse.service,
				username: username,
				socketID: discoveryResponse.responseSenderID
			});
		}
		
	});
	

	return {
		dispose: function () {
			for (var i = 0; i < disposers.length; i++) {
				disposers[i]();
			}
		}
	};
}

return intializeJDT;
});