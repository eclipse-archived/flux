package org.eclipse.flux.service.common;

import java.net.URL;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class MessageServiceLauncher implements IServiceLauncher {
	
	private static final long MIN_TIMEOUT = 500L;
	private static final long TIME_STEP = 50L;
	
	private static final int MAX_NUMBER_OF_TRIALS = 3;
	
	private String serviceID;
		
	private long timeout;
	
	private int maxPoolSize = 1;
	
	private MessageConnector messageConnector;
	
	private ConcurrentHashMap<String, String> userToServiceCache = new ConcurrentHashMap<String, String>();
	
	private Deque<String> servicePoolQueue = new LinkedList<String>();
	
	private final Object servicePoolLock = new Object();
	
	private final Object poolSizeLock = new Object();
	
	public MessageServiceLauncher(URL host, final String serviceID, long timeout) {
		this.serviceID = serviceID;
		
		if (timeout < MIN_TIMEOUT) {
			throw new IllegalArgumentException("Timeout value cannot be smaller than " + MIN_TIMEOUT + " miliseconds");
		} else {
			this.timeout = timeout;
		}
		
		this.messageConnector = MessageConnector.getServiceMessageConnector(host);
		
		messageConnector.addMessageHandler(new IMessageHandler() {
			
			@Override
			public void handle(String type, JSONObject message) {
				try {
					userToServiceCache.put(message.getString("username"), message.getString("socketID"));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public String getMessageType() {
				return "startServiceResponse";
			}
			
			@Override
			public boolean canHandle(String type, JSONObject message) {
				try {
					return message.has("service") && message.getString("service").equals(serviceID);
				} catch (JSONException e) {
					e.printStackTrace();
					return false;
				}
			}
		});
		
		messageConnector.addMessageHandler(new IMessageHandler() {
			
			@Override
			public void handle(String type, JSONObject message) {
				try {
					synchronized(servicePoolQueue) {
						servicePoolQueue.add(message.getString("socketID"));
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public String getMessageType() {
				return "serviceReady";
			}
			
			@Override
			public boolean canHandle(String type, JSONObject message) {
				try {
					return message.has("service") && message.getString("service").equals(serviceID);
				} catch (JSONException e) {
					e.printStackTrace();
					return false;
				}
			}
			
		});
		
		messageConnector.addConnectionListener(new IConnectionListener() {

			@Override
			public void connected() {
				synchronized (poolSizeLock) {
					synchronized (servicePoolLock) {
						if (servicePoolQueue.isEmpty()) {
							for (int i = 0; i < maxPoolSize; i++) {
								addServiceToPool();
							}
						}
					}
				}
			}

			@Override
			public void disconnected() {
				// nothing
			}
			
		});
	}

	@Override
	public boolean startService(String user) {
		try {
			for (int i = 0; i < MAX_NUMBER_OF_TRIALS; i++) {
				addServiceToPool();
				String socketId = null;
				while (socketId == null) {
					synchronized (servicePoolLock) {
						socketId = servicePoolQueue.isEmpty() ? null : servicePoolQueue.poll();
					}
					if (socketId == null) {
						Thread.sleep(TIME_STEP);
						Thread current = Thread.currentThread();
						int priority = current.getPriority();
						if (priority < Thread.MAX_PRIORITY) {
							current.setPriority(priority + 1);
						}
					}
				}
				JSONObject message = new JSONObject();
				message.put("service", serviceID);
				message.put("username", user);
				message.put("socketID", socketId);
				messageConnector.send("startServiceRequest", message);
				for (long elapsedTime = 0; elapsedTime < timeout; elapsedTime += TIME_STEP) {
					if (userToServiceCache.containsKey(user)) {
						return true;
					} else {
						Thread.sleep(TIME_STEP);
					}
				}
				stopService(socketId, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean stopService(String user) {
		String socketId = userToServiceCache.remove(user);
		try {
			stopService(socketId, user);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private void stopService(String socketId, String user) throws JSONException {
		if (socketId != null) {
			JSONObject message = new JSONObject();
			message.put("service", serviceID);
			if (user != null) {
				message.put("username", user);
			}
			message.put("socketID", socketId);
			messageConnector.send("shutdownService", message);
		}
	}
	
	public void setServicePoolSize(final int size) {
		if (size <= 0) {
			throw new IllegalArgumentException("Cannot be negative or zero!");
		}
		
		synchronized(poolSizeLock) {
			if (messageConnector.isConnected()) {
	 			synchronized(servicePoolLock) {
					for (int i = size; i < maxPoolSize && i < servicePoolQueue.size(); i++) {
						try {
							stopService(servicePoolQueue.pollLast(), null);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					for (int i = maxPoolSize; i < size; i++) {
						addServiceToPool();
					}
				}
	 			this.maxPoolSize = size;
			} else {
				maxPoolSize = size;
			}
		}
	}
	
	abstract protected void addServiceToPool();

}
