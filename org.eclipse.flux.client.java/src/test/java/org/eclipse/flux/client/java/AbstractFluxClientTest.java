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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageHandler;
import org.eclipse.flux.client.util.BasicFuture;
import org.eclipse.flux.client.util.ExceptionUtil;
import org.json.JSONObject;

public abstract class AbstractFluxClientTest extends TestCase {


    /** 
     * Limits the duration of various operations in the test harness so that we
     * don't have infinite hanging tests if something goes wrong.
     */
	public static final long TIMEOUT = 60000; 
	
	private final List<Process<?>> processes = new ArrayList<>();

	protected abstract MessageConnector createConnection(String user) throws Exception;
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		
		//ensure all processes are terminated.
		synchronized (processes) {
			for (Process<?> process : processes) {
				try {
					process.stop();
				} catch (Throwable e) {
				}
			}
		}
	}
	
	/**
	 * A 'test process' is essentially a thread with some convenient methods to be
	 * able to easily script test sequences that send / receive messages to / from 
	 * a flux connection. There is also a built-in timeout mechanism that ensures
	 * no test process runs forever. To use tis class simply subclass (typically
	 * with a anonymous class) and implement the 'execute' method.
	 */
	public abstract class Process<T> extends Thread {
		
		protected MessageConnector conn;
		public final BasicFuture<T> result;

		public Process(String user) throws Exception {
			this.result = new BasicFuture<>();
			result.setTimeout(TIMEOUT);
			this.conn = createConnection(user);
			this.conn.connectToChannelSync(user);
			//Make sure a 'stuck' thread is not left running after a timeout.
			result.whenDone(new Runnable() {
				public void run() {
					Process.this.stop();
				}
			});
			synchronized (processes) {
				processes.add(this);
			}
		}
		
		@Override
		public final void run() {
			try {
				this.result.resolve(execute());
			} catch (Throwable e) {
				result.reject(e);
			} finally {
				this.conn.disconnect();
			}
		}
		
		
		public void send(String type, JSONObject msg) throws Exception {
			conn.send(type, msg);
		}

		/**
		 * Asynchronous receive. Returns a BasicFuture that resolves when message
		 * is received.
		 */
		public BasicFuture<JSONObject> areceive(String type) {
			final BasicFuture<JSONObject> result = new BasicFuture<JSONObject>();
			result.setTimeout(TIMEOUT);
			once(new MessageHandler(type) {
				public void handle(String type, JSONObject message) {
					result.resolve(message);
				}
			});
			return result;
		}
		
		/**
		 * Synchronous receive. Blocks until message of given type is received.
		 */
		public JSONObject receive(String type) throws Exception {
			return areceive(type).get();
		}

		public void once(final MessageHandler messageHandler) {
			conn.addMessageHandler(new MessageHandler(messageHandler.getMessageType()) {
				@Override
				public boolean canHandle(String type, JSONObject message) {
					return messageHandler.canHandle(type, message);
				}
				
				@Override
				public void handle(String type, JSONObject message) {
					conn.removeMessageHandler(this);
					messageHandler.handle(type, message);
				}
			});
		}

		protected abstract T execute() throws Exception;
		
	}
	
	/**
	 * Run a bunch of processes by starting them in the provided order. Once all processes are running,
	 * block until all of them complete. If any one of the processes is terminated by an Exception then
	 * 'run' guarantees that at least one of the exceptions is re-thrown
	 */
	public void run(Process<?>... processes) throws Exception {
		for (Process<?> process : processes) {
			process.start();
		}
		await(processes);
	}

	public void await(Process<?>... processes) throws Exception {
		Throwable error = null;
		for (Process<?> process : processes) {
			try {
				process.result.get();
			} catch (Throwable e) {
				e.printStackTrace();
				if (error==null) {
					error = e;
				}
			}
		}
		if (error!=null) {
			throw ExceptionUtil.exception(error);
		}
	}
	
}
