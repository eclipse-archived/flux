/**
 * Testing ground to play around with 'rabbit.js' library which can
 * be used to talk to an amqp message broker such as rabbitmq.
 *
 * This code is not part of flux, just bits and pieces to
 * try things out.
 *
 * Docs on rabbit.js here:
 *   http://www.squaremobius.net/rabbit.js/
 */
var rabbit = require('rabbit.js');
var rabbitUrl = 'amqp://localhost';

var context = rabbit.createContext(rabbitUrl);

context.on('ready', function () {

	var pub = context.socket('PUBLISH');
	pub.connect('flux');

	var sub = context.socket('SUBSCRIBE');
	sub.setEncoding('utf8');
	sub.on('data', receive);

	function receive(msg) {
		console.log("<== ", msg);
	}

	function send(msg) {
		console.log("==> ", msg);
		pub.write(msg, 'utf8');
	}

	function hello(count, callback) {
		if (count > 0) {
			setTimeout(function () {
				send("Hello "+count);
				hello(count-1, callback);
			}, 2000);
		} else {
			callback();
		}
	}

	function done() {
		console.log("Our work is done, close everything");
		pub.close();
		sub.close();
		context.close();
	}

	sub.connect('flux', function () {
		hello(5, done);
	});

});




