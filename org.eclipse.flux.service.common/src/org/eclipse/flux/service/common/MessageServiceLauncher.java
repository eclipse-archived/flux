package org.eclipse.flux.service.common;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class MessageServiceLauncher implements IServiceLauncher {
	
	private static final long MIN_TIMEOUT = 500L;
	private static final long TIME_STEP = 50L;
	
	/**
	 * Time to wait for serviceReady message for all pending services 
	 */
	private static final long DISPOSE_TIMEOUT = 5000L; 
	
	private static final int MAX_NUMBER_OF_TRIALS = 3;
	
	protected String serviceID;
		
	final private long timeout;
	
	final protected int maxPoolSize;
	
	private MessageConnector messageConnector;
	
	private ConcurrentHashMap<String, String> userToServiceCache = new ConcurrentHashMap<String, String>();
	
	private Deque<String> servicePoolQueue = new LinkedList<String>();
	
	private final Object servicePoolLock = new Object();
	
	private final AtomicBoolean active = new AtomicBoolean(false);
	
	private final IMessageHandler[] MESSAGE_HANDLERS = new IMessageHandler[] {
			
			new IMessageHandler() {	
				
				@Override
				public void handle(String type, JSONObject message) {
					try {
						String user = message.getString("username");
						String socketId = message.getString("socketID");
						userToServiceCache.put(user, socketId);
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
			},
			
			new IMessageHandler() {
				
				@Override
				public void handle(String type, JSONObject message) {
					try {
						String socketId = message.getString("socketID");
						synchronized (servicePoolQueue) {
							servicePoolQueue.add(socketId);
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
				
			}			
	};
		
	public MessageServiceLauncher(MessageConnector messageConnector, final String serviceID, int maxPoolSize, long timeout) {
		this.serviceID = serviceID;
		
		if (timeout < MIN_TIMEOUT) {
			throw new IllegalArgumentException("Timeout value cannot be smaller than " + MIN_TIMEOUT + " miliseconds");
		} else {
			this.timeout = timeout;
		}
		
		if (maxPoolSize < 0) {
			throw new IllegalArgumentException("Pool size must not be negative!");
		}
		
		this.maxPoolSize = maxPoolSize;
		this.messageConnector = messageConnector;		
	}

	@Override
	public boolean startService(String user) {
		try {
			for (int i = 0; i < MAX_NUMBER_OF_TRIALS && active.get(); i++) {
				addService(1);
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
				removeService(socketId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean stopService(String user) {
		String socketId = userToServiceCache.remove(user);
		return removeService(socketId);
	}
	
	protected boolean removeService(String socketId) {
		try {
			if (socketId != null) {
				JSONObject message = new JSONObject();
				message.put("service", serviceID);
				message.put("socketID", socketId);
				messageConnector.send("shutdownService", message);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	

	@Override
	public void init() {
		active.compareAndSet(false, true);
		for (IMessageHandler messageHandler : MESSAGE_HANDLERS) {
			messageConnector.addMessageHandler(messageHandler);
		}
		synchronized(servicePoolLock) {
			addService(maxPoolSize);
		}
	}
	
	protected void initServices() {
	}

	@Override
	public void dispose() {
		active.compareAndSet(true, false);
		try {
			Thread.sleep(DISPOSE_TIMEOUT);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for (IMessageHandler messageHandler : MESSAGE_HANDLERS) {
			messageConnector.removeMessageHandler(messageHandler);
		}
		synchronized(servicePoolLock) {
			while (!servicePoolQueue.isEmpty()) {
				removeService(servicePoolQueue.poll());
			}
		}
		while (!userToServiceCache.isEmpty()) {
			removeService(userToServiceCache.keys().nextElement());
		}
	}
	
	abstract protected void addService(int n);

}
