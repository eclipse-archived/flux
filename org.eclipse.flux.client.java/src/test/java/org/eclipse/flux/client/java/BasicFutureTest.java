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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipse.flux.client.util.BasicFuture;
import org.eclipse.flux.client.util.ExceptionUtil;

public class BasicFutureTest extends TestCase {
	
	private Timer timer;

	@Override
	protected void setUp() throws Exception {
		timer = new Timer();
	}
	
	@Override
	protected void tearDown() throws Exception {
		timer.cancel();
	}
	
	/**
	 * Test that setTimeout makes a future reject after some time.
	 */
	public void testTimeout() {
		BasicFuture<Void> f = new BasicFuture<Void>();
		f.setTimeout(500);
		try {
			f.get();
			Assert.fail("Should have thrown a TimeoutException");
		} catch (Throwable e) {
			assertTrue(ExceptionUtil.getDeepestCause(e) instanceof TimeoutException);
		}
	}

	/**
	 * Test that value resolved is returned by get, if resolve is called before get.
	 */
	public void testResolveBeforeGet() throws Exception {
		BasicFuture<String> f = new BasicFuture<String>();
		f.resolve("foo");
		f.setTimeout(200);
		assertEquals("foo", f.get());
	}
	
	/**
	 * Test that value resolved is returned by get, if resolve is called after get.
	 */
	public void testResolveAfterGet() throws Exception {
		BasicFuture<String> f = new BasicFuture<String>();
		resolveAfter(f, 200, "foo");
		f.setTimeout(500);
		assertEquals("foo", f.get());
	}
	
	/**
	 * Test that double resolve returns first value
	 */
	public void testDoubleResolve() throws Exception {
		BasicFuture<String> f = new BasicFuture<String>();
		f.resolve("first");
		f.resolve("second");
		f.setTimeout(500);
		assertEquals("first", f.get());
	}
	
	/**
	 * Test that double resolve returns first value
	 */
	public void testRejectResolve() throws Exception {
		BasicFuture<String> f = new BasicFuture<String>();
		f.reject(new Error("Fail"));
		f.resolve("success");
		f.setTimeout(500);
		assertEquals("Fail", getException(f).getMessage());
	}
	
	/**
	 * Test that double resolve returns first value
	 */
	public void testResolveReject() throws Exception {
		BasicFuture<String> f = new BasicFuture<String>();
		f.resolve("success");
		f.reject(new Error("Fail"));
		f.setTimeout(500);
		assertEquals("success", f.get());
	}

	private <T> Throwable getException(BasicFuture<T> f) {
		try {
			T v = f.get();
			fail("Should have thrown an exception but returned "+v);
			throw new Error("unreachable code");
		} catch (Throwable e) {
			return ExceptionUtil.getDeepestCause(e);
		}
	}

	private <T> void resolveAfter(final BasicFuture<T> f, long delay, final T value) {
		timer.schedule(new TimerTask() {
			public void run() {
				f.resolve(value);
			}
		}, delay);
	}
}
