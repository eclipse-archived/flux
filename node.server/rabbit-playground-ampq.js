/*global require*/

/**
 * Testing ground to play around with 'rabbit.js' library which can
 * be used to talk to an amqp message broker such as rabbitmq.
 *
 * This code is not part of flux, just bits and pieces to
 * try things out.
 *
 * Useful info on amqplib is here:
 *  - short overview: http://www.squaremobius.net/amqp.node/
 *  - more details: http://www.squaremobius.net/amqp.node/doc/channel_api.html
 *  - example code: https://github.com/squaremo/amqp.node/tree/master/examples/tutorials
 */
var amqp = require('amqplib');
var rabbitUrl = 'amqp://localhost';
var when = require('when');

/**
 * Do with connection 2: Create a channel then do something with it
 */
function doWithConnection(connection) {
	return connection.createChannel().then(function (channel) {
		channel.on('error', function (err) {
			console.error('Channel error; ', err);
		});
		channel.on('close', function () {
			console.log('Channel closed');
		});
		return when.join(
			sayHello(channel),
			receiveHello(channel, "Bob"),
			receiveHello(channel, "Jane")
		);
	});
}

function logFor(prefix) {
	return function () {
		var params = Array.prototype.slice.call(arguments);
		params.unshift('['+prefix+']: ');
		console.log.apply(console, params);
	};
}

var fluxQueueOpts = {
		//exclusive: false (default)
		//autoDelete: false (default)
		durable: false
};


function receiveHello(channel, id) {
	var log = logFor(id +" <= ");

	var rcvCount = 0;
	function consumer(msg) {
		log('Received '+ (++rcvCount)+': '+msg.content);
	}

	log("Starting");
	return channel.assertQueue('flux', fluxQueueOpts)
	.then(function (info) {
		log("asserted queue: ", info);
		return channel.consume('flux', consumer, {noAck: true});
	}).then(function () {
		log("Consumer started");
		log("Waiting 3 seconds...");
		return when().delay(3000);
	});
}

function sayHello(channel) {
	var log = logFor("sayHello");
	log("Starting");
	return channel.assertQueue('flux',fluxQueueOpts)
	.then(function (info) {
		log("Asserted queue: ", info);
		var q = info.queue;
		channel.publish('', q, new Buffer("Hello Kris!"));
		log("Message sent");
	});
}

when(amqp.connect(rabbitUrl)).done(function (connection) {
	console.log('Connected');

	connection.on('error', function (err) {
		console.error('Connection error:', err);
	});
	connection.on('close', function () {
		console.log('Connection closed');
	});

	return doWithConnection(connection).then(function() {
		return connection.close();
	}).then(function () {
		console.log('The END');
	});

});

//var context = rabbit.createContext(rabbitUrl);
//
//context.on('ready', function () {
//
//	var pub = context.socket('PUBLISH');
//	pub.connect('flux');
//
//	var sub = context.socket('SUBSCRIBE');
//	sub.setEncoding('utf8');
//	sub.on('data', receive);
//
//	function receive(msg) {
//		console.log("<== ", msg);
//	}
//
//	function send(msg) {
//		console.log("==> ", msg);
//		pub.write(msg, 'utf8');
//	}
//
//	function hello(count, callback) {
//		if (count > 0) {
//			setTimeout(function () {
//				send("Hello "+count);
//				hello(count-1, callback);
//			}, 2000);
//		} else {
//			callback();
//		}
//	}
//
//	function done() {
//		console.log("Our work is done, close everything");
//		pub.close();
//		sub.close();
//		context.close();
//	}
//
//	sub.connect('flux', function () {
//		hello(5, done);
//	});
//
//});
