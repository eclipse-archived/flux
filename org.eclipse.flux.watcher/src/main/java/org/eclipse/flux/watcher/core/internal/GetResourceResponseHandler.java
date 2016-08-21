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

import static org.eclipse.flux.watcher.core.FluxMessage.Fields.CONTENT;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.HASH;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.PROJECT;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.RESOURCE;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.TIMESTAMP;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.TYPE;
import static org.eclipse.flux.watcher.core.FluxMessageType.GET_RESOURCE_RESPONSE;
import static org.eclipse.flux.watcher.core.FluxMessageType.RESOURCE_STORED;
import static org.eclipse.flux.watcher.core.Resource.ResourceType;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.FILE;

/**
 * Handler replying to a {@link com.codenvy.flux.watcher.core.FluxMessageType#GET_RESOURCE_RESPONSE}.
 *
 * @author Kevin Pollet
 */
@FluxMessageTypes(GET_RESOURCE_RESPONSE)
public final class GetResourceResponseHandler implements FluxMessageHandler {
    @Override
    public void onMessage(FluxMessage message, Repository repository) throws JSONException {
        final JSONObject request = message.getContent();
        final String projectName = request.getString(PROJECT);
        final String resourcePath = request.getString(RESOURCE);
        final long resourceTimestamp = request.getLong(TIMESTAMP);
        final String resourceHash = request.getString(HASH);
        final String resourceContent = request.getString(CONTENT);

        final Project project = repository.getProject(projectName);
        if (project != null) {
            final ResourceType resourceType = ResourceType.valueOf(request.getString(TYPE).toUpperCase());

            if (resourceType == FILE) {
                boolean isResourceStored = false;
                final Resource localResource = project.getResource(resourcePath);
                final Resource resource = Resource.newFile(resourcePath, resourceTimestamp, resourceContent.getBytes());

                if (localResource == null) {
                    project.createResource(resource);
                    isResourceStored = true;

                } else if (!localResource.hash().equals(resourceHash) && localResource.timestamp() < resourceTimestamp) {
                    project.updateResource(resource);
                    isResourceStored = true;
                }

                if (isResourceStored) {
                    final JSONObject content = new JSONObject()
                            .put(PROJECT, projectName)
                            .put(RESOURCE, resourcePath)
                            .put(TIMESTAMP, resourceTimestamp)
                            .put(HASH, resourceHash)
                            .put(TYPE, resourceType.name().toLowerCase());

                    message.getSource()
                           .sendMessage(new FluxMessage(RESOURCE_STORED, content));
                }
            }
        }
    }
}
