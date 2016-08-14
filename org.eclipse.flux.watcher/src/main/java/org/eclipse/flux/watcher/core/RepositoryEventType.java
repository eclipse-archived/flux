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
package org.eclipse.flux.watcher.core;

/**
 * Type of events sent by a {@link com.codenvy.flux.watcher.core.Repository}.
 *
 * @author Kevin Pollet
 */
public enum RepositoryEventType {
    PROJECT_RESOURCE_CREATED,
    PROJECT_RESOURCE_MODIFIED,
    PROJECT_RESOURCE_DELETED
}
