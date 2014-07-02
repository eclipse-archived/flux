/*******************************************************************************
 * @license
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html).
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
/*global require console exports process __dirname*/

var SESSION_SECRET = 'When we get serious this "secret" should be really secret';

// create and configure express
var URI = require('URIjs');
var express = require('express');
var mongo = require('mongodb');
var app = express();
var passport = require('passport');
var GitHubStrategy = require('passport-github').Strategy;
var githubSecret = require('./github-secret');
var cookieParse = require('cookie').parse;
var deref = require('./util/deref');

var host = process.env.VCAP_APP_HOST || 'localhost';
var port = process.env.VCAP_APP_PORT || '3000';
var homepage = '/client/index.html';
var pathResolve = require('path').resolve;
var connect = require('connect');

var github = require('./github');

var sessionStore = new express.session.MemoryStore(); //TODO: use database
var session = express.session({
	secret: SESSION_SECRET,
	store: sessionStore
});

app.use(express.cookieParser());
app.use(express.bodyParser());
app.use(express.methodOverride());
app.use(session);
app.use(passport.initialize());
app.use(passport.session());

passport.serializeUser(function(user, done) {
  done(null, user);
});

passport.deserializeUser(function(obj, done) {
  done(null, obj);
});

passport.use(new GitHubStrategy({
    clientID: githubSecret.id,
    clientSecret: githubSecret.secret,
    callbackURL: "http://"+host+":"+port+"/auth/github/callback"
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

//app.use('/', ensureAuthenticated);
//app.use('/hello', ensureAuthenticated);
//app.use('/hello', function (req, res, next) {
//	console.log('MW ONE');
//	next();
//});
//
//app.use('/hello', function (req, res, next) {
//	console.log('MW TWO');
//	next();
//});

function ensureAuthenticated(req, res, next) {
	var ok = req.isAuthenticated();
	if (req.isAuthenticated()) {
		return next();
	}
	res.redirect('/auth/github');
}

app.use('/client', ensureAuthenticated);
app.use(app.router);
app.use("/client/js/URIjs", express['static'](__dirname + '/node_modules/URIjs/src'));
app.use("/client", express['static'](__dirname + '/web-editor'));

app.get('/auth/github',
  passport.authenticate('github'));

app.get('/auth/github/callback',
  passport.authenticate('github', { failureRedirect: '/auth/github' }),
  function(req, res) {
	console.log('callback received, userName = '+userName(req));
	var target = URI(homepage).query({user: userName(req)}).toString();
	console.log('redirecting: '+target);
	res.redirect(URI(homepage).query({user: userName(req)}).toString());
	//res.send('Authenticated as '+userName(req) + '(' + userDisplayName(req) +')');
  }
);

////////////////////////////////////////////////////////
// Register http end points

function userDisplayName(req) {
	return req && req.user && req.user.displayName;
}

function userName(req) {
	return req && req.user && req.user.username;
}

app.get("/",
	function (req, res) {
		res.redirect(homepage);
	}
);

////////////////////////////////////////////////////////

var messagingHost = process.env.FLIGHT_MESSAGING_HOST || 'localhost';
var messagingPort = process.env.FLIGHT_MESSAGING_PORT || 3000;

var server = app.listen(port, host);
console.log('Express server started on port ' + port);

// create and configure socket.io
var io = require('socket.io').listen(server);
io.set('transports', ['websocket']);
io.set('log level', 1); //makes too much noise otherwise

io.set('authorization', function (handshakeData, accept) {
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
	} else {
		console.log('No cookie, check for custom header with github token');
		var token = handshakeData.headers['x-flux-user-token'];
		var user = handshakeData.headers['x-flux-user-name'];
		console.log('token = ',token);
		if (!token) {
			console.log('No token');
			return accept('No cookie or github token', false);
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
			//TODO: here we know user is a valid user. But nowhere do we check
			//what data a user is allowed to see (i.e what messages).
			//So really, any valid user can access any data at the moment.
			return accept(null, true);
		});
	}
});

// create and configure services
var MessageCore = require('./messages-core.js').MessageCore;
var messageSync = new MessageCore();

io.sockets.on('connection', function (socket) {
	messageSync.initialize(socket, io.sockets);
});

// check for MongoDB and create in-memory-repo in case MongoDB is not available
var MongoClient = mongo.MongoClient;
MongoClient.connect("mongodb://localhost:27017/flight-db", function(err, db) {

	var Repository;
	var repository;

	if (err) {
		console.log('create in-memory backup repository');
		Repository = require('./repository-inmemory.js').Repository;
	}
	else {
		console.log('create mongodb-based backup repository');
		Repository = require('./repository-mongodb.js').Repository;
	}

	repository = new Repository();

	var RestRepository = require('./repository-rest-api.js').RestRepository;
	var restrepository = new RestRepository(app, repository);

	var MessagesRepository = require('./repository-message-api.js').MessagesRepository;
	var messagesrepository = new MessagesRepository(repository);

	var client_io = require('socket.io-client');

	var client_socket = client_io.connect(messagingHost, {
		port : messagingPort
	});

	client_socket.on('connect', function() {
		console.log('client socket connected');

		client_socket.emit('connectToChannel', {
			'channel' : 'internal'
		}, function(answer) {
			if (answer.connectedToChannel) {
				repository.setNotificationSender.call(repository, client_socket);
				messagesrepository.setSocket.call(messagesrepository, client_socket);
			}
		});
	});

});
