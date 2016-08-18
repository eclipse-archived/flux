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

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

import static org.eclipse.flux.watcher.core.FluxMessage.Fields.CALLBACK_ID;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.CHANNEL;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.CONNECTED_TO_CHANNEL;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.USERNAME;
import static org.eclipse.flux.watcher.core.FluxMessageType.CONNECT_TO_CHANNEL;
import static org.eclipse.flux.watcher.core.FluxMessageType.GET_PROJECT_REQUEST;
import static org.eclipse.flux.watcher.core.FluxMessageType.GET_RESOURCE_REQUEST;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a connection to a Flux remote.
 *
 * @author Kevin Pollet
 */
public class FluxConnection {
    private static final String FLUX_USER_NAME_HEADER_KEY  = "X-flux-user-name";
    private static final String FLUX_USER_TOKEN_HEADER_KEY = "X-flux-user-token";

    private final SocketIO       socket;
    private final FluxMessageBus messageBus;
    private final Credentials    credentials;

    /**
     * Constructs an instance of {@code FluxConnection}.
     *
     * @param serverURL
     *         the server {@link java.net.URL} to connect to.
     * @param credentials
     *         the {@link Credentials} used to connect.
     * @param messageBus
     *         the {@link com.codenvy.flux.watcher.core.FluxMessageBus} instance.
     * @throws java.lang.NullPointerException
     *         if {@code serverURL}, {@code credentials} or {@code messageBus} parameter is {@code null}.
     */
    FluxConnection(URL serverURL, Credentials credentials, FluxMessageBus messageBus) {
        this.messageBus = checkNotNull(messageBus);
        this.credentials = checkNotNull(credentials);

        this.socket = new SocketIO(checkNotNull(serverURL));
        if (credentials.token() != null) {
            this.socket.addHeader(FLUX_USER_NAME_HEADER_KEY, credentials.username());
            this.socket.addHeader(FLUX_USER_TOKEN_HEADER_KEY, credentials.token());
        }
    }

    /**
     * Open the connection.
     *
     * @return the opened {@link com.codenvy.flux.watcher.core.FluxConnection} instance.
     */
    FluxConnection open() {
        if (!socket.isConnected()) {
            socket.connect(new IOCallback() {
                @Override
                public void onDisconnect() {

                }

                @Override
                public void onConnect() {
                    try {

                        final JSONObject content = new JSONObject().put(CHANNEL, credentials.username());
                        socket.emit(CONNECT_TO_CHANNEL.value(), new IOAcknowledge() {
                            @Override
                            public void ack(Object... objects) {
                                if (objects.length == 1 && objects[0] instanceof JSONObject) {
                                    final JSONObject ack = (JSONObject)objects[0];
                                    try {

                                        if (ack.has(CONNECTED_TO_CHANNEL) && ack.getBoolean(CONNECTED_TO_CHANNEL)) {
                                            return;
                                        }

                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                socket.disconnect();
                            }
                        }, content);

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onMessage(String s, IOAcknowledge ioAcknowledge) {

                }

                @Override
                public void onMessage(JSONObject jsonObject, IOAcknowledge ioAcknowledge) {

                }

                //TODO in flux implementation the username is checked, what to do?
                @Override
                public void on(String name, IOAcknowledge ioAcknowledge, Object... objects) {
                    final FluxMessageType messageType = FluxMessageType.fromType(name);
                    if (messageType != null && objects.length > 0 && objects[0] instanceof JSONObject) {
                        final FluxMessage message = new FluxMessage(FluxConnection.this, messageType, (JSONObject)objects[0]);
                        messageBus.messageReceived(message);
                    }
                }

                @Override
                public void onError(SocketIOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return this;
    }

    /**
     * Close the connection.
     */
    void close() {
        if (socket.isConnected()) {
            socket.disconnect();
        }
    }

    /**
     * Sends a {@link FluxMessage} on this connection
     *
     * @param message
     *         the {@link FluxMessage} instance to send.
     * @throws java.lang.NullPointerException
     *         if {@code message} parameter is {@code null}.
     */
    public void sendMessage(FluxMessage message) {
        checkNotNull(message);

        final JSONObject content = message.getContent();

        try {
            if (!content.has(USERNAME)) {
                content.put(USERNAME, credentials.username());
            }

            if (!content.has(CALLBACK_ID)) {
                if (message.getType() == GET_RESOURCE_REQUEST || message.getType() == GET_PROJECT_REQUEST) {
                    content.put(CALLBACK_ID, messageBus.id());
                }
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        socket.emit(message.getType().value(), message.getContent());
    }
}
