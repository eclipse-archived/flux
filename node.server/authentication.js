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

//Authentication TODO list
//  - use https connections
//  - support client connections for a 'uber' user that is privvy to messages from/to all users
//    E.g. needed to implement a repositoryService.
//  - Check whether authenticated user is allowed to send/recieve specific messages

var deref = require('./util/deref');

var githubSecret = require('./github-secret');
var github = require('./github');
var GitHubStrategy = require('passport-github').Strategy;

var express = require('express');
var connect = require('connect');
var cookieParse = require('cookie').parse;

var SESSION_SECRET = githubSecret.secret; //reuse our github client secret also as session secret.

var sessionStore = new express.session.MemoryStore(); //TODO: use database
var session = express.session({
	secret: SESSION_SECRET,
	store: sessionStore
});

/**
 * socket.io handshake handler that authenticates connection based on express session
 * cookie. This method is used to implicitly authenticate connections estabilshed from
 * browser code running in a express session context.
 */
function sessionCookieHandshake(handshakeData, accept) {
	console.log('io.handshakeData: ', handshakeData);

	//See: http://howtonode.org/socket-io-auth for the source of this code below
	if (handshakeData.headers.cookie) {
		console.log('header.cookie = ', handshakeData.headers.cookie);
		var cookie = cookieParse(handshakeData.headers.cookie);
		console.log('parsedCookie = ', cookie);

		var sessionID = connect.utils.parseSignedCookie(cookie['connect.sid'], SESSION_SECRET);
		console.log('sessionID = ', sessionID);
		//Note: check below is different from the one in the sample code linked above
		// Nevertheless this *is* the correct check. When cookie cannot be 'unsigned' with our secret
		// sessionID will be false.
		if (!sessionID) {
			console.log('Cookie forged?');
			return accept('Cookie is invalid.', false);
		}
		return sessionStore.get(sessionID, function (err, session) {
			if (err) {
				console.log('Trouble accessing session store', err);
				return accept(err, false);
			}
			console.log('session = ', session);
			handshakeData.fluxUser = deref(session, ['passport', 'user', 'username']);
			if (!handshakeData.fluxUser) {
				console.log('passport session data not found, user not authenticated?');
				return accept('passport session data missing', false);
			}
			return accept(null, true);
		});
	}
	console.log('No cookie');
	return accept('No cookie', false);
}

/**
 * socket.io handshake handler that authenticates connection based on
 * 'x-flux-user' and 'x-flux-token' request headers. This method is
 * used by the Java client.
 */
function xFluxHeaderHandshake(handshakeData, accept) {
	console.log('check x-flux headers for user/token');
	var token = handshakeData.headers['x-flux-user-token'];
	var user = handshakeData.headers['x-flux-user-name'];
	console.log('token = ',token);
	if (!token) {
		console.log('No token');
		return accept('No github token', false);
	}
	return github.verify(user, token, function (err, user) {
		if (err) {
			console.log('github verify failed: ', err);
			return accept(err, false);
		}
		console.log('user = ', user);
		if (!user) {
			console.log('Token not associate with a valid user ', user);
			return accept('token not valid', false);
		}
		return accept(null, true);
	});
}

/**
 * Combine several handshake functions into a single function that chains them
 * together so they are tried in order until one of them succeeds.
 */
function handshakeCompose() {

	/**
	 * On the way out of a chain of handshake handlers this function appends
	 * error messages together so that if all handlers fail, the error messages
	 * from each handler in the chain are combined.
	 */
	function appendError(err, acceptFun) {
		//TODO: proper implementation should append err messages together
		// This implementation is nice and simple (so no bugs :-) but it looses
		// errors from all but the last handshake handler.
		return acceptFun;
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
 * Handler that is called by socket.io when websocket connections are being established.
 * This function is supposed to verify whether connection can be authorized.
 */
var socketIoHandshake = handshakeCompose(
	sessionCookieHandshake,
	xFluxHeaderHandshake
);

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
/// Configure passport library

var passport = require('passport');

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

//////////////////// export ///////////////////

exports.ensureAuthenticated = ensureAuthenticated;
exports.socketIoHandshake = socketIoHandshake;
exports.session = session;
exports.passport = passport;
