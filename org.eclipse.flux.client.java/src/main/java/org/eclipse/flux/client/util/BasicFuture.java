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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Basic implementation of Future interface. Note: Apache http libs have something similar to
 * this, but we don't want a dependency on that lib.
 * 
 * @author Kris De Volder
 */
public class BasicFuture<T> implements Future<T> {

	private boolean isDone = false;
	
	private Throwable exception; //set when 'rejected'
	private T value; //set when 'resolved'

	private Runnable onDone;
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
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
			runOnDone = onDone;
			onDone = null;
		}
		//Careful to call runnable outside synch block. We don't know what it does and
		// it should be responsible for its own thread synch/locks. This is still
		//thread safe because using a local variable
		if (runOnDone!=null) {
			runOnDone.run();
		}
	}
	
	@Override
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

	@Override
	public synchronized T get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Schedule a runnable to be executed when this future transitions to 'done' state
	 */
	public synchronized void whenDone(Runnable runnable) {
		if (onDone!=null) {
			throw new IllegalStateException("whenDone callback already registered");
		}
		onDone = runnable;
	}

}
