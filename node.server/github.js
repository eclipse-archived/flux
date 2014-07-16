var http = require('https');
var httpRequest = http.request;

var HOST = 'api.github.com';

/**
 * Remember tokens that have been validated and either rejected or accepted recently.
 * This to avoid hammering github api with requests for the same thing over and over.
 */
var cache = {
	MAX_AGE: 1000*60*20, /* 20 minutes, then we'll check again */
	entries: {},
	get: function (token) {
		var entry = this.entries[token];
		if (entry) {
			var age = Date.now() - entry.created;
			if (age > this.MAX_AGE) {
				delete this.entries[token];
				return;
			}
			return entry;
		}
	},
	put: function (token, err, value) {
		this.entries[token] = {
			created: Date.now(),
			error: err,
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
function _getUser(token, callback) {
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
			console.log("error response from github: ", res.statusCode);
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
 * Cache-enabled version of _getUser
 */
function getUser(token, callback) {
	var cacheEntry = cache.get(token);
	if (cacheEntry) {
		console.log('returning cached response ',cacheEntry);
		return callback(cacheEntry.error, cacheEntry.value);
	}
	//Not yet in cache
	return _getUser(token, function (err, user) {
		if (err) {
			//Only cache if this is a clear 'not authorized' response
			if (err.statusCode==401) {
				console.log('Token rejected by github');
				cache.put(token, err.statusCode);
			}
		} else {
			console.log('github says token belongs to ', user);
			cache.put(token, err, user);
		}
		return callback(err, user);
	});
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

