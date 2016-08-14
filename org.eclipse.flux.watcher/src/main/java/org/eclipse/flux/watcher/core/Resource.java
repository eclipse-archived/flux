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

import org.eclipse.flux.watcher.core.utils.ResourceHelper;

import static org.eclipse.flux.watcher.core.Resource.ResourceType.FILE;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.FOLDER;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.UNKNOWN;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a project resource in a repository.
 *
 * @author Kevin Pollet
 */
public class Resource {
    private static final String NULL_CONTENT_HASH = "0";

    private final String       path;
    private final long         timestamp;
    private final String       hash;
    private final ResourceType type;
    private final byte[]       content;

    /**
     * Constructs an instance of {@link com.codenvy.flux.watcher.core.Resource} representing a {@link
     * Resource.ResourceType#UNKNOWN}.
     *
     * @param path
     *         the {@link com.codenvy.flux.watcher.core.Resource} relative path.
     * @param timestamp
     *         the {@link com.codenvy.flux.watcher.core.Resource} timestamp.
     * @return the new {@link com.codenvy.flux.watcher.core.Resource} instance.
     * @throws java.lang.NullPointerException
     *         if {@code path} parameter is {@code null}.
     */
    public static Resource newUnknown(String path, long timestamp) {
        return new Resource(path, timestamp, UNKNOWN, null);
    }

    /**
     * Constructs an instance of {@link com.codenvy.flux.watcher.core.Resource} representing a {@link
     * Resource.ResourceType#FOLDER}.
     *
     * @param path
     *         the {@link com.codenvy.flux.watcher.core.Resource} relative path.
     * @param timestamp
     *         the {@link com.codenvy.flux.watcher.core.Resource} timestamp.
     * @return the new {@link com.codenvy.flux.watcher.core.Resource} instance.
     * @throws java.lang.NullPointerException
     *         if {@code path} parameter is {@code null}.
     */
    public static Resource newFolder(String path, long timestamp) {
        return new Resource(path, timestamp, FOLDER, null);
    }

    /**
     * Constructs an instance of {@link com.codenvy.flux.watcher.core.Resource} representing a {@link
     * Resource.ResourceType#FILE}.
     *
     * @param path
     *         the {@link com.codenvy.flux.watcher.core.Resource} relative path.
     * @param timestamp
     *         the {@link com.codenvy.flux.watcher.core.Resource} timestamp.
     * @param content
     *         the {@link com.codenvy.flux.watcher.core.Resource} content.
     * @return the new {@link com.codenvy.flux.watcher.core.Resource} instance.
     * @throws java.lang.NullPointerException
     *         if {@code path} parameter is {@code null}.
     */
    public static Resource newFile(String path, long timestamp, byte[] content) {
        return new Resource(path, timestamp, FILE, content);
    }

    /**
     * Constructs an instance of {@link com.codenvy.flux.watcher.core.Resource}.
     *
     * @param path
     *         the {@link com.codenvy.flux.watcher.core.Resource} relative path.
     * @param timestamp
     *         the {@link com.codenvy.flux.watcher.core.Resource} timestamp.
     * @param type
     *         the {@link com.codenvy.flux.watcher.core.Resource} {@link Resource.ResourceType}.
     * @param content
     *         the {@link com.codenvy.flux.watcher.core.Resource} content.
     * @throws java.lang.NullPointerException
     *         if {@code path} or {@code type} parameter is {@code null}.
     */
    private Resource(String path, long timestamp, ResourceType type, byte[] content) {
        this.path = checkNotNull(path);
        this.timestamp = timestamp;
        this.type = checkNotNull(type);
        this.content = content;
        hash = content != null ? ResourceHelper.sha1(content) : NULL_CONTENT_HASH;
    }

    /**
     * Returns the relative path of this {@link com.codenvy.flux.watcher.core.Resource}.
     *
     * @return this {@link com.codenvy.flux.watcher.core.Resource} relative path, never {@code null}.
     */
    public String path() {
        return path;
    }

    /**
     * Returns the timestamp of this {@link com.codenvy.flux.watcher.core.Resource}.
     *
     * @return this {@link com.codenvy.flux.watcher.core.Resource} timestamp.
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Returns the type hash this {@link com.codenvy.flux.watcher.core.Resource}.
     *
     * @return this {@link com.codenvy.flux.watcher.core.Resource} hash, never {@code null}.
     */
    public String hash() {
        return hash;
    }

    /**
     * Returns the type of this {@link com.codenvy.flux.watcher.core.Resource}.
     *
     * @return this {@link com.codenvy.flux.watcher.core.Resource} type, never {@code null}.
     */
    public ResourceType type() {
        return type;
    }

    /**
     * Returns the content of this {@link com.codenvy.flux.watcher.core.Resource}.
     *
     * @return this {@link com.codenvy.flux.watcher.core.Resource} content, {@code null} if none.
     */
    public byte[] content() {
        return content;
    }

    /**
     * The {@link com.codenvy.flux.watcher.core.Resource} type.
     */
    public enum ResourceType {
        FILE,
        FOLDER,
        UNKNOWN
    }

    @Override
    public String toString() {
        return "Resource:" + path + ", " + type + ", " + hash + ", " + timestamp;
    }
}
