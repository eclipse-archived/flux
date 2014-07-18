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

/*global require*/

//Authentication TODO list
//  - use https connections

var githubSecret = require('./github-secret');
var isEnabled = githubSecret.id && githubSecret.secret && true;

exports.isEnabled = isEnabled;

var deref = require('./util/deref');

var github = require('./github');
var GitHubStrategy = require('passport-github').Strategy;

var express = require('express');
var connect = require('connect');
var cookieParse = require('cookie').parse;

var SUPER_USER = '$super$'; //A special user id that identifies the super user.
                            //This should be string that is not valid as a userid on github
                            //Otherwise attackers could create such a user on github and
                            //become the superuser by authenticating as this user.

var SESSION_SECRET = githubSecret.secret || 'no auth so use whatever'; //reuse our github client secret also as session secret.

var sessionStore = new express.session.MemoryStore(); //TODO: use database
var session = express.session({
	secret: SESSION_SECRET,
	store: sessionStore
});

var URIjs = require('URIjs');

/**
 * socket.io handshake handler that authenticates connection based on express session
 * cookie. This method is used to implicitly authenticate connections estabilshed from
 * browser code running in a express session context.
 */
function sessionCookieHandshake(handshakeData, accept) {
	//console.log('io.handshakeData: ', handshakeData);

	//See: http://howtonode.org/socket-io-auth for the source of this code below
	if (handshakeData.headers.cookie) {
		//console.log('header.cookie = ', handshakeData.headers.cookie);
		var cookie = cookieParse(handshakeData.headers.cookie);
		//console.log('parsedCookie = ', cookie);

		var sessionID = connect.utils.parseSignedCookie(cookie['connect.sid'], SESSION_SECRET);
		//console.log('sessionID = ', sessionID);
		//Note: check below is different from the one in the sample code linked above
		// Nevertheless this *is* the correct check. When cookie cannot be 'unsigned' with our secret
		// sessionID will be false.
		if (!sessionID) {
			//console.log('Cookie forged?');
			return accept('Cookie is invalid.', false);
		}
		return sessionStore.get(sessionID, function (err, session) {
			if (err) {
				//console.log('Trouble accessing session store', err);
				return accept(err, false);
			}
			//console.log('session = ', session);
			handshakeData.fluxUser = deref(session, ['passport', 'user', 'username']);
			if (!handshakeData.fluxUser) {
				//console.log('passport session data not found, user not authenticated?');
				return accept('passport session data missing', false);
			}
			return accept(null, true);
		});
	}
	//console.log('No cookie');
	return accept('No cookie', false);
}

/**
 * Extract user (name) and oauth token from handshake data.
 */
function getUserToken(handshakeData, callback) {
	// First look in the headers (provided like this by Java client)
	var token = handshakeData.headers['x-flux-user-token'];
	var user = handshakeData.headers['x-flux-user-name'];
	if (token && user) {
		return callback(user, token);
	}

	// Then look in query params (provided like this by nodejs client)
	return callback(
		deref(handshakeData, ["query", "user"]),
		deref(handshakeData, ["query", "token"])
	);
}

function authenticateSuperUser(user, token) {
	return user===SUPER_USER && token===githubSecret.secret;
}

/**
 * socket.io handshake handler that authenticates connection based on
 * user and github oauth token. The user and token can be provided either
 * as query params or as 'x-flux-user' and 'x-flux-token' request headers.
 *
 * The header method is used by Java clients and the query params by
 * nodejs clients.
 *
 * In theory nodejs and java should both be able to use the same method
 * of authentication.
 *
 * However...
 *    - the Java library makes it
 *         - easy to set request headers
 *         - provides no way to set query parameters
 *    - the node client is exactly the oposite!
 */
function tokenHandshake(handshakeData, accept) {
	return getUserToken(handshakeData, function (user, token) {
		console.log('token = ', token && (token.substring(0,4)+'...'));
		if (!token) {
			//console.log('No token');
			return accept('No x-flux-user-token header', false);
		}
		if (!user) {
			return accept('No user specified', false);
		}
		if (!token) {
			return accept('No github token specified', false);
		}
		if (authenticateSuperUser(user, token)) {
			handshakeData.fluxUser = SUPER_USER;
			return accept(null, true);
		}
		return github.verify(user, token, function (err, user) {
			if (err) {
				//console.log('x-flux-user-token verify failed: ', err);
				return accept(err, false);
			}
			//console.log('user = ', user);
			if (!user) {
				//console.log('Token not associate with a valid user ', user);
				return accept('x-flux-user-token not valid for user '+user, false);
			}
			handshakeData.fluxUser = user;
			return accept(null, true);
		});
	});
}

/**
 * Combine several handshake functions into a single function that chains them
 * together so they are tried in order until one of them succeeds.
 */
