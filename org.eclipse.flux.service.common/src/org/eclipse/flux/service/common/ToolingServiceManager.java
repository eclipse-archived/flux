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
import java.util.concurrent.TimeUnit;
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
	
	/**
	 * Service Manager is active
	 */
	private boolean active = false;
		
	/**
	 * Flux messaging server URL
	 */
	private String host;
	
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
	 * Shutdown timeout for the thread pool
	 */
	private long shutdownTimeout = 60 * 1000; // 60 seconds	
	
	/**
	 * Regular expression for acceptable resource types which would trigger tooling service start 
	 */
	private String fileFiltersRegEx = null;
	
	/**
	 * Cleanup thread
	 */
	private Thread cleanupThread = null;
	
	/**
	 * Constructs Tooling Services Manager
	 * 
	 * @param host Flux server URL
	 * @param serviceLauncher The tooling service starter/stopper 
	 */
	public ToolingServiceManager(String host, IServiceLauncher serviceLauncher) {
		super();
		host(host);
		serviceLauncher(serviceLauncher);
	}
	
	private void init() {
		executor = Executors.newFixedThreadPool(maxThreadNumber);
		messageConnector = new MessageConnector(host);

		messageConnector.addMessageHandler(new IMessageHandler() {

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

		});

		messageConnector.addMessageHandler(new IMessageHandler() {

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
		});

	}

	private void doRun() {
		while(!messageConnector.isConnected()) {
			System.out.println("Attempting to connect to messaging server: " + host);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		while (true) {
			try {

				// Do the cleanup of unused services
				cleanupServices();

				// Sleep until the next cleanup time.
				Thread.sleep(cleanupTimePeriod);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void validateState() {
		if (active) {
			throw new IllegalArgumentException("Cannot set parameters when service manager is running!");
		}
	}
	
	/**
	 * Sets the Flux messaging server URL
	 * 
	 * @param host Flux messaging server URL
	 * @return
	 */
	public ToolingServiceManager host(String host) {
		validateState();
		if (host == null) {
			throw new IllegalArgumentException("Parameter must not be NULL!");
		}
		this.host = host;
		return this;
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
	
	public ToolingServiceManager shutdownTimeoutTime(long shutdownTimeout) {
		validateState();
		if (shutdownTimeout < 0) {
			throw new IllegalArgumentException("Parameter must be positive or 0!");
		}
		this.shutdownTimeout = shutdownTimeout;
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
		// Stop the cleanup thread. Service will be shutdown below anyway.
		cleanupThread.interrupt();
		
		messageConnector.dispose();
		
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
			newUsersWithActiveService.clear();
		} finally {
			lock.writeLock().unlock();
		}
		
		// Wait until all JDT services shutdown
		try {
			executor.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		active = false;
		
	}
	
	final public void start() {
		active = true;
		init();
		doRun();
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
			e.printStackTrace();
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
	
	private void launchService(final String user, final boolean handleFailre) {
		executor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					if (!serviceLauncher.startService(user) && handleFailre) {
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
