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

import org.eclipse.flux.watcher.core.RepositoryEvent;
import org.eclipse.flux.watcher.core.RepositoryEventBus;
import org.eclipse.flux.watcher.core.RepositoryEventType;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.flux.watcher.core.RepositoryEventType.PROJECT_RESOURCE_CREATED;
import static org.eclipse.flux.watcher.core.RepositoryEventType.PROJECT_RESOURCE_DELETED;
import static org.eclipse.flux.watcher.core.RepositoryEventType.PROJECT_RESOURCE_MODIFIED;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.nio.file.WatchEvent.Kind;

/**
 * Thread watching the file system to notify clients about modifications.
 *
 * @author Kevin Pollet
 */
public class JDKProjectWatchService extends Thread {
    private final WatchService          watchService;
    private final BiMap<WatchKey, Path> watchKeyToPath;
    private final Map<Path, Project>    pathToProject;
    private final Object                mutex;
    private final RepositoryEventBus    repositoryEventBus;
    private final FileSystem            fileSystem;

    /**
     * Constructs an instance of {@link JDKProjectWatchService}.
     *
     * @param fileSystem
     *         the {@link java.nio.file.FileSystem} to watch.
     * @param repositoryEventBus
     *         the {@link com.codenvy.flux.watcher.core.RepositoryEvent} bus.
     * @throws java.lang.NullPointerException
     *         if {@code repositoryEventBus} or {@code fileSystem} parameter is {@code null}.
     */
    JDKProjectWatchService(FileSystem fileSystem, RepositoryEventBus repositoryEventBus) {
        this.watchKeyToPath = HashBiMap.create();
        this.pathToProject = new HashMap<>();
        this.mutex = new Object();
        this.repositoryEventBus = checkNotNull(repositoryEventBus);
        this.fileSystem = checkNotNull(fileSystem);

        try {

            this.watchService = fileSystem.newWatchService();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void watch(Project project) {
        checkNotNull(project);
        watch(project, fileSystem.getPath(project.path()));
    }

    private void watch(final Project project, Path path) {
        checkNotNull(path);
        checkArgument(exists(path) && isDirectory(path) && path.isAbsolute());

        synchronized (mutex) {
            try {

                walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (!watchKeyToPath.containsValue(dir)) {
                            final WatchKey watchKey = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

                            watchKeyToPath.put(watchKey, dir);
                            pathToProject.put(dir, project);
                        }
                        return CONTINUE;
                    }
                });

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void unwatch(Project project) {
        checkNotNull(project);
        unwatch(fileSystem.getPath(project.path()));
    }

    private void unwatch(Path path) {
        checkNotNull(path);
        checkArgument(exists(path) && isDirectory(path) && path.isAbsolute());

        synchronized (mutex) {
            try {

                walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        final WatchKey watchKey = watchKeyToPath.inverse().get(dir);
                        if (watchKey != null) {
                            watchKeyToPath.inverse().remove(dir);
                            pathToProject.remove(dir);
                            watchKey.cancel();
                        }
                        return CONTINUE;
                    }
                });

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Process all events for keys queued to the watcher.
     */
    @Override
    public void run() {
        while (!isInterrupted()) {
            try {

                final WatchKey watchKey = watchService.take();
                synchronized (mutex) {
                    if (!watchKeyToPath.containsKey(watchKey)) {
                        continue;
                    }
                }

                for (WatchEvent<?> oneEvent : watchKey.pollEvents()) {
                    final Path watchablePath = (Path)watchKey.watchable();
                    final WatchEvent<Path> pathEvent = cast(oneEvent);
                    final Path resourcePath = watchablePath.resolve(pathEvent.context());

                    if (oneEvent.kind() == OVERFLOW) {
                        continue;
                    }

                    if (oneEvent.kind() == ENTRY_CREATE && isDirectory(resourcePath)) {
                        watch(pathToProject.get(watchablePath), resourcePath);
                    }

                    final Project project = pathToProject.get(watchablePath);
                    final RepositoryEventType repositoryEventType = kindToRepositoryEventType(pathEvent.kind());
                    final Resource resource = pathToResource(pathEvent.kind(), project, resourcePath);
                    repositoryEventBus.fireRepositoryEvent(new RepositoryEvent(repositoryEventType, resource, project));
                }

                final boolean isValid = watchKey.reset();
                if (!isValid) {
                    synchronized (mutex) {
                        final Path path = watchKeyToPath.remove(watchKey);
                        if (path != null) {
                            pathToProject.remove(path);
                        }
                    }
                }

            } catch (ClosedWatchServiceException | InterruptedException e) {
                return;
            }
        }
    }

    /**
     * Converts a {@link java.nio.file.WatchEvent} {@link java.nio.file.WatchEvent.Kind} to a {@link
     * com.codenvy.flux.watcher.core.RepositoryEventType}.
     *
     * @param kind
     *         the {@link java.nio.file.WatchEvent.Kind} to convert.
     * @return the corresponding {@link com.codenvy.flux.watcher.core.RepositoryEventType} or {@code null} if none.
     */
    private RepositoryEventType kindToRepositoryEventType(Kind<?> kind) {
        if (kind == ENTRY_CREATE) {
            return PROJECT_RESOURCE_CREATED;
        }
        if (kind == ENTRY_MODIFY) {
            return PROJECT_RESOURCE_MODIFIED;
        }
        if (kind == ENTRY_DELETE) {
            return PROJECT_RESOURCE_DELETED;
        }
        return null;
    }

    /**
     * Converts the given resource {@link java.nio.file.Path} to a {@link com.codenvy.flux.watcher.core.Resource}.
     *
     * @param kind
     *         the watch event {@link java.nio.file.WatchEvent.Kind}.
     * @param project
     *         the {@link com.codenvy.flux.watcher.core.spi.Project} containing the resource.
     * @param resourcePath
     *         the absolute resource {@link java.nio.file.Path}.
     * @return the {@link com.codenvy.flux.watcher.core.Resource} instance, never {@code null}.
     * @throws java.lang.NullPointerException
     *         if {@code kind}, {@code project} or {@code resourcePath} is {@code null}.
     * @throws java.lang.IllegalArgumentException
     *         if the resource does not exist and the {@link java.nio.file.WatchEvent.Kind} is not {@link
     *         java.nio.file.StandardWatchEventKinds#ENTRY_DELETE}.
     */
    private Resource pathToResource(Kind<Path> kind, Project project, Path resourcePath) {
        checkNotNull(kind);
        checkNotNull(project);
        checkNotNull(resourcePath);
        checkArgument(resourcePath.isAbsolute());

        try {

            final boolean exists = exists(resourcePath);
            checkArgument(kind == ENTRY_DELETE || exists);

            final Path projectPath = fileSystem.getPath(project.path());
            final String relativeResourcePath = projectPath.relativize(resourcePath).toString();
            final long timestamp = exists ? getLastModifiedTime(resourcePath).toMillis() : System.currentTimeMillis();

            if (exists) {
                final boolean isDirectory = isDirectory(resourcePath);
                return isDirectory ? Resource.newFolder(relativeResourcePath, timestamp)
                                   : Resource.newFile(relativeResourcePath, timestamp, readAllBytes(resourcePath));

            } else {
                return Resource.newUnknown(relativeResourcePath, timestamp);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Cast the given {@link java.nio.file.WatchEvent} to a {@link java.nio.file.Path} {@link java.nio.file.WatchEvent}.
     *
     * @param event
     *         the {@link java.nio.file.WatchEvent} to cast.
     * @return the casted {@link java.nio.file.WatchEvent}.
     * @throws java.lang.NullPointerException
     *         if {@code event} parameter is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private WatchEvent<Path> cast(WatchEvent<?> event) {
        return (WatchEvent<Path>)checkNotNull(event);
    }
}
