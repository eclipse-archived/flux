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
package org.eclipse.flux.watcher.core.spi;

/**
 * Project factory contract implemented by a provider.
 *
 * @author Kevin Pollet
 */
public interface ProjectFactory {
    /**
     * Returns a new {@link com.codenvy.flux.watcher.core.spi.Project} instance for the given project id and path.
     *
     * @param projectId
     *         the project id.
     * @param projectPath
     *         the project path.
     * @return the new {@link com.codenvy.flux.watcher.core.spi.Project} instance, never {@code null}.
     * @throws java.lang.NullPointerException
     *         if {@code projectId} or {@code projectPath} parameter is {@code null}.
     * @throws java.lang.IllegalArgumentException
     *         if {@code projectPath} parameter is not absolute, doesn't exist or is not a folder.
     */
    Project newProject(String projectId, String projectPath);
}
