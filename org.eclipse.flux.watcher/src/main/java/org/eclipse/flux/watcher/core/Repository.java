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
import org.eclipse.flux.watcher.core.spi.ProjectFactory;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.eclipse.flux.watcher.core.FluxMessage.Fields.INCLUDE_DELETED;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.PROJECT;
import static org.eclipse.flux.watcher.core.FluxMessageType.*;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;

/**
 * Represents a repository with Flux connectivity capabilities.
 *
 * @author Kevin Pollet
 */
@Singleton
public class Repository {
    private final Set<Project>       projects;
    private final FluxMessageBus     messageBus;
    private final RepositoryEventBus repositoryEventBus;
    private final ProjectFactory     projectFactory;

    /**
     * Constructs an instance of {@link Repository}.
     *
     * @param messageBus
     *         the {@link com.codenvy.flux.watcher.core.FluxMessageBus} instance.
     * @param projectFactory
     *         the {@link com.codenvy.flux.watcher.core.spi.ProjectFactory} instance.
     * @param repositoryEventBus
     *         the {@link com.codenvy.flux.watcher.core.RepositoryEventBus} instance.
     * @throws java.lang.NullPointerException
     *         if {@code messageBus}, {@code projectFactory} or {@code repositoryEventBus} parameter is {@code null}.
     */
    @Inject
    Repository(FluxMessageBus messageBus, ProjectFactory projectFactory, RepositoryEventBus repositoryEventBus) {
        this.repositoryEventBus = checkNotNull(repositoryEventBus);
        this.messageBus = checkNotNull(messageBus);
        this.projectFactory = checkNotNull(projectFactory);
        this.projects = new CopyOnWriteArraySet<>();
    }

    /**
     * Connects this {@link Repository} to a Flux remote.
     *
     * @param remoteURL
     *         the remote {@link java.net.URL}.
     * @param credentials
     *         the {@link com.codenvy.flux.watcher.core.Credentials} used to connect.
     * @return the opened {@link com.codenvy.flux.watcher.core.FluxConnection}.
     * @throws java.lang.NullPointerException
     *         if {@code remoteURL} or {@code credentials} parameter is {@code null}.
     */
    public FluxConnection addRemote(URL remoteURL, Credentials credentials) {
        return messageBus.connect(checkNotNull(remoteURL), checkNotNull(credentials));
    }

    /**
     * Disconnects this {@link Repository} from a Flux remote.
     *
     * @param remoteURL
     *         the server {@link java.net.URL}.
     * @throws java.lang.NullPointerException
     *         if {@code remoteURL} parameter is {@code null}.
     */
    public void removeRemote(URL remoteURL) {
        messageBus.disconnect(checkNotNull(remoteURL));
    }

    /**
     * Add a project to the repository.
     *
     * @param projectId
     *         the project id.
     * @param projectPath
     *         the absolute project path.
     * @return the added {@link com.codenvy.flux.watcher.core.spi.Project} instance, never {@code null}.
     * @throws java.lang.NullPointerException
     *         if {@code projectId} or {@code projectPath} parameter is {@code null}.
     * @throws java.lang.IllegalArgumentException
     *         if the given {@code projectPath} is not a directory, not absolute or doesn't exist.
     */
    public Project addProject(String projectId, String projectPath) {
        checkNotNull(projectId);
        checkNotNull(projectPath);

        Project project = getProject(projectId);
        if (project == null) {
            project = projectFactory.newProject(projectId, projectPath);
            project.setSynchronized(true);
            projects.add(project);

            try {

                JSONObject content = new JSONObject().put(PROJECT, projectId);
                messageBus.sendMessages(new FluxMessage(PROJECT_CONNECTED, content));

                content = new JSONObject().put(PROJECT, projectId).put(INCLUDE_DELETED, true);
                messageBus.sendMessages(new FluxMessage(GET_PROJECT_REQUEST, content));

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return project;
    }

    /**
     * Remove a project from the repository.
     *
     * @param projectId
     *         the project id.
     * @return the removed {@link com.codenvy.flux.watcher.core.spi.Project} instance or {@code null} if none.
     */
    public Project removeProject(String projectId) {
        final Project project = getProject(projectId);
        if (project != null) {
            projects.remove(project);
            project.setSynchronized(false);

            try {

                final JSONObject content = new JSONObject().put(PROJECT, projectId);
                messageBus.sendMessages(new FluxMessage(PROJECT_DISCONNECTED, content));

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return project;
    }

    /**
     * Returns the {@link com.codenvy.flux.watcher.core.spi.Project} with the given id.
     *
     * @return the {@link com.codenvy.flux.watcher.core.spi.Project} or {@code null} if none.
     */
    public Project getProject(final String projectId) {
        return FluentIterable.from(projects)
                             .firstMatch(new Predicate<Project>() {
                                 @Override
                                 public boolean apply(Project project) {
                                     return Objects.equals(projectId, project.id());
                                 }
                             })
                             .orNull();
    }

    /**
     * Returns all synchronized {@link com.codenvy.flux.watcher.core.spi.Project}.
     *
     * @return a {@link java.util.Set} of all synchronized {@link com.codenvy.flux.watcher.core.spi.Project}.
     */
    public Set<Project> getSynchronizedProjects() {
        return ImmutableSet.copyOf(FluentIterable.from(projects)
                                   .filter(notNull())
                                   .filter(new Predicate<Project>() {
                                       @Override
                                       public boolean apply(Project project) {
                                           return project.getSynchronized();
                                       }
                                   }));
    }
    
    /**
     * Returns the {@link com.codenvy.flux.watcher.core.RepositoryEventBus}.
     *
     * @return the {@link com.codenvy.flux.watcher.core.RepositoryEventBus}, never {@code null}.
     */
    public RepositoryEventBus repositoryEventBus() {
        return repositoryEventBus;
    }

    public FluxMessageBus getMessageBus(){
    	return messageBus;
    }

}
