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

import org.eclipse.flux.watcher.core.spi.Project;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event sent when a modification is done in the repository.
 *
 * @author Kevin Pollet
 * @see com.codenvy.flux.watcher.core.RepositoryEventType
 * @see com.codenvy.flux.watcher.core.Resource
 * @see com.codenvy.flux.watcher.core.spi.Project
 */
public class RepositoryEvent {
    private final RepositoryEventType type;
    private final Project             project;
    private final Resource            resource;

    /**
     * Constructs an instance of {@link com.codenvy.flux.watcher.core.RepositoryEvent}.
     *
     * @param type
     *         the {@link com.codenvy.flux.watcher.core.RepositoryEventType}.
     * @param resource
     *         the {@link com.codenvy.flux.watcher.core.Resource} source of the event.
     * @param project
     *         the {@link com.codenvy.flux.watcher.core.spi.Project} source of the event.
     * @throws java.lang.NullPointerException
     *         if {@code type}, {@code resource} or {@code project} parameter is {@code null}.
     */
    public RepositoryEvent(RepositoryEventType type, Resource resource, Project project) {
        this.project = checkNotNull(project);
        this.type = checkNotNull(type);
        this.resource = checkNotNull(resource);
    }

    /**
     * Returns the {@link com.codenvy.flux.watcher.core.RepositoryEventType} of this event.
     *
     * @return the {@link com.codenvy.flux.watcher.core.RepositoryEventType}, never {@code null}.
     */
    public RepositoryEventType type() {
        return type;
    }

    /**
     * Returns the {@link com.codenvy.flux.watcher.core.Resource} source of this event.
     *
     * @return the {@link com.codenvy.flux.watcher.core.Resource}, never {@code null}.
     */
    public Resource resource() {
        return resource;
    }

    /**
     * Returns the {@link com.codenvy.flux.watcher.core.spi.Project} source of this event.
     *
     * @return the {@link com.codenvy.flux.watcher.core.spi.Project} source of this event, never {@code null}.
     */
    public Project project() {
        return project;
    }
}
