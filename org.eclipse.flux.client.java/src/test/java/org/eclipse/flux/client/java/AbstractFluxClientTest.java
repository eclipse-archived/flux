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
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.TestCase;

import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageHandler;
import org.eclipse.flux.client.SingleResponseHandler;
import org.eclipse.flux.client.util.BasicFuture;
import org.eclipse.flux.client.util.ExceptionUtil;
import org.json.JSONObject;

/**
 * Test harness for FluxClient. Subclass this class to use it.
 *  
 * @author Kris De Volder
 */
public abstract class AbstractFluxClientTest extends TestCase {

	public static abstract class ResponseHandler<T> {
		protected abstract T handle(String messageType, JSONObject msg) throws Exception;
	}

	/** 
	 * Limits the duration of various operations in the test harness so that we
	 * can, for example also write 'negative' tests that succeed only if
	 * certain messages are not received (within the timeout).
	 */
	public static final long TIMEOUT = 2000; 
	
	/**
	 * The expectation in the test harness is that Processes are meant to terminate naturally 
	 * without raising exceptions, within a reasonable time. When a process gets stuck
	 * this timeout kicks in to allow operations that are waiting for processes to terminate to proceed. 
	 */
	public static final long STUCK_PROCESS_TIMEOUT = 60000;
	
	public static <T> void assertError(Class<? extends Throwable> expected, BasicFuture<T> r) {
		T result = null;
		Throwable error = null;
		try {
			result = r.get();
		} catch (Throwable e) {
			error = e;
		}
		assertTrue("Should have thrown "+expected.getName()+" but returned "+result,
				error!=null 
				&& ( expected.isAssignableFrom(error.getClass()) 
					|| expected.isAssignableFrom(ExceptionUtil.getDeepestCause(error).getClass())
				)
		);
	}
	
	public static <T> void assertError(String expectContains, BasicFuture<T> r) {
		T result = null;
		Throwable error = null;
		try {
			result = r.get();
		} catch (Throwable e) {
			error = e;
		}
		assertTrue("Should have thrown '..."+expectContains+"...' but returned "+result, 
				error!=null 
				&& ( contains(error.getMessage(), expectContains)
				  || contains(ExceptionUtil.getDeepestCause(error).getMessage(), expectContains)
				)
		);
	}

	private Timer timer;

	/**
	 * Javascript style 'setTimeout' useful for tests that are doing 'callback' style things rather thread-style waiting.
	 */
	public void setTimeout(long delay, TimerTask task) {
		timer().schedule(task, delay);
	}
	
	private synchronized Timer timer() {
		if (timer==null) {
			timer = new Timer();
		}
		return timer;
	}

	private static boolean contains(String message, String expectContains) {
		return message!=null && message.contains(expectContains);
	}

	private final List<Process<?>> processes = new ArrayList<>();

	protected abstract MessageConnector createConnection(String user) throws Exception;
	
	@Override
	protected void tearDown() throws Exception {
		try {
			super.tearDown();
			
			//ensure all processes are terminated.
			synchronized (processes) {
				for (Process<?> process : processes) {
					assertTrue("Process not started", process.hasRun);
					assertFalse("Poorly behaved tests, left a processes running", process.isAlive());
				}
			}
		} finally {
			//Make sure this gets executed no matter what or there will be Thread leakage!
			if (timer!=null) {
				timer.cancel();
			}
		}
	}
	
	/**
	 * A 'test process' is essentially a thread with some convenient methods to be
	 * able to easily script test sequences that send / receive messages to / from 
	 * a flux connection. There is also a built-in timeout mechanism that ensures
	 * no test process runs forever. To use this class simply subclass (typically
	 * with a anonymous class) and implement the 'execute' method.
	 * <p>
	 * A well behaved process should terminate naturally without throwing an exception.
	 * The test harness tries to detect if a process is not well behaved.
	 */
	public abstract class Process<T> extends Thread {
		
		protected MessageConnector conn;
		public final BasicFuture<T> result;
		boolean hasRun = false; //To be able to detect mistakes in tests where a process is created but never started.
								// It doesn't really make sense to create a Process if this process is never being run
								// so this almost certainly means there's a bug in the test that created the process.

		public Process(String user) throws Exception {
			this.result = new BasicFuture<>();
			this.conn = createConnection(user);
			this.conn.connectToChannelSync(user);
			this.result.setTimeout(STUCK_PROCESS_TIMEOUT);
			synchronized (processes) {
				processes.add(this);
			}
		}
		
		@Override
		public final void run() {
			hasRun = true;
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
		 * Asynchronously send a request and return a Future with the response.
		 */
		public <R> BasicFuture<R> asendRequest(final String messageType, JSONObject msg, final ResponseHandler<R> responseHandler) throws Exception {
			SingleResponseHandler<R> response = new SingleResponseHandler<R>(conn, responseType(messageType)) {
				@Override
				protected R parse(String messageType, JSONObject message) throws Exception {
					return responseHandler.handle(messageType, message);
				}
			};
			conn.addMessageHandler(response);
			send(messageType, msg);
			return response.getFuture();
		}

		/**
		 * Synchronously send a request and return the response.
		 */
		public <R> R sendRequest(String messageType, JSONObject msg, ResponseHandler<R> responseHandler) throws Exception {
			return asendRequest(messageType, msg, responseHandler).get();
		}
		
		private String responseType(String messageType) {
			if (messageType.endsWith("Request")) {
				return messageType.substring(0, messageType.length()-"Request".length()) + "Response";
			}
			throw new IllegalArgumentException("Not a 'Request' message type: "+messageType);
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
		//Allthough the work the Processes are doing is 'finished' It is possible the threads themselves
		// are still 'busy' for a brief time thereafter so wait for the threads to die.
		for (Process<?> process : processes) {
			process.join(500); //shouldn't be long (unless test is ill-behaved and process is 'stuck', but then it would 
								// not be possible to reach this point, since at least a TimeoutException will be raised
								// above as a result of that 'stuck' Process's result.promise timing out.
		}
	}
	
}
