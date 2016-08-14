/*******************************************************************************
 * Copyright (c) 2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.flux.watcher.core;

/**
 * Listener used to be notified for {@link com.codenvy.flux.watcher.core.Repository} events.
 *
 * @author Kevin Pollet
 */
public interface RepositoryListener {
    /**
     * Called when an events is fired by a {@link com.codenvy.flux.watcher.core.Repository}.
     *
     * @param event
     *         the {@link com.codenvy.flux.watcher.core.RepositoryEvent} instance, never {@code null}.
     * @throws java.lang.Exception
     *         if something goes wrong.
     */
    void onEvent(RepositoryEvent event) throws Exception;
}
