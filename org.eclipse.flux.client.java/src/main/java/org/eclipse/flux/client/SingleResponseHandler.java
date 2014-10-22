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
package org.eclipse.flux.client;

import static org.eclipse.flux.client.MessageConstants.ERROR;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.flux.client.util.BasicFuture;
import org.json.JSONObject;

/**
 * This class provides a easier way to handle responses if you are content with
 * just getting the first response to your request.
 * <p>
 * It handles thread synchronization and will timeout if no resonpse arrives within
 * some time limit (currently the timeout is fixed to 1000 milliseconds.
 * <p>
 * Typical use:
 * <p>
 * SingleResponseHandler<ProjectList> resp = new FluxResponseHandler(conn, "getProjectsResponse", "kdvolder") {
 *      protected ProjectList parse(JSONObject message) throws Exception {
 *          ... extract ProjectList from message...
 *          ... throw some exception if message doesn't parse...
 *      }
 * }
 * conn.send(...send the request...);
 * return resp.awaitResult();
 * <p>
 * WARNING: SingleResponseHandler is convenient but it is also arguably somewhat
 * of an 'anti-pattern' in Flux because generally multiple parties may respond
 * to a request and the requestor normally should handle that or risk having
 * incomplete or outdated information.
 */
public abstract class SingleResponseHandler<T> extends MessageHandler {
	
	public static final String USERNAME = "username";

	/**
	 * Positive timeout in milliseconds. Negative number or 0 means 'infinite'.
	 */
	private static final long TIME_OUT = 30 * 1000; //quite long for now, for debugging purposes 
	
	private static Timer timer;
	
	/**
	 * Timer thread shared between all 'FluxResponseHandler' to handle timeouts.
	 */
	private static synchronized Timer timer() {
		if (timer==null) {
			timer = new Timer(SingleResponseHandler.class.getName()+"_TIMER", true);
		}
		return timer;
	}
	
	private AtomicBoolean timeoutStarted = new AtomicBoolean(false);
	
	private MessageConnector conn;
	private BasicFuture<T> future; // the result goes in here once we got it.

	private void cleanup() {
		MessageConnector c = this.conn; // local var: thread safe
		if (c!=null) {
			this.conn = null;
			c.removeMessageHandler(this);
		}
	}
	
	public SingleResponseHandler(MessageConnector conn, String messageType) {
		super(messageType);
		this.conn = conn;
		this.future = new BasicFuture<T>();
		this.future.whenDone(new Runnable() {
			@Override
			public void run() {
				cleanup();
			}
		});
		conn.addMessageHandler(this);
	}

	@Override
	public void handle(String type, JSONObject message) {
		try {
			errorParse(type, message);
			future.resolve(parse(type, message));
		} catch (Throwable e) {
			future.reject(e);
		}
	}

	/**
	 * Should inspect the message to determine if it is an 'error'
	 * response and throw an exception in that case. Do nothing otherwise.
	 */
	protected void errorParse(String messageType, JSONObject message) throws Exception {
		if (message.has(ERROR)) {
			if (message.has("errorDetails")) {
				System.err.println(message.get("errorDetails"));
			}
			throw new Exception(message.getString(ERROR));
		}
	}

	protected abstract T parse(String messageType, JSONObject message) throws Exception;

	/**
	 * Block while waiting for the response. Returns the result once its been received.
	 */
	public T awaitResult() throws Exception {
		return getFuture().get();
	}

	private void ensureTimeout() {
		if (!future.isDone() && TIME_OUT>0 && timeoutStarted.compareAndSet(false, true)) {
			timer().schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						future.reject(new TimeoutException());
					} catch (Throwable e) {
						//don't let Exception fly.. or the timer thread will die!
						e.printStackTrace();
					}
				}
			}, TIME_OUT);
		}
	}

	public BasicFuture<T> getFuture() {
		ensureTimeout();
		return future;
	}

}