function handshakeCompose() {

	/**
	 * On the way out of a chain of handshake handlers this function wraps the
	 * acceptFun so that if all handlers fail, the error messages
	 * from each handler in the chain are combined.
	 */
	function appendError(err, acceptFun) {
		if (!err) {
			return acceptFun;
		} else {
			return function (err2, accepted) {
				if (accepted) {
					//err message doesn't really matter in this case
					return acceptFun(err2, accepted);
				} else if (!err2) {
					//No second error message, so only report first error
					return acceptFun(err, accepted);
				} else {
					//Retain both error explanations in a single message.
					return acceptFun(err + " & "+err2, accepted);
				}
			};
		}
	}

	/**
	 * Compose 2 handlers
	 */
	function compose(h1, h2) {
		return function (handshakeData, accept) {
			return h1(handshakeData, function (err, accepted) {
				if (accepted) {
					return accept(err, accepted);
				} else {
					return h2(handshakeData, appendError(err, accept));
				}
			});
		};
	}

	var shakers = Array.prototype.slice.call(arguments, 0);

	function loop(i) {
		if (i===shakers.length-1) {
			//Only one shaker left, no need to compose, just use as is.
			return shakers[i];
		} else {
			return compose(shakers[i], loop(i+1));
		}
	}
	return loop(0);
}

/**
 * Add some logging to a socket.io handshake handler. This wraps the
 * handler returning a new 'logging handler' delegating to it.
 */
function addLogging(shaker) {
	return function (hsd, accept) {
		return shaker(hsd, function (err, accepted) {
			if (accepted) {
				console.log('User authenticated: ', hsd.fluxUser);
			} else {
				console.log('Autentication REJECTED: ',err);
			}
			return accept(err, accepted);
		});
	};
}

/**
 * Handler that is called by socket.io when websocket connections are being established.
 * This function is supposed to verify whether connection can be authorized.
 */
var socketIoHandshake = addLogging(handshakeCompose(
	sessionCookieHandshake, // browser clients use this
	tokenHandshake			// Java and nodejs clients use this
));

/**
 * express middleware that checks whether req is authenticated.
 * 'next' midleware is only called for authenticated requests.
 */
function ensureAuthenticated(req, res, next) {
	var ok = req.isAuthenticated();
	if (req.isAuthenticated()) {
		return next();
	}
	res.redirect('/auth/github');
}

///////////////////////////////////////
// Configuration options for nodejs
// socket.io client

/**
 * Given an options objects to create nodejs socketio client connections,
 * add credentials that grant super user access to the connection.
 */
function asSuperUser(options) {
	options.query = new URIjs()
		.query(options.query||"")
		.query({
			user: SUPER_USER,
			token: githubSecret.secret
		})
		.query();
	return options;
}



///////////////////////////////////////
/// Configure passport library

var passport = undefined;

//Configuring passport without a secret causes an error.
if (isEnabled) {
	passport = require('passport');
	passport.serializeUser(function(user, done) {
	  done(null, user);
	});

	passport.deserializeUser(function(obj, done) {
	  done(null, obj);
	});


	passport.use(new GitHubStrategy({
	    clientID: githubSecret.id,
	    clientSecret: githubSecret.secret,
	    callbackURL: "/auth/github/callback"
	  },
	  function(accessToken, refreshToken, profile, done) {
	    process.nextTick(function () {
		  console.log('GH strategy user lookup called');
	      // To keep the example simple, the user's GitHub profile is returned to
	      // represent the logged-in user. In a typical application, you would want
	      // to associate the GitHub account with a user record in your database,
	      // and return that user instead.
	      return done(null, profile);
	    });
	  }
	));
}

/**
 * Check whether a given socket has the authorization needed to
 * join a channel. The callback will be called with a error message
 * explaining the failure if the check fails and a falsy value otherwise.
 */
function checkChannelJoin(socket, requestData, callback) {
	console.log('Join channel request: ', requestData);
	var channel = requestData.channel;
	if (!channel) {
		console.log('REJECT: channel not specified');
		return callback('Channel not specified');
	}
	var handshakeData = socket.handshake;
	if (!handshakeData) {
		console.log('REJECT: no handshakeData');
		return callback('No handshakeData in the socket');
	}
	var user = handshakeData.fluxUser;
	if (!user) {
		console.log('REJECT: no fluxUser');
		return callback('No fluxUser info in handshakeData');
	}
	if (user===SUPER_USER || user===channel) {
		console.log('ACCEPT "'+user+'" to join "'+channel+'"');
		return callback(); //OK!
	} else {
		console.log('REJECT "'+user+'" to join "'+channel+'"');
		return callback('"'+user+'" is not allowed to join channel "'+channel+"'");
	}
}

/**
 * Dummy implementation of checkChannelJoin used when authentication is disabled.
 */
function dummyCheckChannelJoin(socket, requestData, callback) {
	return callback(); //OK!
}

//////////////////// export ///////////////////

exports.ensureAuthenticated = ensureAuthenticated;
exports.socketIoHandshake = socketIoHandshake;
exports.session = session;
exports.passport = passport;
exports.asSuperUser = asSuperUser;
exports.checkChannelJoin = isEnabled ? checkChannelJoin : dummyCheckChannelJoin;
exports.SUPER_USER = SUPER_USER;
exports.isEnabled = isEnabled;
