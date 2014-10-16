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

var SERVICE_TYPE_ID = 'org.eclipse.flux.jdt';
var SERVICE_NAME = 'Flux JDT Service';

var DISCOVERY_TIMEOUT = 3000; // Interval we wait for service discovery responses
	                          // The time should be relatively short. Services providers
	                          // should respond quickly with info about their status.

var SERVICE_RESTART_DELAY = 10000;
							  // After service becomes unavailable, we wait for a little
							  // while and then we try to restart it.
							  // This time should probably be larger than 'DISCOVERY_TIMEOUT'.

var when = require('when');

function intializeJDT(msgService, socket, username) {

	//Example code snippets for using 'msgService')
	//msgService.setStatus({Message: "Looking for JDT Service..."});
	//msgService.setStatus((Message: "JDT Service ready for user '"+username});
	//msgService.setStatus({Severity: "Error", Message: "Sorry, JDT Service Currently Unavailable"});

	var disposed = false;
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
		var deferred = when.defer();
		
		msgService.setStatus({ Message: "Looking for "+SERVICE_NAME });

		on('discoverServiceResponse', handleDiscoveryResponse);
		socket.emit('discoverServiceRequest', {
			username: username,
			service: SERVICE_TYPE_ID
		});
		setTimeout(handleTimeout, DISCOVERY_TIMEOUT); //ensures we don't wait forever.

		onDispose(finish); //Ensure finish called even if we get disposed early.

		return deferred.promise;
	} // end of 'discover'

	function showStatus(fluxMsg) {
		var status = fluxMsg.status;
		switch (status) {
			case 'ready':
				msgService.setStatus({ Message: SERVICE_NAME+' is Ready!' });
				break;
			case 'unavailable':
				var msg = SERVICE_NAME+' Unavailable';
				if (fluxMsg.error) {
					msg = msg+": "+fluxMsg.error;
					msgService.setStatus({ Severity: "Error", Message: msg });
				} else if (fluxMsg.info) {
					msg = msg+": "+fluxMsg.info;
					msgService.setStatus({ Message: msg });
				}
				setTimeout(startService, SERVICE_RESTART_DELAY)
				break;
			case 'available':
				//Don't display. Confusing to users since it says 'available' but
				// it doesn't mean 'ready to use', only 'available to start'.
				//A 'starting message should appear shorthy thereafter anyhow.
				break;
			default:
				msgService.setStatus({ Message: SERVICE_NAME+' is '+status });
		}
	}
	
	var lastKnownStatus = null; //safety to avoid double handling if we receive spurious 'change' events.
	function handleServiceStatusChange(data) {
		if (data.service===SERVICE_TYPE_ID && data.username===username) {
			showStatus(data);
			
			if (data.status!==lastKnownStatus) {
				lastKnownStatus = data.status;
				if (data.status==='unavailable') {
					//Service died?
					// Wait a little and then try to bring it back up
					setTimeout(startService, SERVICE_RESTART_DELAY);
					
					//Not really sure if this is such a good idea.
					// Maybe for failed start requests, but not if a service we where
					// using goes down.
					
				}
			}
		}
	}

	//begin 'intializeJDT' function body
	
	on('serviceStatusChange', handleServiceStatusChange);
	
	//Line below is commented because same info is also received in 'serviceStatusChange':
	//on('startServiceResponse', handleServiceStatusChange);
	
	function startService() {
		if (disposed) {
			//ignore restart attempt that may have been queued by a call to 'setTimeout' just prior to
			//getting disposed.
			return;
		}
		discover().then(function (discoveryResponse) {
			if (!discoveryResponse) {
				msgService.setStatus({ Severity: "Error", Message: "Looking for "+SERVICE_NAME+": Timed Out" });
			} else {
				showStatus(discoveryResponse);
				if (discoveryResponse.status === 'available') {
					//instance is available but not yet started. Must ask to start it
					socket.emit('startServiceRequest', {
						service: discoveryResponse.service,
						username: username,
						socketID: discoveryResponse.responseSenderID
					});
				}
			}
		});
	}
	
	startService();

	return {
		dispose: function () {
			disposed = true;
			if (disposers) {
				for (var i = 0; i < disposers.length; i++) {
					disposers[i]();
				}
				disposers = null;
			}
		}
	};
}

return intializeJDT;
});