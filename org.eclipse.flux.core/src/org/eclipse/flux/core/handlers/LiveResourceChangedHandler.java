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
@FluxMessageTypes(FluxMessageType.LIVE_RESOURCE_CHANGED)
public class LiveResourceChangedHandler implements FluxMessageHandler {

    private Collection<ILiveEditConnector> liveEditConnectors;

    public LiveResourceChangedHandler(Collection<ILiveEditConnector> liveEditConnectors){
        this.liveEditConnectors = liveEditConnectors;
    }

    @Override
    public void onMessage(FluxMessage message, Repository repository) throws Exception {
        JSONObject content = message.getContent();
        String username = content.getString(FluxMessage.Fields.USERNAME);
        String projectName = content.getString(FluxMessage.Fields.PROJECT);
        String resourcePath = content.getString(FluxMessage.Fields.RESOURCE);
        int offset = content.getInt(FluxMessage.Fields.OFFSET);
        int removedCharCount = content.getInt(FluxMessage.Fields.REMOVED_CHAR_COUNT);
        String addedChars = JSONUtils.getString(content, FluxMessage.Fields.ADDED_CHARACTERS, "");
        String liveEditID = projectName + "/" + resourcePath;

        for (ILiveEditConnector connector : liveEditConnectors) {
            connector.liveEditingEvent(username, liveEditID, offset, removedCharCount, addedChars);
        }
    }

}
