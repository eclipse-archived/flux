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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

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
    Repository(ProjectFactory projectFactory, RepositoryEventBus repositoryEventBus) {
        this.repositoryEventBus = checkNotNull(repositoryEventBus);
        this.projectFactory = checkNotNull(projectFactory);
        this.projects = new CopyOnWriteArraySet<>();
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
}
