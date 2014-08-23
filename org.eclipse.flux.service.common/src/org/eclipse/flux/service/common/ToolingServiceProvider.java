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
package org.eclipse.flux.service.common;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;

public class ToolingServiceProvider {

	private static final long WAIT_TIME_BEFORE_SENDING_AVAILABLE = 500;
	private static final long EXPECTED_RESPONSE_TIMEOUT = 500;
	private static final long POOL_MAINTENANCE_PERIOD = 60 * 1000;
	private static final long POOL_MAINTENANCE_NOW_DELAY = 50;

	private static final String DISCOVER_SERVICE_REQUEST = "discoverServiceRequest";
	private static final String DISCOVER_SERVICE_RESPONSE = "discoverServiceResponse";
	private static final String SERVICE_STATUS_CHANGE = "serviceStatusChange";
	private static final String START_SERVICE_REQUEST = "startServiceRequest";
	private static final String START_SERVICE_RESPONSE = "startServiceResponse";
	private static final String SERVICE_REQUIRED_REQUEST = "serviceRequiredRequest";
	private static final String SERVICE_REQUIRED_RESPONSE = "serviceRequiredResponse";
	
	private static final String[] JSON_PROPERTIES = new String[] { "username",
			"service", "requestSenderID" };

	/**
	 * Service Manager is active
	 */
	private boolean active = false;
		
	/**
	 * Web socket connector
	 */
	private MessageConnector messageConnector;
		
	private ScheduledExecutorService scheduledExecutor;
	
	private ScheduledExecutorService poolMaintenanceExecutor;
	
	private String serviceId;
	
	/**
	 * Starts and stops tooling services
	 */
	private IServiceLauncher serviceLauncher = null;
	
	private int poolSize;
	
	private IMessageHandler[] messageHandlers;
	
	private ScheduledFuture<?> poolMaintenanceFuture;
	
	private ConcurrentLinkedDeque<String> pendingStartServiceRequests;
	
