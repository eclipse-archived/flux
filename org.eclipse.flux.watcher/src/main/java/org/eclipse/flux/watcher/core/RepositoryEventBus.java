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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.notNull;
import static java.util.Arrays.asList;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

/**
 * Event bus to listen and fire {@link com.codenvy.flux.watcher.core.Repository} events.
 *
 * @author Kevin Pollet
 * @see com.codenvy.flux.watcher.core.RepositoryEvent
 */
@Singleton
public class RepositoryEventBus {
    private final Provider<Repository>    repository;
    private final Set<RepositoryListener> repositoryListeners;

    /**
     * Constructs an instance of {@link com.codenvy.flux.watcher.core.RepositoryEventBus}.
     *
     * @param repositoryListeners
     *         the repository listeners to register.
     * @param repository
     *         the {@link com.codenvy.flux.watcher.core.Repository}.
     * @throws java.lang.NullPointerException
     *         if {@code repositoryListeners} or {@code repository} parameter is {@code null}.
     */
    @Inject
    public RepositoryEventBus(Set<RepositoryListener> repositoryListeners, Provider<Repository> repository) {
        this.repository = checkNotNull(repository);
        this.repositoryListeners = new CopyOnWriteArraySet<>(checkNotNull(repositoryListeners));
    }

    /**
     * Adds a {@link com.codenvy.flux.watcher.core.RepositoryListener}.
     *
     * @param listener
     *         the {@link com.codenvy.flux.watcher.core.RepositoryListener} to add.
     * @return {@code true} if the listener was not already added, {@code false} otherwise.
     * @throws java.lang.NullPointerException
     *         if {@code listener} parameter is {@code null}.
     */
    public boolean addRepositoryListener(RepositoryListener listener) {
        return repositoryListeners.add(checkNotNull(listener));
    }

    /**
     * Removes a {@link com.codenvy.flux.watcher.core.RepositoryListener}.
     *
     * @param listener
     *         the {@link com.codenvy.flux.watcher.core.RepositoryListener} to remove.
     * @return {@code true} if the listener has been removed, {@code false} otherwise.
     * @throws java.lang.NullPointerException
     *         if {@code listener} parameter is {@code null}.
     */
    public boolean removeRepositoryListener(RepositoryListener listener) {
        return repositoryListeners.remove(checkNotNull(listener));
    }

    /**
     * Fires a {@link com.codenvy.flux.watcher.core.RepositoryEvent} to all {@link com.codenvy.flux.watcher.core.RepositoryListener}
     * registered.
     *
     * @param event
     *         the {@link com.codenvy.flux.watcher.core.RepositoryEvent} to fire.
     * @throws java.lang.NullPointerException
     *         if {@code event} parameter is {@code null}.
     * @throws java.lang.IllegalStateException
     *         if the {@link com.codenvy.flux.watcher.core.spi.Project} concerned by the event is not in the {@link
     *         com.codenvy.flux.watcher.core.Repository}.
     */
    public void fireRepositoryEvent(final RepositoryEvent event) {
        checkNotNull(event);
        checkState(repository.get().getProject(event.project().id()) != null);

        final Set<RepositoryListener> filteredRepositoryListeners = ImmutableSet.copyOf(FluentIterable
                .from(repositoryListeners)
                .filter(notNull())
                .filter(new Predicate<RepositoryListener>() {
                    @Override
                    public boolean apply(RepositoryListener listener) {
                        final RepositoryEventTypes repositoryEventTypes = listener.getClass().getAnnotation(RepositoryEventTypes.class);
                        return asList(repositoryEventTypes.value()).contains(event.type());
                    }
                }));;

        for (RepositoryListener oneRepositoryListener : filteredRepositoryListeners) {
            try {

                oneRepositoryListener.onEvent(event);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
