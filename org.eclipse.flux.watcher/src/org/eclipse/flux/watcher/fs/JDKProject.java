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

import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Set;

import static org.eclipse.flux.watcher.core.Resource.ResourceType.FILE;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.FOLDER;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.setLastModifiedTime;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.Files.write;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * {@link com.codenvy.flux.watcher.core.spi.Project} implementation backed by Java {@code FileSystem}.
 *
 * @author Kevin Pollet
 */
@Singleton
public class JDKProject implements Project {
    private final String                 id;
    private final Path                   path;
    private final JDKProjectWatchService watchService;
    private boolean sync;

    /**
     * Constructs an instance of {@link com.codenvy.flux.watcher.fs.JDKProject}.
     *
     * @param fileSystem
     *         the {@link java.nio.file.FileSystem}
     * @param watchService
     *         the {@link JDKProjectWatchService}.
     * @param id
     *         the project id.
     * @param path
     *         the project absolute path.
     * @throws java.lang.NullPointerException
     *         if {@code fileSystem}, {@code watchService}, {@code id} or {@code path} parameter is {@code null}.
     * @throws java.lang.IllegalArgumentException
     *         if {@code path} parameter is not absolute, doesn't exist or is not a folder.
     */
    JDKProject(FileSystem fileSystem, JDKProjectWatchService watchService, String id, String path) {
        this.id = checkNotNull(id);
        this.watchService = checkNotNull(watchService);

        this.path = checkNotNull(fileSystem).getPath(checkNotNull(path));
        checkArgument(exists(this.path) && isDirectory(this.path) && this.path.isAbsolute());

        // start the watch service
        if(!this.watchService.isAlive()) this.watchService.start();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String path() {
        return path.toString();
    }

    @Override
    public void setSynchronized(boolean synchronize) {
        if (synchronize) {
            watchService.watch(this);
        } else {
            watchService.unwatch(this);
        }
        sync = synchronize;
    }
    
    @Override
    public boolean getSynchronized() {
        return sync;
    }

    @Override
    public Set<Resource> getResources() {
        final Set<Resource> resources = new HashSet<>();
        try {

            walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(path)) {
                        final long timestamp = getLastModifiedTime(dir).toMillis();
                        final String relativeResourcePath = path.relativize(dir).toString();

                        resources.add(Resource.newFolder(relativeResourcePath, timestamp));
                    }

                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    final long timestamp = getLastModifiedTime(file).toMillis();
                    final String relativeResourcePath = path.relativize(file).toString();
                    final byte[] content = readAllBytes(file);

                    resources.add(Resource.newFile(relativeResourcePath, timestamp, content));

                    return CONTINUE;
                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return resources;
    }

    @Override
    public Resource getResource(String resourcePath) {
        checkNotNull(resourcePath);

        final Path resource = path.resolve(resourcePath);
        if (exists(resource)) {
            try {
                final boolean isDirectory = isDirectory(resource);
                final long timestamp = getLastModifiedTime(resource).toMillis();

                return isDirectory ? Resource.newFolder(resourcePath, timestamp)
                                   : Resource.newFile(resourcePath, timestamp, readAllBytes(resource));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    @Override
    public void createResource(Resource resource) {
        checkNotNull(resource);

        final Path resourcePath = path.resolve(resource.path());
        if (!exists(resourcePath)) {
            try {

                if (resource.type() == FOLDER) {
                    createDirectory(resourcePath);
                    setLastModifiedTime(resourcePath, FileTime.from(resource.timestamp(), MILLISECONDS));

                } else if (resource.type() == FILE) {
                    write(resourcePath, resource.content());
                    setLastModifiedTime(resourcePath, FileTime.from(resource.timestamp(), MILLISECONDS));
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void updateResource(Resource resource) {
        checkNotNull(resource);
        checkArgument(resource.type() == FILE);

        final Path resourcePath = path.resolve(resource.path());
        if (exists(resourcePath)) {
            try {

                write(resourcePath, resource.content());
                setLastModifiedTime(resourcePath, FileTime.from(resource.timestamp(), MILLISECONDS));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void deleteResource(Resource resource) {
        checkNotNull(resource);

        final Path resourcePath = path.resolve(resource.path());
        if (exists(resourcePath)) {
            try {

                Files.walkFileTree(resourcePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        delete(file);
                        return CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path folder, IOException exc) throws IOException {
                        delete(folder);
                        return CONTINUE;
                    }
                });

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JDKProject that = (JDKProject)o;

        if (!id.equals(that.id)) return false;
        if (!path.equals(that.path)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }
}
