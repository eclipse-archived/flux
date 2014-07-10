var http = require('https');
var httpRequest = http.request;

var HOST = 'api.github.com';

/**
 * Remember tokens that have been validated and either rejected or accepted recently.
 */
var cache = {
	MAX_AGE: 1000*60*2,
	entries: {},
	get: function (token) {
		var entry = this.entries[token];
		if (entry) {
			var age = Date.now() - entry.created;
			if (age > this.MAX_AGE) {
				delete this.entries[token];
				return;
			}
			return entry.value;
		}
	},
	put: function (token, value) {
		this.entries[token] = {
			created: Date.now(),
			value: value
		};
	}
};

/**
 * Construct a options object that can be passed to http.request function, to
 * retrieve the authenticated user's info from github.
 */
function getUserRequest(token) {
	return {
		hostname: HOST,
		path: '/user',
		headers: {
			'User-Agent': 'Flux',
			Authorization: 'token '+token
		}
	};
}

/**
 * Retrieve github login id associated with token. Calls the callback
 * with either an error in case of a problem, or (null, userid) otherwise.
 */
function getUser(token, callback) {
	var req = httpRequest(getUserRequest(token), function (res) {
		console.log('>>> github response received ===');
		console.log('STATUS: ' + res.statusCode);
		console.log('HEADERS: ' + JSON.stringify(res.headers));
		console.log('<<< github response received ===');
		res.setEncoding('utf8');
		var data = "";

		res.on('data', function (chunk) {
			data = data + chunk; //TODO: more efficient way to collect the data?
			console.log('BODY: ' + chunk);
		});
		res.on('end', function () {
			console.log('end of data '+res.statusCode);
			if (res.statusCode === 200) {
				try {
					if (data) {
						data = JSON.parse(data);
						console.log('data = ', data);
						var user = data.login;
						if (user) {
							return callback(null, user);
						} else {
							//This shouldn't happen unless some major changes in github rest API.
							return callback("Unexpected reponse from github login not found in body");
						}
					}
				} catch (e) {
					console.error(e);
					return callback(e);
				}
			}
			//other response codes. We don't know how to handle so treat as errors.
			return callback(res);
		});

	});

	req.on('error', function (e) {
		console.error('github request failed:', e);
		callback(e);
	});
	req.end();
}

/**
 * Verify that a given github token is valid. Calls the callback with
 * a string representing the github user id if token could be validated
 * or otherwise false.
 */
function verify(user, token, callback) {
	return getUser(token, function (err, authUser) {
		if (err) {
			return callback(err);
		} else if (user===authUser) {
			return callback(null, user);
		} else {
			return callback("Token not authorized for this user: "+user);
		}
	});
}


exports.verify = verify;

