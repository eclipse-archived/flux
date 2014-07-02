var http = require('https');
var httpRequest = http.request;

var HOST = 'api.github.com';

//TODO: add a cache to avoid hitting github for every request to verify a token.

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
 * Verify that a given github token is valid. Calls the callback with
 * a string representing the github user id if token could be validated
 * or otherwise false.
 */
function verify(user, token, callback) {
	var req = httpRequest(getUserRequest(token), function (res) {
		console.log('>>> github response received ===');
		console.log('STATUS: ' + res.statusCode);
		console.log('HEADERS: ' + JSON.stringify(res.headers));
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
						var actualUser = data.login;
						if (user!==actualUser) {
							return callback("Token not authorized for this user: "+user);
						}
						return callback(null, data.login);
					}
				} catch (e) {
					console.error(e);
					return callback(e, false);
				}
			}
			//other response codes. We don't know how to handle so treat as errors.
			return callback("status: "+res.statusCode+ "\n"+data);
		});

		console.log('<<< github response received ===');
	});

	req.on('error', function (e) {
		console.error('github request failed:', e);
		callback(e);
	});
	req.end();
}

// Fake implementation:
//	if (token==='something-secret') {
//		callback('kdvolder');
//	} else {
//		callback(false);
//	}

//	httpRequest(getUserRequest(token), function (res) {
//		var status = res.statusCode;
//
//
//	});
//}

exports.verify = verify;

