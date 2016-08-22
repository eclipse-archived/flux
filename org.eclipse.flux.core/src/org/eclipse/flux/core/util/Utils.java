package org.eclipse.flux.core.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

public class Utils {
    public static IResource getResourceByPath(String projectName, String resourcePath){
        Path path = new Path(projectName + "/" + resourcePath);
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
        return file;
    }
}
