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
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Singleton;

import static org.eclipse.flux.watcher.core.FluxMessage.Fields.CALLBACK_ID;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.FILES;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.HASH;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.PATH;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.PROJECT;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.REQUEST_SENDER_ID;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.TIMESTAMP;
import static org.eclipse.flux.watcher.core.FluxMessage.Fields.TYPE;
import static org.eclipse.flux.watcher.core.FluxMessageType.GET_PROJECT_REQUEST;
import static org.eclipse.flux.watcher.core.FluxMessageType.GET_PROJECT_RESPONSE;

/**
 * Handler replying to a {@link com.codenvy.flux.watcher.core.FluxMessageType#GET_PROJECT_REQUEST}.
 *
 * @author Kevin Pollet
 */
@Singleton
@FluxMessageTypes(GET_PROJECT_REQUEST)
public final class GetProjectRequestHandler implements FluxMessageHandler {
    @Override
    public void onMessage(FluxMessage message, Repository repository) throws JSONException {
        final JSONObject request = message.content();
        final int callbackId = request.getInt(CALLBACK_ID.value());
        final String requestSenderId = request.getString(REQUEST_SENDER_ID.value());
        final String projectName = request.getString(PROJECT.value());

        final Project project = repository.getProject(projectName);
        if (project != null) {
            final JSONArray files = new JSONArray();
            for (Resource oneResource : project.getResources()) {
                files.put(new JSONObject()
                                  .put(PATH.value(), oneResource.path())
                                  .put(TIMESTAMP.value(), oneResource.timestamp())
                                  .put(HASH.value(), oneResource.hash())
                                  .put(TYPE.value(), oneResource.type().name().toLowerCase()));
            }

            final JSONObject content = new JSONObject()
                    .put(CALLBACK_ID.value(), callbackId)
                    .put(REQUEST_SENDER_ID.value(), requestSenderId)
                    .put(PROJECT.value(), projectName)
                    .put(FILES.value(), files);

            message.source()
                   .sendMessage(new FluxMessage(GET_PROJECT_RESPONSE, content));
        }
    }
}
