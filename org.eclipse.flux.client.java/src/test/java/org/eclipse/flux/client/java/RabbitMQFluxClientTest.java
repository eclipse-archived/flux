/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.client.java;

import static org.eclipse.flux.client.MessageConstants.SUPER_USER;
import static org.eclipse.flux.client.MessageConstants.USERNAME;
import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.IChannelListener;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.client.MessageHandler;
import org.eclipse.flux.client.RequestResponseHandler;
import org.eclipse.flux.client.config.RabbitMQFluxConfig;
import org.eclipse.flux.client.util.BasicFuture;
import org.json.JSONObject;

public class RabbitMQFluxClientTest extends AbstractFluxClientTest {

	/**
	 * A Receiver connects to flux message bus and adds message handler
	 * to recieve a single message asynchronously. Once message is 
	 * received it disconnects.
	 */
	public abstract class Receiver<T> extends MessageHandler {

		private MessageConnector flux;
		public final BasicFuture<T> result;
		private String user;

		public Receiver(String user, String type) throws Exception {
			this(user, type, user);
		}
		
		public Receiver(String user, String type, String channel) throws Exception {
			super(type);
			this.user = user;
			flux = createConnection(user);
			flux.connectToChannelSync(channel);
			flux.addMessageHandler(this);
			result = new BasicFuture<T>();
			result.setTimeout(TIMEOUT);
			result.whenDone(new Runnable() {
				public void run() {
					flux.disconnect();
				}
			});
		}

		@Override
		public void handle(String type, JSONObject message) {
			try {
				result.resolve(receive(type, message));
			} catch (Throwable e) {
				result.reject(e);
			}
		}

		protected abstract T receive(String type, JSONObject message) throws Throwable;
		
		public T get() throws Exception {
			return result.get();
		}

		public String getUser() {
			return user;
		}

		public BasicFuture<T> future() {
			return result;
		}
		
	}

	private FluxClient client = FluxClient.DEFAULT_INSTANCE;
	
	public void testConnectAndDisconnect() throws Exception {
		MessageConnector conn = createConnection("Bob");
		conn.disconnect();
	}
	
	public void testSendAndReceive() throws Exception {
		final Process<Void> sender = new Process<Void>("Bob") {
			protected Void execute() throws Exception {
				send("bork", new JSONObject()
					.put(USERNAME, "Bob")
					.put("msg", "Hello")
				);
				return null;
			}

		};
		
		final Process<String> receiver = new Process<String>("Bob") {
			protected String execute() throws Exception {
				BasicFuture<JSONObject> msg = areceive("bork");
				sender.start();
				return msg.get().getString("msg");
			}
		};

		receiver.start();	//only start receiver (not sender).
							// receiver should start sender at the right time 
							// to avoid race condition between them.
		await(sender, receiver);
		assertEquals("Hello", receiver.result.get());
	}

	/**
	 * Test that messages sent to '*' user are received by multiple users.
	 */
	public void testMessageToEveryOne() throws Exception {
		
		String[] users = { "Bob", "Alice", "Sender", MessageConstants.SUPER_USER};
		
		//First create all the receivers (will register the message handlers to receive messages
		// this must be done before starting the sender to avoid a race condition.
		List<Receiver<String>> receivers = new ArrayList<>();
		for (String user : users) {
			receivers.add(new Receiver<String>(user, "bork") {
				@Override
				protected String receive(String type, JSONObject message) throws Throwable {
					return message.getString("msg");
				}
			});
		}

		//Run the process that sends messages.
		run(new Process<Void>("Sender") {
			protected Void execute() throws Exception {
				send("bork", new JSONObject()
					.put(USERNAME, "*") //send to all users.
					.put("msg", "Hello")
				);
				return null;
			}
		});

		//Now check that each receiver got the message
		for (Receiver<String> r : receivers) {
			//Yes "Sender" receiving messages from 'himself' because they are sent using a different MessageConnector.
			// The rule about not delivering messages to their 'origin' applies at the MessageConnector level, not
			// the user level. Two message connectors for the same user count as different origins.
			assertEquals("Hello", r.get());
		}
	}

