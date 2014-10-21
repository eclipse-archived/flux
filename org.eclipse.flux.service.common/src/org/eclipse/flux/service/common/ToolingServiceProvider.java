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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.flux.client.IChannelListener;
import org.eclipse.flux.client.IMessageHandler;
import org.eclipse.flux.client.MessageConnector;
import org.json.JSONException;
import org.json.JSONObject;

public class ToolingServiceProvider {

	private static final long EXPECTED_RESPONSE_TIMEOUT = 500;
	private static final long POOL_MAINTENANCE_PERIOD = 2 * 60 * 1000;
	private static final long POOL_MAINTENANCE_NOW_DELAY = 50;
	private static final long POOL_INITIALIZATION_WAIT_TIME_STEP = 1000;
	private static final long POOL_INITIALIZATION_TIMEOUT = 2 * 60 * 1000;

	private static final String DISCOVER_SERVICE_REQUEST = "discoverServiceRequest";
	private static final String DISCOVER_SERVICE_RESPONSE = "discoverServiceResponse";
	private static final String SERVICE_STATUS_CHANGE = "serviceStatusChange";
	private static final String SERVICE_REQUIRED_REQUEST = "serviceRequiredRequest";
	private static final String SERVICE_REQUIRED_RESPONSE = "serviceRequiredResponse";
	
	private static final String[] JSON_PROPERTIES = new String[] { "username",
			"service", "requestSenderID" };

	/**
	 * Service Manager is active
	 */
	private AtomicBoolean active = new AtomicBoolean(false);
		
	/**
	 * Web socket connector
	 */
	private MessageConnector messageConnector;
		
	private ScheduledExecutorService poolMaintenanceExecutor;
	
	private ExecutorService serviceLauncherExecutor;
	
	private String serviceId;
	
	/**
	 * Starts and stops tooling services
	 */
	private IServiceLauncher serviceLauncher = null;
	
	private int poolSize;
	
	private IMessageHandler[] messageHandlers;
	
	private ScheduledFuture<?> poolMaintenanceFuture;
	
	private Exception launchException;
	
	final private boolean autoMaintainServicePoolSize;
	
