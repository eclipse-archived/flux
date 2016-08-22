package org.eclipse.flux.core;

import java.util.Set;

import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONObject;

public interface IRepositoryCallback {
    void notifyResourceChanged(Resource resource, Project project);
    void sendMessage(String messageType, JSONObject content) throws Exception;
    Project getWatcherProject(String projectName);
    Set<Project> getSynchronizedProjects();
}