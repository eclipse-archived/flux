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
import org.eclipse.flux.watcher.core.FluxMessageHandler;
import org.eclipse.flux.watcher.core.FluxMessageTypes;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;

import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Singleton;

import static org.eclipse.flux.watcher.core.FluxMessage.Fields.CALLBACK_ID;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.CONTENT;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.HASH;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.PROJECT;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.REQUEST_SENDER_ID;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.RESOURCE;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.TIMESTAMP;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.TYPE;
import static org.eclipse.flux.watcher.core.FluxMessageType.GET_RESOURCE_REQUEST;
import static org.eclipse.flux.watcher.core.FluxMessageType.GET_RESOURCE_RESPONSE;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.FILE;

/**
 * Handler replying to a {@link com.codenvy.flux.watcher.core.FluxMessageType#GET_RESOURCE_REQUEST}.
 *
 * @author Kevin Pollet
 */
@Singleton
@FluxMessageTypes(GET_RESOURCE_REQUEST)
public final class GetResourceRequestHandler implements FluxMessageHandler {
    @Override
    public void onMessage(FluxMessage message, Repository repository) throws JSONException {
        final JSONObject request = message.content();
        final int callbackId = request.getInt(CALLBACK_ID.value());
        final String requestSenderId = request.getString(REQUEST_SENDER_ID.value());
        final String projectName = request.getString(PROJECT.value());
        final String resourcePath = request.getString(RESOURCE.value());

        final Project project = repository.getProject(projectName);
        if (project != null) {
            final Resource resource = project.getResource(resourcePath);

            // compare the time stamp and allow a nearly difference for now TODO find out why CodenvyVFS -> Eclipse has different timestamp
            long requestTimeStamp = request.has(TIMESTAMP.value()) ? request.getLong(TIMESTAMP.value()) : 0;
            long resourceTimeStamp = resource.timestamp();
            if (!request.has(TIMESTAMP.value()) || Math.abs(requestTimeStamp - resourceTimeStamp) < 10000) {
                final JSONObject content = new JSONObject()
                                                           .put(CALLBACK_ID.value(), callbackId)
                                                           .put(REQUEST_SENDER_ID.value(), requestSenderId)
                                                           .put(PROJECT.value(), projectName)
                                                           .put(RESOURCE.value(), resourcePath)
                                                           .put(TIMESTAMP.value(), resourceTimeStamp)
                                                           .put(HASH.value(), resource.hash())
                                                           .put(TYPE.value(), resource.type().name().toLowerCase());

                if (resource.type() == FILE) {
                    content.put(CONTENT.value(), new String(resource.content()));
                }

                message.source()
                       .sendMessage(new FluxMessage(GET_RESOURCE_RESPONSE, content));
            }
        }
    }
}
