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

import org.json.JSONObject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class representing a Flux message.
 *
 * @author Kevin Pollet
 */
public class FluxMessage {
    private final FluxConnection  source;
    private final FluxMessageType type;
    private final JSONObject      content;

    /**
     * Constructs an instance of {@link FluxMessage}.
     *
     * @param type
     *         the {@link FluxMessageType}.
     * @param content
     *         the message {@link org.json.JSONObject} content.
     * @throws java.lang.NullPointerException
     *         if {@code type} or {@code content} parameter is {@code null}.
     */
    public FluxMessage(FluxMessageType type, JSONObject content) {
        this(null, type, content);
    }

    /**
     * Constructs an instance of {@link FluxMessage}.
     *
     * @param source
     *         the {@code FluxConnection} where the {@link FluxMessage} comes from.
     * @param type
     *         the {@link FluxMessageType}.
     * @param content
     *         the message {@link org.json.JSONObject} content.
     * @throws java.lang.NullPointerException
     *         if {@code type} or {@code content} parameter is {@code null}.
     */
    public FluxMessage(FluxConnection source, FluxMessageType type, JSONObject content) {
        this.source = source;
        this.type = checkNotNull(type);
        this.content = checkNotNull(content);
    }

    /**
     * Returns the {@code FluxConnection} where the {@link FluxMessage} comes from.
     *
     * @return the {@code FluxConnection} where the {@link FluxMessage} comes from or {@code null} if none.
     */
    public FluxConnection source() {
        return source;
    }

    /**
     * Returns the {@link FluxMessageType}.
     *
     * @return the {@link FluxMessageType}, never {@code null}.
     */
    public FluxMessageType type() {
        return type;
    }

    /**
     * Returns the {@link FluxMessage} content.
     *
     * @return the {@link FluxMessage} content, never {@code null}.
     */
    public JSONObject content() {
        return content;
    }

    /**
     * Fields used in {@link org.json.JSONObject} of Flux messages.
     *
     * @author Kevin Pollet
     */
    public enum Fields {
        CALLBACK_ID("callback_id"),
        CHANNEL("channel"),
        CONNECTED_TO_CHANNEL("connectedToChannel"),
        CONTENT("content"),
        DELETED("deleted"),
        FILES("files"),
        HASH("hash"),
        INCLUDE_DELETED("includeDeleted"),
        PATH("path"),
        PROJECT("project"),
        REQUEST_SENDER_ID("requestSenderID"),
        RESOURCE("resource"),
        TIMESTAMP("timestamp"),
        TYPE("type"),
        USERNAME("username");

        private final String value;

        Fields(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
