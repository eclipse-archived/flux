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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Determines when to start and stop Tooling IDE services per Flux user. Tooling
 * IDE service is a service typically available when source code is being edited
 * in an editor by a user. Tooling IDE service provides problem markers, content
 * assist, navigation, refactoring etc. functionality for users.
 * 
 * A tooling service would be started for a user if
 * <code>liveResourceStarted</code> message is received and the data in the
 * message indicates that appropriate resource is being edited. A tooling
 * service would be stopped for a user if <code>getLiveResourcesResponse</code>
 * message is received with the data not containing appropriate resources.
 * 
 * @author aboyko
 *
 */
final public class ToolingServiceManager {
	
	private static final long SERVICE_POOL_INITIALIZATION_TIMEOUT = 5 * 60 * 1000; // 5 minute
	private static final long SERVICE_INITIALIZATION_TIME_INCREMENT = 1000; // 1 second
	
	/**
	 * Service Manager is active
	 */
	private boolean active = false;
		
	/**
	 * Web socket connector
	 */
	private MessageConnector messageConnector;
	
	/**
	 * Users for which Tooling services need to be active during the cleanup phase
	 */
	private Map<String, Boolean> newUsersWithActiveService = new HashMap<String, Boolean>();
	
	/**
	 * Users with currently active tooling service
	 */
	private Map<String, Boolean> currentUsersWithActiveService = new HashMap<String, Boolean>();
	
	/**
	 * Lock for accessing/modifying references to caches of users with active tooling service
	 */
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	/**
	 * Executor service for starting and stopping tooling services
	 */
	private ExecutorService executor;
	
	/**
	 * Starts and stops tooling services
	 */
	private IServiceLauncher serviceLauncher = null;
	
	/**
	 * ID for the tooling service cleanup thread
	 */
	private String cleanupThreadId = "Service-Cleanup";
	
	/**
	 * Callback ID for Flux messages
	 */
	private int cleanupCallbackId;
	
	/**
	 * Time period between two consecutive tooling service cleanups
	 */
	private long cleanupTimePeriod = 60 * 1000; // 1 minute in milliseconds
	
	/**
	 * Time reserved for tooling services cleanup
	 */
	private long cleanupTime = 1000; // 1 second for the cleanup time
	
	/**
	 * Maximum number of tooling service start/stop threads
	 */
	private int maxThreadNumber = 10;
	
	/**
	 * Regular expression for acceptable resource types which would trigger tooling service start 
	 */
	private String fileFiltersRegEx = null;
	
	/**
	 * Cleanup thread
	 */
	private Thread cleanupThread = null;
	
	private IMessageHandler[] messageHandlers;

	private final IChannelListener CONNECTION_LISTENER = new IChannelListener() {

		@Override
		public void connected(String userChannel) {
			if (Utils.SUPER_USER.equals(userChannel)) {
				init();
				cleanupThread.start();
			}
		}

		@Override
		public void disconnected(String userChannel) {
			if (Utils.SUPER_USER.equals(userChannel)) {
				stop();
			}
		}
		
	};;
	
