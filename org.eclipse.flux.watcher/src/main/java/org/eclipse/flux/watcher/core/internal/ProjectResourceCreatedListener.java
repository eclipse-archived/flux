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
package org.eclipse.flux.watcher.core.internal;

import org.eclipse.flux.watcher.core.FluxMessage;
import org.eclipse.flux.watcher.core.FluxMessageBus;
import org.eclipse.flux.watcher.core.RepositoryEvent;
import org.eclipse.flux.watcher.core.RepositoryEventTypes;
import org.eclipse.flux.watcher.core.RepositoryListener;

import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.eclipse.flux.watcher.core.FluxMessage.Fields.HASH;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.PROJECT;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.RESOURCE;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.TIMESTAMP;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.TYPE;
import static org.eclipse.flux.watcher.core.FluxMessageType.RESOURCE_CREATED;
import static org.eclipse.flux.watcher.core.FluxMessageType.RESOURCE_STORED;
import static org.eclipse.flux.watcher.core.RepositoryEventType.PROJECT_RESOURCE_CREATED;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listener sending a message to flux connections when a project resource is created in the repository.
 *
 * @author Kevin Pollet
 */
@Singleton
//@RepositoryEventTypes(PROJECT_RESOURCE_CREATED)
public final class ProjectResourceCreatedListener implements RepositoryListener {
    private final FluxMessageBus messageBus;

    /**
     * Constructs an instance of {@code ProjectResourceCreatedListener}.
     *
     * @param messageBus
     *         the {@link com.codenvy.flux.watcher.core.FluxMessageBus}.
     * @throws java.lang.NullPointerException
     *         if {@code messageBus} parameter is {@code null}.
     */
    @Inject
    ProjectResourceCreatedListener(FluxMessageBus messageBus) {
        this.messageBus = checkNotNull(messageBus);
    }

    @Override
    public void onEvent(RepositoryEvent event) throws JSONException {
        JSONObject content = new JSONObject();
        content.put(PROJECT, event.project().id());
        content.put(RESOURCE, event.resource().path());
        content.put(TIMESTAMP, event.resource().timestamp());
        content.put(HASH, event.resource().hash());
        content.put(TYPE, event.resource().type().name().toLowerCase());

        messageBus.sendMessages(new FluxMessage(RESOURCE_CREATED, content), new FluxMessage(RESOURCE_STORED, content));
    }
}