	/**
	 * Tests that messages are not delivered back to their origin.
	 */
	public void testSelfSending() throws Exception {
		final String messageType = "bork";
		
		Receiver<String> receiver = receiver("Bob", messageType);
		
		run(new Process<Void>("Bob") {
			protected Void execute() throws Exception {
				BasicFuture<JSONObject> received = areceive(messageType);
				send(messageType, new JSONObject()
					.put(USERNAME, "Bob")
					.put("msg", "Hello")
				);
				//message sent by this process (i.e same messageConnector) is not received.
				assertError(TimeoutException.class, received);
				return null;
			}
		});
		
		//Message should still be delivered to another messageconnector for the same user.
		assertEquals("Hello", receiver.get());
	}
	
	/**
	 * Super user, connected to super user channel receives messages from everyone.
	 */
	public void testSuperReceiver() throws Exception {
		Receiver<String> root = receiver(SUPER_USER, "bork");
		Receiver<String> bob = receiver("Bob", "bork");
		Receiver<String> alice = receiver("Alice", "bork");
		
		run(new Process<Void>("Bob") {
			protected Void execute() throws Exception {
				send("bork", new JSONObject()
					.put(USERNAME, "Bob")
					.put("msg", "Message from Bob")
				);
				return null;
			}
		});
		
		assertEquals("Message from Bob", root.result.get());
		assertEquals("Message from Bob", bob.result.get());
		assertError(TimeoutException.class, alice.future()); //Alice is not super user shouldn't get Bob's message.
	}
	
	/**
	 * Super user can connect to specific user's channel and only get messages from that user.
	 */
	public void testSuperUserConnectToOtherChannel() throws Exception {
		Receiver<String> root = receiver(SUPER_USER, "bork", "Bob"); // connect "super" receiver to "Bob" channel
		Receiver<String> bob = receiver("Bob", "bork"); 
		
		run(new Process<Void>("Bob") {
			protected Void execute() throws Exception {
				send("bork", new JSONObject()
					.put(USERNAME, "Bob")
					.put("msg", "Message from Bob")
				);
				return null;
			}
		});
		
		assertEquals("Message from Bob", root.result.get());
		assertEquals("Message from Bob", bob.result.get());
	}
	
	/**
	 * Super user can connect to specific user's channel and will not get messages from other users.
	 */
	public void testSuperUserConnectToOtherChannel2() throws Exception {
		Receiver<String> root = receiver(SUPER_USER, "bork", "Alice"); // connect "super" receiver to "Alice" channel
		Receiver<String> bob = receiver("Bob", "bork"); 
		
		run(new Process<Void>("Bob") {
			protected Void execute() throws Exception {
				send("bork", new JSONObject()
					.put(USERNAME, "Bob")
					.put("msg", "Message from Bob")
				);
				return null;
			}
		});
		
		assertError(TimeoutException.class, root.future());
		assertEquals("Message from Bob", bob.result.get());
	}
	
	public void testRequestResponsePattern() throws Exception {
		
		final BasicFuture<Void> serviceStarted = new BasicFuture<Void>();
		final BasicFuture<Void> quitRequested = new BasicFuture<Void>();
		
		Process<Void> service = new Process<Void>(SUPER_USER) {
			protected Void execute() throws Exception {
				conn.addMessageHandler(new RequestResponseHandler(conn, "helloRequest") {
					@Override
					protected JSONObject fillResponse(String type, JSONObject req, JSONObject res) throws Exception {
						String cmd = req.getString("cmd"); 
						if ("quit".equals(cmd)) {
							quitRequested.resolve(null);
							//Note this response may not be delivered because the service may shut down before
							// the message is sent.
							return res.put("msg", "Quit request received from "+req.getString(USERNAME));
						} else if ("greeting".equals(cmd)) {
							return res.put("msg", "Hello "+req.getString(USERNAME));
						} else {
							throw new IllegalArgumentException("Unkown command: "+cmd);
						}
					}
				});
				serviceStarted.resolve(null);
				
				//Wait for quit request
				return quitRequested.get();
			}
			
		};
		
		Process<Void> bob = new Process<Void>("Bob") {
			protected Void execute() throws Exception {
				//Bob has to wait for the service to be ready before sending requests to it!
				serviceStarted.get();
				try {
					assertEquals("Hello Bob", sendRequest("helloRequest", 
						new JSONObject()
							.put(USERNAME, "Bob")
							.put("cmd", "greeting"),
						new ResponseHandler<String>() {
							protected String handle(String messageType, JSONObject msg) throws Exception {
								return msg.getString("msg");
							}
						}
					));
					
					assertError("bogusCommand", asendRequest("helloRequest",
						new JSONObject()
							.put(USERNAME, "Bob")
							.put("cmd", "bogusCommand"),
						new ResponseHandler<String>() {
							protected String handle(String messageType, JSONObject msg) throws Exception {
								return msg.getString("msg");
							}
						}
					));
				} finally {
					conn.send("helloRequest", new JSONObject()
						.put(USERNAME, "Bob")
						.put("cmd", "quit")
					);
				}
				return null;
			}

		};

		run(service, bob);
	}
	
