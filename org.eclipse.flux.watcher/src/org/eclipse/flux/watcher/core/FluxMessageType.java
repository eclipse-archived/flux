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

/**
 * The types of {@link com.codenvy.flux.watcher.core.FluxMessage}.
 *
 * @author Kevin Pollet
 */
public enum FluxMessageType {
    CONNECT_TO_CHANNEL("connectToChannel"),
    GET_PROJECT_REQUEST("getProjectRequest"),
    GET_PROJECT_RESPONSE("getProjectResponse"),
    GET_RESOURCE_REQUEST("getResourceRequest"),
    GET_RESOURCE_RESPONSE("getResourceResponse"),
    PROJECT_CONNECTED("projectConnected"),
    PROJECT_DISCONNECTED("projectDisconnected"),
    RESOURCE_CHANGED("resourceChanged"),
    RESOURCE_CREATED("resourceCreated"),
    RESOURCE_DELETED("resourceDeleted"),
    RESOURCE_STORED("resourceStored");

    private final String value;

    /**
     * Constructs an instance of {@link com.codenvy.flux.watcher.core.FluxMessageType}.
     *
     * @param value
     *         the {@link com.codenvy.flux.watcher.core.FluxMessageType} value.
     * @throws java.lang.NullPointerException
     *         if {@code value} parameter is {@code null}.
     */
    FluxMessageType(String value) {
        this.value = checkNotNull(value);
    }

    /**
     * Returns the {@link com.codenvy.flux.watcher.core.FluxMessageType} corresponding to the given type.
     *
     * @param type
     *         the {@link com.codenvy.flux.watcher.core.FluxMessageType} type.
     * @return the {@link com.codenvy.flux.watcher.core.FluxMessageType} corresponding to the given type or {@code null} if none.
     * @throws java.lang.NullPointerException
     *         if {@code type} parameter is {@code null}.
     * @throws java.lang.IllegalArgumentException
     *         if no {@link com.codenvy.flux.watcher.core.FluxMessageType} exists for the given type.
     */
    public static FluxMessageType fromType(String type) {
        checkNotNull(type);

        final FluxMessageType[] messageTypes = FluxMessageType.values();
        for (FluxMessageType oneMessageType : messageTypes) {
            if (oneMessageType.value.equals(type)) {
                return oneMessageType;
            }
        }
        throw new IllegalArgumentException("No enum found for type '" + type + "'");
    }

    /**
     * Returns the {@link com.codenvy.flux.watcher.core.FluxMessageType} value.
     *
     * @return the {@link com.codenvy.flux.watcher.core.FluxMessageType} value, never {@code null}.
     */
    public String value() {
        return value;
    }
}
