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

/**
 * Convenience 'adapter' that implements CompletionCallback. Can be used when you
 * just want to implement one of the callback methods instead of both.
 * <p>
 * @author Kris De Volder
 */
public abstract class CompletionAdapter<T> implements BasicFuture.CompletionCallback<T> {

	@Override
	public void resolved(T result) {
	}

	@Override
	public void rejected(Throwable e) {
	}

}
