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

import static org.eclipse.flux.watcher.core.FluxMessage.Fields.CALLBACK_ID;
import static org.eclipse.flux.watcher.core.FluxMessageType.GET_PROJECT_RESPONSE;
import static org.eclipse.flux.watcher.core.FluxMessageType.GET_RESOURCE_RESPONSE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static java.util.Collections.emptySet;

import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.json.JSONObject;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

/**
 * Message bus connected to Flux instance.
 *
 * @author Kevin Pollet
 */
@Singleton
public class FluxMessageBus {
    private final int                                id;
    private final ConcurrentMap<URL, FluxConnection> connections;
    private final Provider<Repository>               repository;
    private final Set<FluxMessageHandler>            messageHandlers;

    /**
     * Constructs an instance of {@link com.codenvy.flux.watcher.core.FluxMessageBus}.
     *
     * @param messageHandlers
     *         the {@link FluxMessageHandler} to register.
     * @param repository
     *         the {@link Repository} provider instance.
     * @throws java.lang.NullPointerException
     *         if {@code messageHandlers} is {@code null}.
     */
    @Inject
    FluxMessageBus(Set<FluxMessageHandler> messageHandlers, Provider<Repository> repository) {
        id = Long.valueOf(UUID.randomUUID().getMostSignificantBits()).intValue();
        this.repository = repository;
        this.messageHandlers = new CopyOnWriteArraySet<>(checkNotNull(messageHandlers));
        connections = new ConcurrentHashMap<>();
    }

    /**
     * Returns the {@link com.codenvy.flux.watcher.core.FluxMessageBus} unique id.
     *
     * @return the {@link com.codenvy.flux.watcher.core.FluxMessageBus} unique id.
     */
    public int id() {
        return id;
    }

    /**
     * Open a connection to the given server {@link java.net.URL}.
     *
     * @param serverURL
     *         the server {@link java.net.URL} to connect to.
     * @param credentials
     *         the {@link Credentials} to use for the connection.
     * @return the opened {@link com.codenvy.flux.watcher.core.FluxConnection} or the existing {@link
     * com.codenvy.flux.watcher.core.FluxConnection} if already opened.
     * @throws java.lang.NullPointerException
     *         if {@code serverURL} or {@code credentials} parameter is {@code null}.
     */
    public FluxConnection connect(URL serverURL, Credentials credentials) {
        checkNotNull(serverURL);
        checkNotNull(credentials);

        FluxConnection connection = connections.get(serverURL);
        if (connection == null) {
            FluxConnection newConnection = new FluxConnection(serverURL, credentials, this);
            connection = connections.putIfAbsent(serverURL, newConnection);
            if (connection == null) {
                connection = newConnection.open();
            }
        }

        return connection;
    }

    /**
     * Close the connection to the given server {@link java.net.URL}.
     *
     * @param serverURL
     *         the server {@link java.net.URL} to disconnect from.
     * @throws java.lang.NullPointerException
     *         if {@code serverURL} parameter is {@code null}.
     */
    public void disconnect(URL serverURL) {
        final FluxConnection connection = connections.remove(checkNotNull(serverURL));
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Adds a {@link FluxMessageHandler}.
     *
     * @param handler
     *         the {@link FluxMessageHandler} to add.
     * @return {@code true} if the {@code handler} is not already added, {@code false} otherwise.
     * @throws java.lang.NullPointerException
     *         if {@code handler} parameter is {@code null}.
     */
    public boolean addMessageHandler(FluxMessageHandler handler) {
        return messageHandlers.add(checkNotNull(handler));
    }

    /**
     * Removes a {@link FluxMessageHandler}.
     *
     * @param handler
     *         the {@link FluxMessageHandler} to remove.
     * @return {@code true} if the {@code handler} was already added, {@code false} otherwise.
     * @throws java.lang.NullPointerException
     *         if {@code handler} parameter is {@code null}.
     */
    public boolean removeMessageHandler(FluxMessageHandler handler) {
        return messageHandlers.remove(checkNotNull(handler));
    }

    /**
     * Broadcast messages to all opened {@link com.codenvy.flux.watcher.core.FluxConnection}.
     *
     * @param messages
     *         the {@link FluxMessage} to broadcast.
     * @throws java.lang.NullPointerException
     *         if {@code messages} parameter is {@code null}.
     */
    public void sendMessages(FluxMessage... messages) {
        checkNotNull(messages);

        for (FluxMessage oneMessage : messages) {
            checkNotNull(oneMessage);

            for (FluxConnection oneConnection : connections.values()) {
                oneConnection.sendMessage(oneMessage);
            }
        }
    }

    /**
     * Fires the given {@link FluxMessage} to all {@link FluxMessageHandler}
     * registered.
     *
     * @param message
     *         the message.
     * @throws java.lang.NullPointerException
     *         if {@code message} parameter is {@code null}.
     */
    public void messageReceived(FluxMessage message) {
        checkNotNull(message);

        if (message.getType() == GET_RESOURCE_RESPONSE || message.getType() == GET_PROJECT_RESPONSE) {
            final JSONObject content = message.getContent();
            if (content.optInt(CALLBACK_ID) != id) {
                return;
            }
        }

        final Set<FluxMessageHandler> messageHandlers = getMessageHandlersFor(message.getType().value());
        for (FluxMessageHandler oneMessageHandler : messageHandlers) {
            try {

                oneMessageHandler.onMessage(message, repository.get());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Set<FluxMessageHandler> getMessageHandlersFor(final String messageType) {
        return ImmutableSet.copyOf(FluentIterable.from(messageHandlers)
                             .filter(notNull())
                             .filter(new Predicate<FluxMessageHandler>() {
                                 @Override
                                 public boolean apply(FluxMessageHandler messageHandler) {
                                     final Set<String> supportedTypes = getMessageTypesFor(messageHandler);
                                     return supportedTypes.contains(messageType);
                                 }
                             }));
    }

    private Set<String> getMessageTypesFor(FluxMessageHandler messageHandler) {
        final FluxMessageTypes types = messageHandler.getClass().getAnnotation(FluxMessageTypes.class);
        if (types == null) {
            return emptySet();
        }

        return ImmutableSet.copyOf(FluentIterable.from(Arrays.asList(types.value()))
                             .filter(Predicates.notNull())
                             .transform(new Function<FluxMessageType, String>() {
                                 @Override
                                 public String apply(FluxMessageType messageType) {
                                     return messageType.value();
                                 }
                             }));
    }
}
