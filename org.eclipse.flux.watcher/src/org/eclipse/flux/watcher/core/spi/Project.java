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
package org.eclipse.flux.watcher.core.spi;

import org.eclipse.flux.watcher.core.Resource;

import java.util.Set;

/**
 * Project contract implemented by a provider.
 *
 * @author Kevin Pollet
 * @see com.codenvy.flux.watcher.core.spi.ProjectFactory
 */
public interface Project {
    /**
     * Returns the project unique id.
     *
     * @return the project unique id, never {@code null}.
     */
    String id();

    /**
     * Returns the project absolute path.
     *
     * @return the project absolute path, never {@code null}.
     */
    String path();

    /**
     * Defines if this project is synchronized. The implementer must send the project events to the {@link
     * com.codenvy.flux.watcher.core.RepositoryEventBus}.
     *
     * @param synchronize
     *         {@code true} if the project have to be synchronized, {@code false} otherwise.
     */
    void setSynchronized(boolean synchronize);

    /**
     * Returns whether or not the project is currently synchronized.
     *
     * @return true or false, never {@code null}.
     */
    boolean getSynchronized();
    
    /**
     * Returns all project {@link com.codenvy.flux.watcher.core.Resource}.
     *
     * @return the {@link com.codenvy.flux.watcher.core.Resource} {@link java.util.Set}, never {@code null}.
     */
    Set<Resource> getResources();

    /**
     * Returns the {@link com.codenvy.flux.watcher.core.Resource} with the given relative resource path.
     *
     * @param resourcePath
     *         the {@link com.codenvy.flux.watcher.core.Resource} relative path.
     * @return the {@link com.codenvy.flux.watcher.core.Resource} or {@code null} if not found.
     * @throws java.lang.NullPointerException
     *         if {@code resourcePath} parameter is {@code null}.
     */
    Resource getResource(String resourcePath);

    /**
     * Creates the given {@link com.codenvy.flux.watcher.core.Resource}.
     *
     * @param resource
     *         the {@link com.codenvy.flux.watcher.core.Resource} to be created.
     * @throws java.lang.NullPointerException
     *         if {@code resource} parameter is {@code null}.
     */
    void createResource(Resource resource);

    /**
     * Updates the given {@link com.codenvy.flux.watcher.core.Resource}.
     *
     * @param resource
     *         the {@link com.codenvy.flux.watcher.core.Resource} to be updated.
     * @throws java.lang.NullPointerException
     *         if {@code resource} parameter is {@code null}.
     * @throws java.lang.IllegalArgumentException
     *         if {@code resource} parameter is not a file.
     */
    void updateResource(Resource resource);

    /**
     * Deletes the given {@link com.codenvy.flux.watcher.core.Resource}.
     *
     * @param resource
     *         the {@link com.codenvy.flux.watcher.core.Resource} to be deleted.
     * @throws java.lang.NullPointerException
     *         if {@code resource} parameter is {@code null}.
     */
    void deleteResource(Resource resource);
}