	/**
	 * Test that super user can connect and disconnect channels to 
	 * switch between users.
	 * <p>
	 * This basic test only swtiches channels and verifies the 'isConnected' 
	 * state follows suite. It does not verify whether message delivery is
	 * changed according connected channels. 
	 */
	public void testBasicChannelSwitching() throws Exception {
		Process<Void> root = new Process<Void>(SUPER_USER) {
			protected Void execute() throws Exception {
				assertTrue(conn.isConnected(SUPER_USER));
				assertEquals(SUPER_USER, conn.getChannel());
				
				conn.disconnectFromChannelSync(SUPER_USER);
				conn.connectToChannelSync("Bob");
				assertFalse(conn.isConnected(SUPER_USER));
				assertTrue(conn.isConnected("Bob"));
				assertEquals("Bob", conn.getChannel());
				
				conn.disconnectFromChannelSync("Bob");
				conn.connectToChannelSync("Alice");
				assertFalse(conn.isConnected("Bob"));
				assertTrue(conn.isConnected("Alice"));
				assertEquals("Alice", conn.getChannel());
				
				return null;
			}
		};
		run(root);
	}
	
	/**
	 * Test that super user can connect and disconnect channels to 
	 * switch between users.
	 * <p>
	 * This basic test only swtiches channels and verifies the 'isConnected' 
	 * state follows suite. It does not verify whether message delivery is
	 * changed according connected channels. 
	 */
	public void testChannelSwitchingMessageReception() throws Exception {
		
		final String[] users = {"Bob", "Alice"};
		
		/**
		 * Resolves when the root process is connected to corresponding channel
		 */
		final List<BasicFuture<Void>> channelSynch = new ArrayList<BasicFuture<Void>>();
		for (final String user : users) {
			channelSynch.add(new BasicFuture<Void>());
			
			final Process<Void> userProcess = new Process<Void>(user) {
				protected Void execute() throws Exception {
					for (int i = 0; i < users.length; i++) {
						String currentChannel = users[i];
						channelSynch.get(i).get(); //wait for root to switch to this channel.
						send("bork", new JSONObject()
							.put(USERNAME, user)
							.put("msg", user+" -> "+currentChannel));
					}
					return null;
				};
			};
			userProcess.start();
		}
		
		Process<List<String>> root = new Process<List<String>>(SUPER_USER) {
			protected List<String> execute() throws Exception {
				final List<String> receivedMessages = new ArrayList<String>();
				assertTrue(conn.isConnected(SUPER_USER));
				conn.disconnectFromChannelSync(SUPER_USER);
				assertFalse(conn.isConnected(SUPER_USER));
				
				conn.addMessageHandler(new MessageHandler("bork") {
					@Override
					public void handle(String type, JSONObject message) {
						try {
							receivedMessages.add(message.getString("msg"));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				
				for (int i = 0; i < users.length; i++) {
					String currentChannel = users[i];
					if (i>0) {
						String previousChannel = users[i-1];
						System.out.println("Previous channel = "+previousChannel);
						conn.disconnectFromChannelSync(previousChannel);
					}
					conn.connectToChannelSync(currentChannel);
					
					//Check channel connected state(s)
					for (String channel : users) {
						boolean isConnected = conn.isConnected(channel);
						assertEquals("["+currentChannel+"] "+channel+" isConnected? "+isConnected, channel.equals(currentChannel), isConnected);
					}
					
					channelSynch.get(i).resolve(null);
					Thread.sleep(500); //Allow some time for user processes to send messages while root is connected to this channel.
				}
				
				return receivedMessages;
			}
		};
		
		run(root);
		
		List<String> results = root.result.get();
		String[] expectedMessages = new String[users.length];
		for (int i = 0; i < expectedMessages.length; i++) {
			expectedMessages[i] = users[i]+" -> "+users[i];
		}
		assertArrayEquals(expectedMessages, results.toArray());
	}

	/**
	 * Use asynchronous connectToChannel and disconnectFromChannel together with channel listener
	 * to avoid race condition when sending / receiving messages.
	 */
	public void testChannelListener() throws Exception {
		final String[] channels = { SUPER_USER, "Bob", "Alice" };
		
		final BasicFuture<Void> echoServiceReady = new BasicFuture<>();
		final BasicFuture<Void> theEnd = new BasicFuture<Void>(); //resolves at end of callback spagetti sequence
		
		final Process<List<String>> root = new Process<List<String>>(SUPER_USER) {
			protected java.util.List<String> execute() throws Exception {
				final List<String> receivedMessages = new ArrayList<String>();

				echoServiceReady.get();
				
				conn.addMessageHandler(new MessageHandler("echoResponse") {
					@Override
					public void handle(String type, JSONObject message) {
						try {
							receivedMessages.add(message.getString("msg"));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				
				conn.addChannelListener(new IChannelListener() {
					public void disconnected(String oldChannel) {
						String newChannel = nextChannel(oldChannel);
						if (newChannel!=null) {
							conn.connectToChannel(newChannel);
						} else {
							theEnd.resolve(null);
						}
					}

					@Override
					public void connected(final String currentChannel) {
						try {
							send("echoRequest", new JSONObject()
								.put(USERNAME, currentChannel)
								.put("msg", "Hello on channel "+currentChannel));
							//Give some time for response
							setTimeout(500, new TimerTask() {
								public void run() {
									try {
										conn.disconnectFromChannel(currentChannel);
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							});
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
					private String nextChannel(String oldChannel) {
						for (int i = 0; i < channels.length-1; i++) {
							if (channels[i].equals(oldChannel)) {
								return channels[i+1];
							}
						}
						return null;
					}
				});
				
				conn.disconnectFromChannel(channels[0]);
				theEnd.get(); // must wait until callback spagetti finishes before allowing this process to terminate.
				
				return receivedMessages;
			}
		};
		Process<Void> echoService = new Process<Void>(SUPER_USER) {
			protected Void execute() throws Exception {
				conn.addMessageHandler(new RequestResponseHandler(conn, "echoRequest") {
					//no need to override anything. The default implementation is already a 'echo' service.
				});
				echoServiceReady.resolve(null);
				await(root); //once main process finished we can finish too
				return null;
			};
		};
		
		run(echoService, root);
		
		ArrayList<String> expected = new ArrayList<String>();
		for (int i = 1; i < channels.length; i++) {
			expected.add("Hello on channel "+channels[i]);
		}
		assertArrayEquals(expected.toArray(), root.result.get().toArray());
	}
	
	//TODO: RequestResponseHandler uses callback id properly.
	
//	@Override
//	public void handle(String type, JSONObject message) {
//		String command = message.getString("command");
//		if ("quit".equals("command")) {
//			quit.resolve(null);
//		}
//		message()
//	}
	
	//TODO: testing direct request response pattern.
	
	//TODO: normal user can only send messages to their own channel.
	
	//TODO: tests for SingleResponseHandler. Check that it properly cleans up (uregisteres message handler
	//   when response is received, when timed out etc.
	
	//TODO: CallbackIDAwareMessageHandler
	
	//TODO: getState method: add to MessageConnector, test, and reactivate commented code in flux ui Activator that uses it.
	
	/**
	 * Create a simple single response receiver that does these steps:
	 *   - open message connector for some 'user'
	 *   - connect to a 'channel' (may be different from the user)
	 *   - wait for a message of 'messageType'
	 *   - return and retrieve the property 'msg' from the receive message object
	 *   - disconnect from the flux message bus.
	 */
	private Receiver<String> receiver(String user, String messageType, String channel) throws Exception {
		return new Receiver<String>(user, messageType, channel) {
			protected String receive(String type, JSONObject message) throws Throwable {
				return message.getString("msg");
			}
		};
	}

	/**
	 * Convenience method to create single response receiver where channelName = userName
	 */
	private Receiver<String> receiver(String user, String messageType) throws Exception {
		return receiver(user, messageType, user);
	}

	protected MessageConnector createConnection(String user) throws Exception {
		return new RabbitMQFluxConfig(user).connect(client);
	}
	
	
	
}

