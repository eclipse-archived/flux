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
package org.eclipse.flux.core.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ExceptionUtil {
	
	//TODO: duplicated code from flux.java.client.

	public static Throwable getDeepestCause(Throwable e) {
		Throwable cause = e;
		Throwable parent = e.getCause();
		while (parent != null && parent != e) {
			cause = parent;
			parent = cause.getCause();
		}
		return cause;
	}

	public static String getMessage(Throwable e) {
		// The message of nested exception is usually more interesting than the
		// one on top.
		Throwable cause = getDeepestCause(e);
		String msg = cause.getClass().getSimpleName() + ": " + cause.getMessage();
		return msg;
	}

	public static String stackTrace(Throwable e) {
		try {
			ByteArrayOutputStream trace = new ByteArrayOutputStream();
			PrintStream dump = new PrintStream(trace, true, "utf8");
			e.printStackTrace(dump);
			return trace.toString("utf8");
		} catch (Exception shouldNotHappen) {
			throw new RuntimeException(shouldNotHappen);
		}
	}

}