	private Runnable poolMaintenanceOperation = new PoolMaintenanceOperation();

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
				dispose();
			}
		}
		
	};
	
	/**
	 * Constructs Tooling Services Manager
	 * 
	 * @param host Flux server URL
	 * @param serviceLauncher The tooling service starter/stopper 
	 */
	public ToolingServiceProvider(final MessageConnector messageConnector,
			final String serviceId, final IServiceLauncher serviceLauncher,
			int poolSize, boolean autoMaintainServicePoolSize) {
		super();
		this.messageConnector = messageConnector;
		this.serviceId = serviceId;
		this.poolSize = poolSize;
		this.autoMaintainServicePoolSize = autoMaintainServicePoolSize;
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
					} catch (Exception e) {
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
					try {
						JSONObject statusMessage = new JSONObject(message,
								JSON_PROPERTIES);
						statusMessage.put("status", "unavailable");
						String error = getError();
						if (error == null) {
							statusMessage.put("info", "Starting up services, please wait...");
						} else {
							statusMessage.put("error", getError());
						}
						messageConnector.send(DISCOVER_SERVICE_RESPONSE,
								statusMessage);
					} catch (Exception e) {
						e.printStackTrace();
					}
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
						return message.getString("service").equals(serviceId)
								&& "ready".equals(message.getString("status"))
								&& !Utils.SUPER_USER.equals(message.get("username"));
					} catch (JSONException e) {
						e.printStackTrace();
						return false;
					}
				}

				@Override
				public void handle(String type, JSONObject message) {
					schedulePoolMaintenance();
				}

				@Override
				public String getMessageType() {
					return SERVICE_STATUS_CHANGE;
				}
				
			}
		};
	}
	
	private synchronized String getError() {
		if (launchException != null) {
			return launchException.getMessage();
		}
		return null;
	}

	private void init() {
		/*
		 * Get off the socket IO message listening thread
		 */
		new Thread() {

			@Override
			public void run() {
				if (serviceLauncher != null) {
					serviceLauncher.init();
				}
				
				serviceLauncherExecutor = Executors.newFixedThreadPool(5);

				poolMaintenanceExecutor = Executors.newScheduledThreadPool(1);
				new PoolInitilizationOperation().run();
				
				for (IMessageHandler messageHandler : messageHandlers) {
					messageConnector.addMessageHandler(messageHandler);
				}
			}
			
		}.start();
	}
	
	private void validateState() {
		if (active.get()) {
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
		if (!active.get()) {
			return;
		}
		
		dispose();
		messageConnector.removeChannelListener(CONNECTION_LISTENER);
		
		active.set(false);		
	}
	
	private void dispose() {
		for (IMessageHandler messageHandler : messageHandlers) {
			messageConnector.removeMessageHandler(messageHandler);
		}
		
		if (poolMaintenanceFuture != null) {
			poolMaintenanceFuture.cancel(false);
		}
		
		poolMaintenanceExecutor.shutdown();
		
		serviceLauncherExecutor.shutdown();
		
		if (serviceLauncher != null) {
			serviceLauncher.dispose();
		}
	}
	
	final public void start() {
		if (active.get()) {
			return;
		}
		active.set(true);
		messageConnector.addChannelListener(CONNECTION_LISTENER);
		if (messageConnector.isConnected(Utils.SUPER_USER)) {
			CONNECTION_LISTENER.connected(Utils.SUPER_USER);
		} else {
			messageConnector.connectToChannel(Utils.SUPER_USER);
		}
	}
	
	private void startService(final int n) {
		serviceLauncherExecutor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					serviceLauncher.startService(n);
					setLaunchException(null);
				} catch (Exception e) {
					setLaunchException(e);
				}
			}
			
		});
	}
	
	private synchronized void setLaunchException(Exception e) {
		launchException = e;
	}
	
	private void schedulePoolMaintenance() {
		if (poolMaintenanceFuture != null) {
			poolMaintenanceFuture.cancel(false);
		}
		if (autoMaintainServicePoolSize) {
			poolMaintenanceFuture = poolMaintenanceExecutor
					.scheduleWithFixedDelay(
							poolMaintenanceOperation,
							POOL_MAINTENANCE_NOW_DELAY,
							POOL_MAINTENANCE_PERIOD,
							TimeUnit.MILLISECONDS);
		} else {
			poolMaintenanceFuture = poolMaintenanceExecutor
					.schedule(
							poolMaintenanceOperation,
							POOL_MAINTENANCE_NOW_DELAY,
							TimeUnit.MILLISECONDS);
		}
	}
	
	private class PoolMaintenanceOperation implements Runnable {

		protected synchronized int getNumberOfServicesRunning() {

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
				messageConnector
						.send(DISCOVER_SERVICE_REQUEST, discoverMessage);
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				Thread.sleep(EXPECTED_RESPONSE_TIMEOUT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			messageConnector.removeMessageHandler(messageHandler);

			return counter.get();
		}

		@Override
		public synchronized void run() {
			int numberOfServicesToStart = poolSize - getNumberOfServicesRunning();
			if (numberOfServicesToStart > 0) {
				startService(numberOfServicesToStart);
			}
		}
	}
	
	private class PoolInitilizationOperation extends PoolMaintenanceOperation {

		@Override
		public synchronized void run() {
			int numberOfServicesToStart = poolSize - getNumberOfServicesRunning();
			if (numberOfServicesToStart > 0) {
				System.out.println("Need to start " + numberOfServicesToStart + " services");
				final AtomicInteger counter = new AtomicInteger(numberOfServicesToStart);
				IMessageHandler readyServiceHanlder = new IMessageHandler() {
					
					@Override
					public boolean canHandle(String type, JSONObject message) {
						try {
							return message.getString("service").equals(serviceId)
									&& "ready".equals(message.getString("status"))
									&& Utils.SUPER_USER.equals(message.get("username"));
						} catch (JSONException e) {
							e.printStackTrace();
							return false;
						}
					}

					@Override
					public void handle(String type, JSONObject message) {
						if (counter.decrementAndGet() <= 0) {
							messageConnector.removeMessageHandler(this);
						}
					}

					@Override
					public String getMessageType() {
						return SERVICE_STATUS_CHANGE;
					}
				};
				messageConnector.addMessageHandler(readyServiceHanlder);
				startService(numberOfServicesToStart);
				System.out.println("Populating service pool.");
				for (long time = 0; counter.get() > 0 && time < POOL_INITIALIZATION_TIMEOUT; time += POOL_INITIALIZATION_WAIT_TIME_STEP) {
					try {
						Thread.sleep(POOL_INITIALIZATION_WAIT_TIME_STEP);
						System.out.print(".");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				messageConnector.removeMessageHandler(readyServiceHanlder);
				System.out.println();
				int n = counter.get();
				if (n > 0) {
					System.out.println("WARNING: Service pool hasn't been completely initialized. " + n + " service(s) are missing.");
				} else {
					System.out.println("Service pool has been successfully populated");
				}
			} else {
				System.out.println("Service pool is already filled up");
			}
		}
		
	}

}
