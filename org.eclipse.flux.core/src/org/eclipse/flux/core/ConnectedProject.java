/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.core;

import java.io.InputStream;
import org.eclipse.core.resources.IProject;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;

/**
 * @author Martin Lippert
 */
public class ConnectedProject {

    private String name;
    private Project project;

    public ConnectedProject(Project project) {
        this.project = project;
        this.name = project.id();
    }

    public IProject getProject() {
        return null;
    }
    
    public String getName() {
        return this.project.id();
    }
    
    public static ConnectedProject readFromJSON(InputStream inputStream, IProject project) {
        return null;
    }

    public long getTimestamp(String resourcePath) {
        Resource resource = this.project.getResource(resourcePath);
        return resource.timestamp();
    }

    public String getHash(String resourcePath) {
        Resource resource = this.project.getResource(resourcePath);
        return resource.hash();
    }

    public boolean containsResource(String resourcePath) {
        return this.project.hasResource(resourcePath);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ConnectedProject)) {
            return false;
        }
        ConnectedProject other = (ConnectedProject)obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
