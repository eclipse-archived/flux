package org.eclipse.flux.core.handlers;

import java.util.Collection;

import org.eclipse.flux.core.ILiveEditConnector;
import org.eclipse.flux.core.util.JSONUtils;
import org.eclipse.flux.watcher.core.FluxMessage;
import org.eclipse.flux.watcher.core.FluxMessageHandler;
import org.eclipse.flux.watcher.core.FluxMessageType;
import org.eclipse.flux.watcher.core.FluxMessageTypes;
import org.eclipse.flux.watcher.core.Repository;
import org.json.JSONObject;

import com.google.inject.Singleton;

@Singleton
@FluxMessageTypes({ FluxMessageType.GET_LIVE_RESOURCE_REQUEST })
public class LiveResourceRequestHandler implements FluxMessageHandler {

    private Collection<ILiveEditConnector> liveEditConnectors;

    public LiveResourceRequestHandler(Collection<ILiveEditConnector> liveEditConnectors) {
        this.liveEditConnectors = liveEditConnectors;
    }

    @Override
    public void onMessage(FluxMessage message, Repository repository) throws Exception {
        JSONObject content = message.getContent();
        String username = JSONUtils.getString(content, FluxMessage.Fields.USERNAME, null);
        String requestSenderID = content.getString(FluxMessage.Fields.REQUEST_SENDER_ID);
        String projectRegEx = JSONUtils.getString(content, FluxMessage.Fields.PROJECT_REG_EX, null);
        String resourceRegEx = JSONUtils.getString(content, FluxMessage.Fields.RESOURCE_REG_EX, null);
        int callbackID = content.getInt(FluxMessage.Fields.CALLBACK_ID);
        
        for (ILiveEditConnector connector : liveEditConnectors) {
            connector.liveEditors(requestSenderID, callbackID, username, projectRegEx, resourceRegEx);
        }
    }

}
