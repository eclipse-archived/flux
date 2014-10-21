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
package org.eclipse.flux.client.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Basic implementation of Future interface. Note: Apache http libs have something similar to
 * this, but we don't want a hard dependency on that lib.
 * 
 * @author Kris De Volder
 */
public class BasicFuture<T> {
	
	//Note we are not implementing Future interface as its too cumbersome to
	// implement all of it. Ideally we would make this implementation
	// fully conform to JRE's Future interface.

	private boolean isDone = false;
	
	private Throwable exception; //set when 'rejected'
	private T value; //set when 'resolved'
	
	private Collection<CompletionCallback<T>> onDone;

	private Timer timer;

	private TimerTask timeoutTask;

	public interface CompletionCallback<T> {
		void resolved(T result);
		void rejected(Throwable e);
	}
	
	public boolean isDone() {
		return isDone;
	}
	
	public synchronized void resolve(T value) {
		if (isDone) {
			return;
		}
		this.value = value;
		done();
	}
	
	public synchronized void reject(Throwable e) {
		if (isDone) {
			return;
		}
		this.exception = e;
		if (this.exception==null) {
			this.exception = new RuntimeException();
		}
		done();
	}

	/**
	 * Transition to 'done' state. If already in the 'done' state this does nothing.
	 */
	private void done() {
		Runnable runOnDone = null;
		synchronized (this) {
			if (isDone) {
				return;
			}
			isDone = true;
			notifyAll();
			if (onDone!=null) {
				final Object[] callbacks = onDone.toArray();
				runOnDone = new Runnable() {
					@SuppressWarnings("unchecked")
					public void run() {
						for (Object cb : callbacks) {
							callback((CompletionCallback<T>) cb);
						}
					}
				};
			}
			onDone = null;
		}
		//Careful to call callbacks outside synch block. We don't know what they do and
		//they should be responsible for its own thread synch/locks. This is still
		//thread safe because using a local variable
		if (runOnDone!=null) {
			runOnDone.run();
		}
	}
	
	public synchronized T get() throws InterruptedException, ExecutionException {
		waitUntilDone();
		if (this.exception!=null) {
			throw new ExecutionException(exception);
		}
		return value;
	}

	private synchronized void waitUntilDone() throws ExecutionException {
		while (!isDone()) {
			try {
				wait();
			} catch (InterruptedException e) {
				//ignore
			}
		}
	}

	/**
	 * Schedule a runnable to be executed when this future transitions to 'done' state
	 */
	public synchronized void whenDone(final Runnable runnable) {
		whenDone(new CompletionCallback<T>() {
			@Override
			public void resolved(T result) {
				runnable.run();
			}
			
			@Override
			public void rejected(Throwable e) {
				runnable.run();
			}
		});
	}
	
	/**
	 * Add a callback to be called on completion (i.e. when the future resolves or rejects).
	 */
	public synchronized void whenDone(CompletionCallback<T> callback) {
		if (isDone()) {
			callback(callback);
		} else {
			if (onDone==null) {
				onDone = new HashSet<>();
			}
			onDone.add(callback);
		}
	}

	private void callback(CompletionCallback<T> callback) {
		if (exception!=null) {
			callback.rejected(exception);
		} else {
			callback.resolved(value);
		}
	}

	/**
	 * Ensure that this future resolves or rejects within a certain time. 
	 * <p>
	 * If the future is already 'done' this does nothing otherwise it 
	 * schedules a timer task that rejects the future with a TimeoutException when
	 * the time limit is reached. 
	 */
	public void setTimeout(long delay) {
		if (isDone()) {
			return;
		}
		timer().schedule(timeoutTask = new TimerTask() {
			@Override
			public void run() {
				reject(new TimeoutException());
			}
		}, delay);
		whenDone(new Runnable() {
			public void run() {
				timeoutTask.cancel();
				timeoutTask = null;
			}
		});
	}

	private synchronized Timer timer() {
		if (timer==null) {
			timer = new Timer();
		}
		return timer;
	}

}
