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

import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Singleton;
import java.util.Objects;

import static org.eclipse.flux.watcher.core.FluxMessage.Fields.DELETED;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.FILES;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.HASH;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.PATH;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.PROJECT;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.RESOURCE;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.TIMESTAMP;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.TYPE;
import static org.eclipse.flux.watcher.core.FluxMessageType.GET_PROJECT_RESPONSE;
import static org.eclipse.flux.watcher.core.FluxMessageType.GET_RESOURCE_REQUEST;
import static org.eclipse.flux.watcher.core.Resource.ResourceType;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.FILE;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.FOLDER;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.UNKNOWN;

/**
 * Handler replying to a {@link com.codenvy.flux.watcher.core.FluxMessageType#GET_PROJECT_RESPONSE}.
 *
 * @author Kevin Pollet
 */
@Singleton
@FluxMessageTypes(GET_PROJECT_RESPONSE)
public final class GetProjectResponseHandler implements FluxMessageHandler {
    @Override
    public void onMessage(FluxMessage message, Repository repository) throws Exception {
        final JSONObject request = message.content();
        final String projectName = request.getString(PROJECT.value());
        final JSONArray files = request.getJSONArray(FILES.value());
        final JSONArray deleted = request.optJSONArray(DELETED.value());

        final Project project = repository.getProject(projectName);
        if (project != null) {
            // new files
            for (int i = 0; i < files.length(); i++) {
                final JSONObject resource = files.getJSONObject(i);
                final String resourcePath = resource.getString(PATH.value());
                final long resourceTimestamp = resource.getLong(TIMESTAMP.value());
                final ResourceType resourceType = ResourceType.valueOf(resource.optString(TYPE.value(), UNKNOWN.name()).toUpperCase());
                final String resourceHash = resource.optString(HASH.value(), "0");

                final Resource localResource = project.getResource(resourcePath);
                if (resourceType == FILE) {
                    if (localResource == null
                        || localResource.timestamp() < resourceTimestamp
                           && !Objects.equals(resourceHash, localResource.hash())) {

                        final JSONObject content = new JSONObject()
                                .put(PROJECT.value(), projectName)
                                .put(RESOURCE.value(), resourcePath)
                                .put(TIMESTAMP.value(), resourceTimestamp)
                                .put(HASH.value(), resourceHash);

                        message.source()
                               .sendMessage(new FluxMessage(GET_RESOURCE_REQUEST, content));
                    }

                } else if (resourceType == FOLDER && localResource == null) {
                    project.createResource(Resource.newFolder(resourcePath, resourceTimestamp));
                }
            }

            // deleted files
            if (deleted != null) {
                for (int i = 0; i < deleted.length(); i++) {
                    final JSONObject resource = deleted.getJSONObject(i);
                    final String resourcePath = resource.getString(PATH.value());
                    final long resourceTimestamp = resource.getLong(TIMESTAMP.value());

                    final Resource localResource = project.getResource(resourcePath);
                    if (localResource != null && localResource.timestamp() < resourceTimestamp) {
                        project.deleteResource(Resource.newUnknown(resourcePath, resourceTimestamp));
                    }
                }
            }
        }
    }
}
