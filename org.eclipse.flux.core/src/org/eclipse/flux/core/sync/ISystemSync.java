package org.eclipse.flux.core.sync;

import java.util.Set;

import org.eclipse.flux.watcher.core.RepositoryListener;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONObject;

public interface ISystemSync extends RepositoryListener {
    void sendMessage(String messageType, JSONObject content) throws Exception;
    Project getWatcherProject(String projectName);
    Set<Project> getSynchronizedProjects();
    String getUsername();
}