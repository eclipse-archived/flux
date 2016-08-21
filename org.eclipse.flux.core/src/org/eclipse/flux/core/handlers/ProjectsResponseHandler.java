package org.eclipse.flux.core.handlers;

import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProjectsResponseHandler extends AbstractFluxMessageHandler {

    public ProjectsResponseHandler(MessageConnector messageConnector, Repository repository) {
        super(messageConnector, repository, "getProjectsRequest");
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        int callbackID = message.optInt(MessageConstants.CALLBACK_ID);
        String requestSenderId = message.getString(MessageConstants.REQUEST_SENDER_ID);
        String username = message.getString(MessageConstants.USERNAME);
        
        JSONArray projects = new JSONArray();
        for (Project fluxProject : repository.getSynchronizedProjects()) {
            JSONObject project = new JSONObject();
            project.put("name", fluxProject.id());
            projects.put(project);
        }

        JSONObject content = new JSONObject();
        content.put("callback_id", callbackID);
        content.put("requestSenderID", requestSenderId);
        content.put("username", username);
        content.put("projects", projects);

        messageConnector.send("getProjectsResponse", content);
    }
}
