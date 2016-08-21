package org.eclipse.flux.core.handlers;

import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProjectRequestHandler extends AbstractFluxMessageHandler {

    public ProjectRequestHandler(MessageConnector messageConnector, Repository repository) {
        super(messageConnector, repository, GET_PROJECT_REQUEST);
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        Project project = repository.getProject(message.getString(MessageConstants.PROJECT_NAME));
        if(project == null)
            return;
        JSONArray files = new JSONArray();
        for(Resource resource : project.getResources()){
            JSONObject file = new JSONObject();
            file.put(MessageConstants.PATH, resource.path());
            file.put(MessageConstants.TIMESTAMP, resource.timestamp());
            file.put(MessageConstants.HASH, resource.hash());
            file.put(MessageConstants.TYPE, resource.type().name().toLowerCase());
            files.put(file);
        }
        message.put(MessageConstants.FILES, files);
        messageConnector.send(GET_PROJECT_RESPONSE, message);
    }
}