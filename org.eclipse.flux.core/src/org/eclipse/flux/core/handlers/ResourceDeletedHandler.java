package org.eclipse.flux.core.handlers;

import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONObject;

public class ResourceDeletedHandler extends AbstractFluxMessageHandler {

    public ResourceDeletedHandler(MessageConnector messageConnector, Repository repository) {
        super(messageConnector, repository, RESOURCE_DELETED);
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String projectName = message.getString(MessageConstants.PROJECT_NAME);
        String resourcePath = message.getString(MessageConstants.RESOURCE);
        long resourceTimestamp = message.getLong(MessageConstants.TIMESTAMP);
        Project project = repository.getProject(projectName);
        if(project == null)
            return;
        Resource localResource = project.getResource(resourcePath);
        if(localResource == null)
            return;
        boolean isLocalResourceOutdated = localResource.timestamp() < resourceTimestamp;
        if (isLocalResourceOutdated) {
            project.deleteResource(Resource.newUnknown(resourcePath, resourceTimestamp));
        }
    }

}
