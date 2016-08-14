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
package org.eclipse.flux.watcher.fs;

import org.eclipse.flux.watcher.core.RepositoryEventBus;
import org.eclipse.flux.watcher.core.spi.Project;
import org.eclipse.flux.watcher.core.spi.ProjectFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;

/**
 * {@link com.codenvy.flux.watcher.core.spi.ProjectFactory} implementation.
 *
 * @author Kevin Pollet
 */
@Singleton
public class JDKProjectFactory implements ProjectFactory {
    private final FileSystem             fileSystem;
    private final JDKProjectWatchService watchService;

    /**
     * Constructs an instance of {@link com.codenvy.flux.watcher.fs.JDKProjectFactory}.
     *
     * @param fileSystem
     *         the {@link java.nio.file.FileSystem} instance.
     * @param repositoryEventBus
     *         the {@link com.codenvy.flux.watcher.core.RepositoryEventBus}.
     * @throws java.lang.NullPointerException
     *         if {@code fileSystem} or {@code repositoryEventBus} is {@code null}.
     */
    @Inject
    public JDKProjectFactory(FileSystem fileSystem, RepositoryEventBus repositoryEventBus) {
        this.fileSystem = checkNotNull(fileSystem);
        this.watchService = new JDKProjectWatchService(fileSystem, checkNotNull(repositoryEventBus));
    }

    @Override
    public Project newProject(String projectId, String projectPath) {
        checkNotNull(projectId);
        checkNotNull(projectPath);

        final Path path = fileSystem.getPath(projectPath);
        checkArgument(exists(path) && isDirectory(path) && path.isAbsolute());

        return new JDKProject(fileSystem, watchService, projectId, projectPath);
    }
}