	/**
	 * Constructs Tooling Services Manager
	 * 
	 * @param host Flux server URL
	 * @param serviceLauncher The tooling service starter/stopper 
	 */
	public ToolingServiceManager(MessageConnector messageConnector, IServiceLauncher serviceLauncher) {
		super();
		this.messageConnector = messageConnector;
		serviceLauncher(serviceLauncher);

		messageHandlers = new IMessageHandler[] {
				
			new IMessageHandler() {
	
				@Override
				public boolean canHandle(String type, JSONObject message) {
					try {
						String resource = message.getString("resource");
						return fileFiltersRegEx == null
								|| Pattern.matches(fileFiltersRegEx, resource);
					} catch (JSONException e) {
						e.printStackTrace();
						return false;
					}
				}
	
				@Override
				public void handle(String type, JSONObject message) {
					try {
						processUser(message.getString("username"));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
	
				@Override
				public String getMessageType() {
					return "liveResourceStarted";
				}
	
			},
	
			new IMessageHandler() {
	
				@Override
				public void handle(String type, JSONObject message) {
					try {
						JSONObject liveUnits = message
								.getJSONObject("liveEditUnits");
						String user = message.getString("username");
						String[] projects = JSONObject.getNames(liveUnits);
						if (projects != null && projects.length > 0) {
							processUser(user);
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
	
				@Override
				public String getMessageType() {
					return "getLiveResourcesResponse";
				}
	
				@Override
				public boolean canHandle(String type, JSONObject message) {
					return true;
				}
			}
		};
	}

	private void init() {
		if (serviceLauncher != null) {
			serviceLauncher.init();
		}
		lock.writeLock().lock();
		try {
			currentUsersWithActiveService.clear();
		} finally {
			lock.writeLock().unlock();
		}
		executor = Executors.newFixedThreadPool(maxThreadNumber);
		for (IMessageHandler messageHandler : messageHandlers) {
			messageConnector.addMessageHandler(messageHandler);
		}
		cleanupThread = new Thread(cleanupThreadId) {
			@Override
			public void run() {
				doRun();
			}
		};
	}

	private void doRun() {
		boolean interruped = false;
		long timer = 0;
		System.out.print("\nInitializing service launcher");
		while (timer < SERVICE_POOL_INITIALIZATION_TIMEOUT && !serviceLauncher.isInitializationFinished()) {
			timer += SERVICE_INITIALIZATION_TIME_INCREMENT;
			try {
				Thread.sleep(SERVICE_INITIALIZATION_TIME_INCREMENT);
			} catch (InterruptedException e) {
				// ignore
			}
			System.out.print(".");
		}
		if (serviceLauncher.isInitializationFinished()) {
			System.out.println("\nService launcher initialization has been successful");
		} else {
			System.out.println("\nWARNING: could not itinitialize service launcher comnpletely");
		}
		while (!interruped) {
			try {

				// Do the cleanup of unused services
				cleanupServices();

				// Sleep until the next cleanup time.
				Thread.sleep(cleanupTimePeriod);

			} catch (InterruptedException e) {
				interruped = true;
			}
		}
	}
	
	private void validateState() {
		if (active) {
			throw new IllegalArgumentException("Cannot set parameters when service manager is running!");
		}
	}
	
	public ToolingServiceManager serviceLauncher(IServiceLauncher serviceLauncher) {
		validateState();
		if (serviceLauncher == null) {
			throw new IllegalArgumentException("Parameter must not be NULL!");
		}
		this.serviceLauncher = serviceLauncher;
		return this;
	}
		
	public ToolingServiceManager cleanupTimePeriod(long cleanupTimePeriod) {
		validateState();
		if (cleanupTimePeriod <= 0) {
			throw new IllegalArgumentException("Parameter must be positive!");
		}
		this.cleanupTimePeriod = cleanupTimePeriod;
		return this;
	}
	
	public ToolingServiceManager cleanupTime(long cleanupTime) {
		validateState();
		if (cleanupTime <= 0) {
			throw new IllegalArgumentException("Parameter must be positive!");
		}
		this.cleanupTime = cleanupTime;
		return this;
	}
	
	public ToolingServiceManager maxNumberOfLauncherThreads(int maxThreadNumber) {
		validateState();
		if (maxThreadNumber <= 0) {
			throw new IllegalArgumentException("Parameter must be positive!");
		}
		this.maxThreadNumber = maxThreadNumber;
		return this;
	}
	
	public ToolingServiceManager cleanupThreadId(String cleanupThreadId) {
		validateState();
		this.cleanupThreadId = cleanupThreadId;
		return this;
	}
	
	public ToolingServiceManager fileFilters(String fileFiltersRegEx) {
		validateState();
		this.fileFiltersRegEx = fileFiltersRegEx;
		return this;
	}
		
	public ToolingServiceManager cleanupCallbackId(int cleanupCallbackId) {
		validateState();
		this.cleanupCallbackId = cleanupCallbackId;
		return this;
	}
	
	final public void stop() {
		if (!active) {
			return;
		}
		
		messageConnector.removeChannelListener(CONNECTION_LISTENER);
		
		if (cleanupThread != null) {
			cleanupThread.interrupt();
		}
		
		for (IMessageHandler messageHandler : messageHandlers) {
			messageConnector.removeMessageHandler(messageHandler);
		}
		
		// Schedule shutdown of JDT services
		lock.writeLock().lock();
		try {
			for (String user : currentUsersWithActiveService.keySet()) {
				shutdownService(user, false);
			}
			if (newUsersWithActiveService != null) {
				for (String user : newUsersWithActiveService.keySet()) {
					shutdownService(user, false);
				}
			}
			currentUsersWithActiveService.clear();
		} finally {
			lock.writeLock().unlock();
		}
		
		executor.shutdown();
		
		if (serviceLauncher != null) {
			serviceLauncher.dispose();
		}
		
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
	
	private void processUser(String user) {
		lock.readLock().lock();
		try {
			if (newUsersWithActiveService == null) {
				// No cleanup.
				if (!currentUsersWithActiveService.containsKey(user)) {
					currentUsersWithActiveService.put(user, true);
					launchService(user, true);
				}
			} else {
				// During the cleanup
				if (currentUsersWithActiveService.containsKey(user)) {
					currentUsersWithActiveService.remove(user);
					newUsersWithActiveService.put(user, true);
				} else if (!newUsersWithActiveService.containsKey(user)) {
					newUsersWithActiveService.put(user, true);
					launchService(user, true);
				}
			}
		} finally {
			lock.readLock().unlock();
		}
	}
	
	private void cleanupServices() {
		lock.writeLock().lock();
		newUsersWithActiveService = new HashMap<String, Boolean>();
		lock.writeLock().unlock();

		try {
			// Query for all live resources
			JSONObject message = new JSONObject();
			message.put("callback_id", cleanupCallbackId);
			message.put("resourceRegEx", fileFiltersRegEx);
			message.put("username", "*");
			messageConnector.send("getLiveResourcesRequest", message);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// Sleep for a bit to accumulate the reply messages
		try {
			Thread.sleep(cleanupTime);
		} catch (InterruptedException e) {
			// nothing
		}

		lock.writeLock().lock();
		// Stop not active JDT services
		for (final String user : currentUsersWithActiveService.keySet()) {
			shutdownService(user, true);
		}
		// Reset services that are currently running
		currentUsersWithActiveService = newUsersWithActiveService;
		newUsersWithActiveService = null;
		lock.writeLock().unlock();
	}
	
	private void launchService(final String user, final boolean handleFailure) {
		executor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					if (!serviceLauncher.startService(user) && handleFailure) {
						// error stopping the service
						lock.writeLock().lock();
						try {
							currentUsersWithActiveService.remove(user);
							if (newUsersWithActiveService != null) {
								newUsersWithActiveService.remove(user);
							}
						} finally {
							lock.writeLock().unlock();
						}
					}
				} catch (Throwable e) {
					e.printStackTrace();
					lock.writeLock().lock();
					try {
						currentUsersWithActiveService.remove(user);
						if (newUsersWithActiveService != null) {
							newUsersWithActiveService.remove(user);
						}
					} finally {
						lock.writeLock().unlock();
					}
				}
			}
			
		});
	}
	
	private void shutdownService(final String user, final boolean handleFailure) {
		executor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					if (!serviceLauncher.stopService(user) && handleFailure) {
						// error stopping the service
						lock.writeLock().lock();
						try {
							if (newUsersWithActiveService == null) {
								currentUsersWithActiveService.put(user, true);
							} else {
								newUsersWithActiveService.put(user, true);
							}
						} finally {
							lock.writeLock().unlock();
						}
					}
				} catch (Throwable e) {
					e.printStackTrace();
					lock.writeLock().lock();
					try {
						if (newUsersWithActiveService == null) {
							currentUsersWithActiveService.put(user, true);
						} else {
							newUsersWithActiveService.put(user, true);
						}
					} finally {
						lock.writeLock().unlock();
					}
				}
			}
			
		});
	}

}
