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

import static org.eclipse.flux.watcher.core.FluxMessage.Fields.HASH;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.PROJECT;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.RESOURCE;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.TIMESTAMP;
import static org.eclipse.flux.watcher.core.FluxMessageType.GET_RESOURCE_REQUEST;
import static org.eclipse.flux.watcher.core.FluxMessageType.RESOURCE_CHANGED;

/**
 * Handler replying to a {@link com.codenvy.flux.watcher.core.FluxMessageType#RESOURCE_CHANGED}.
 *
 * @author Kevin Pollet
 */
@Singleton
@FluxMessageTypes(RESOURCE_CHANGED)
public final class ResourceChangedHandler implements FluxMessageHandler {
    @Override
    public void onMessage(FluxMessage message, Repository repository) throws JSONException {
        final JSONObject request = message.getContent();
        final String projectName = request.getString(PROJECT);
        final String resourcePath = request.getString(RESOURCE);
        final long resourceTimestamp = request.getLong(TIMESTAMP);
        final String resourceHash = request.getString(HASH);

        final Project project = repository.getProject(projectName);
        if (project != null) {
            final Resource localResource = project.getResource(resourcePath);

            if (localResource != null
                && !localResource.hash().equals(resourceHash)
                && localResource.timestamp() < resourceTimestamp) {

                final JSONObject content = new JSONObject()
                        .put(PROJECT, projectName)
                        .put(RESOURCE, resourcePath)
                        .put(TIMESTAMP, resourceTimestamp)
                        .put(HASH, resourceHash);

                message.getSource()
                       .sendMessage(new FluxMessage(GET_RESOURCE_REQUEST, content));
            }
        }
    }
}