	private Runnable poolMaintenanceOperation = new Runnable() {
		@Override
		public void run() {
			
			final AtomicInteger counter = new AtomicInteger(0);
			
			IMessageHandler messageHandler = new IMessageHandler() {
				
				@Override
				public void handle(String type, JSONObject message) {
					counter.incrementAndGet();
				}
				
				@Override
				public String getMessageType() {
					return DISCOVER_SERVICE_RESPONSE;
				}
				
				@Override
				public boolean canHandle(String type, JSONObject message) {
					try {
						return message.getString("service").equals(serviceId)
								&& "ready".equals(message.getString("status"));
					} catch (JSONException e) {
						e.printStackTrace();
						return false;
					}
				}
			};
			
			messageConnector.addMessageHandler(messageHandler);
			
			try {
				JSONObject discoverMessage = new JSONObject();
				discoverMessage.put("service", serviceId);
				discoverMessage.put("username", Utils.SUPER_USER);
				messageConnector.send(DISCOVER_SERVICE_REQUEST, discoverMessage);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(EXPECTED_RESPONSE_TIMEOUT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			messageConnector.removeMessageHandler(messageHandler);
			
			int numberOfServicesToStart = poolSize - counter.get();
			if (numberOfServicesToStart > 0) {
				serviceLauncher.startService(numberOfServicesToStart);
			}
			
		}
	};

	private final IChannelListener CONNECTION_LISTENER = new IChannelListener() {

		@Override
		public void connected(String userChannel) {
			if (Utils.SUPER_USER.equals(userChannel)) {
				init();
			}
		}

		@Override
		public void disconnected(String userChannel) {
			if (Utils.SUPER_USER.equals(userChannel)) {
				stop();
			}
		}
		
	};
	
	/**
	 * Constructs Tooling Services Manager
	 * 
	 * @param host Flux server URL
	 * @param serviceLauncher The tooling service starter/stopper 
	 */
	public ToolingServiceProvider(final MessageConnector messageConnector, final String serviceId, final IServiceLauncher serviceLauncher, int poolSize) {
		super();
		this.messageConnector = messageConnector;
		this.serviceId = serviceId;
		this.poolSize = poolSize;
		serviceLauncher(serviceLauncher);

		messageHandlers = new IMessageHandler[] {
				
			new IMessageHandler() {
	
				@Override
				public boolean canHandle(String type, JSONObject message) {
					try {
						return message.getString("username").equals(Utils.SUPER_USER) &&
								message.getString("service").equals(serviceId);
					} catch (JSONException e) {
						e.printStackTrace();
						return false;
					}
				}
	
				@Override
				public void handle(String type, JSONObject message) {
					try {
						messageConnector.send(SERVICE_REQUIRED_RESPONSE,
								new JSONObject(message, JSON_PROPERTIES));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
	
				@Override
				public String getMessageType() {
					return SERVICE_REQUIRED_REQUEST;
				}
	
			},
	
			new IMessageHandler() {
	
				@Override
				public void handle(String type, JSONObject message) {
					try {
						messageConnector.send(START_SERVICE_RESPONSE,
								new JSONObject(message, JSON_PROPERTIES));
						
						JSONObject statusMessage = new JSONObject(message, JSON_PROPERTIES);
						statusMessage.put("status", "starting");
						messageConnector.send(SERVICE_STATUS_CHANGE, statusMessage);
						
						pendingStartServiceRequests.add(message.getString("username"));
						serviceLauncher.startService(1);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
	
				@Override
				public String getMessageType() {
					return START_SERVICE_REQUEST;
				}
	
				@Override
				public boolean canHandle(String type, JSONObject message) {
					try {
						return message.getString("service").equals(serviceId);
					} catch (JSONException e) {
						e.printStackTrace();
						return false;
					}
				}
			},
			
			new IMessageHandler() {

				@Override
				public boolean canHandle(String type, JSONObject message) {
					try {
						return message.getString("service").equals(serviceId);
					} catch (JSONException e) {
						e.printStackTrace();
						return false;
					}
				}

				@Override
				public void handle(String type, final JSONObject message) {
					scheduledExecutor.schedule(new Runnable() {
						@Override
						public void run() {
							try {
								JSONObject statusMessage = new JSONObject(message, JSON_PROPERTIES);
							statusMessage.put(
									"status",
									pendingStartServiceRequests
											.contains(message
													.getString("username")) ? "starting"
											: "available");
								messageConnector.send(DISCOVER_SERVICE_RESPONSE, statusMessage);
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}, WAIT_TIME_BEFORE_SENDING_AVAILABLE, TimeUnit.MILLISECONDS);
				}

				@Override
				public String getMessageType() {
					return DISCOVER_SERVICE_REQUEST;
				}
				
			},
			
			new IMessageHandler() {

				@Override
				public boolean canHandle(String type, JSONObject message) {
					try {
						return message.getString("service").equals(serviceId);
					} catch (JSONException e) {
						e.printStackTrace();
						return false;
					}
				}

				@Override
				public void handle(String type, JSONObject message) {
					try {
						if (Utils.SUPER_USER.equals(message.getString("username"))) {
							if ("ready".equals(message.getString("status"))
									&& pendingStartServiceRequests.peek() != null) {
								startServiceForUser(pendingStartServiceRequests.poll(), message.getString("senderID"));
							}
						} else {
							if ("starting".equals(message.getString("status"))) {
								poolMaintenanceFuture.cancel(false);
								poolMaintenanceFuture = poolMaintenanceExecutor
										.scheduleWithFixedDelay(poolMaintenanceOperation,
												POOL_MAINTENANCE_NOW_DELAY,
												POOL_MAINTENANCE_PERIOD, TimeUnit.MILLISECONDS);
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				@Override
				public String getMessageType() {
					return SERVICE_STATUS_CHANGE;
				}
				
			}
		};
	}

	private void init() {
		if (serviceLauncher != null) {
			serviceLauncher.init();
		}

		pendingStartServiceRequests = new ConcurrentLinkedDeque<String>();

		scheduledExecutor = Executors.newScheduledThreadPool(5);

		poolMaintenanceExecutor = Executors.newScheduledThreadPool(1);
		poolMaintenanceFuture = poolMaintenanceExecutor.scheduleWithFixedDelay(
				poolMaintenanceOperation, POOL_MAINTENANCE_NOW_DELAY, POOL_MAINTENANCE_PERIOD,
				TimeUnit.MILLISECONDS);

		for (IMessageHandler messageHandler : messageHandlers) {
			messageConnector.addMessageHandler(messageHandler);
		}
	}

	private void validateState() {
		if (active) {
			throw new IllegalArgumentException("Cannot set parameters when service manager is running!");
		}
	}
	
	public ToolingServiceProvider serviceLauncher(IServiceLauncher serviceLauncher) {
		validateState();
		if (serviceLauncher == null) {
			throw new IllegalArgumentException("Parameter must not be NULL!");
		}
		this.serviceLauncher = serviceLauncher;
		return this;
	}
		
	final public void stop() {
		if (!active) {
			return;
		}
		
		messageConnector.removeChannelListener(CONNECTION_LISTENER);
		
		for (IMessageHandler messageHandler : messageHandlers) {
			messageConnector.removeMessageHandler(messageHandler);
		}
		
		scheduledExecutor.shutdown();
		
		poolMaintenanceFuture.cancel(false);
		poolMaintenanceExecutor.shutdown();
		
		if (serviceLauncher != null) {
			serviceLauncher.dispose();
		}
		
		pendingStartServiceRequests = null;
		
		active = false;		
	}
	
	final public void start() {
		if (active) {
			return;
		}
		active = true;
		messageConnector.addChannelListener(CONNECTION_LISTENER);
		if (messageConnector.isConnected(Utils.SUPER_USER)) {
			CONNECTION_LISTENER.connected(Utils.SUPER_USER);
		} else {
			messageConnector.connectToChannel(Utils.SUPER_USER);
		}
	}
	
	private void startServiceForUser(final String user, final String socketId) throws JSONException {
		final AtomicBoolean started = new AtomicBoolean(false);
		
		final IMessageHandler serviceStartedResponseHandler = new IMessageHandler() {
			
			@Override
			public void handle(String type, JSONObject message) {
				started.set(true);
			}
			
			@Override
			public String getMessageType() {
				return START_SERVICE_RESPONSE;
			}
			
			@Override
			public boolean canHandle(String type, JSONObject message) {
				try {
					return socketId.equals(message.getString("responseSenderID"));
				} catch (JSONException e) {
					e.printStackTrace();
					return false;
				}
			}
		};
		messageConnector.addMessageHandler(serviceStartedResponseHandler);
		
		JSONObject startServiceMessage = new JSONObject();
		startServiceMessage.put("username", user);
		startServiceMessage.put("service", serviceId);
		startServiceMessage.put("socketID", socketId);
		messageConnector.send(START_SERVICE_REQUEST, startServiceMessage);
		
		scheduledExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				messageConnector.removeMessageHandler(serviceStartedResponseHandler);
				if (!started.get()) {
					pendingStartServiceRequests.addFirst(user);
					serviceLauncher.startService(1);
				}
			}
		}, EXPECTED_RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS);
		
	}
	
}
