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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation used to indicate which {@link com.codenvy.flux.watcher.core.FluxMessageType} the {@link
 * com.codenvy.flux.watcher.core.FluxMessageHandler} can handle.
 *
 * @author Kevin Pollet
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface FluxMessageTypes {
    /**
     * Returns the {@link com.codenvy.flux.watcher.core.FluxMessageType}  the {@link
     * com.codenvy.flux.watcher.core.FluxMessageHandler} can handle.
     *
     * @return the {@link com.codenvy.flux.watcher.core.FluxMessageType}  the {@link
     * com.codenvy.flux.watcher.core.FluxMessageHandler} can handle.
     */
    FluxMessageType[] value();
}
