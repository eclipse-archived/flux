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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Credentials used to connect to a Flux server.
 *
 * @author Kevin Pollet
 */
public class Credentials {
    public static final Credentials DEFAULT_USER_CREDENTIALS;
    private static final String DEFAULT_USER_USERNAME = "defaultuser";

    static {
        DEFAULT_USER_CREDENTIALS = new Credentials(DEFAULT_USER_USERNAME);
    }

    private final String username;
    private final String token;

    /**
     * Constructs an instance of {@link com.codenvy.flux.watcher.core.Credentials}.
     *
     * @param username
     *         the username.
     * @throws java.lang.NullPointerException
     *         if {@code username} parameter is {@code null}.
     */
    public Credentials(String username) {
        this(username, null);
    }

    /**
     * Constructs an instance of {@link com.codenvy.flux.watcher.core.Credentials}.
     *
     * @param username
     *         the username.
     * @param token
     *         the user token.
     * @throws java.lang.NullPointerException
     *         if {@code username} parameter is {@code null}.
     */
    public Credentials(String username, String token) {
        this.username = checkNotNull(username);
        this.token = token;
    }

    /**
     * Returns the username.
     *
     * @return the username never {@code null}.
     */
    public String username() {
        return username;
    }

    /**
     * Returns the user token.
     *
     * @return the user token or {@code null} if none.
     */
    public String token() {
        return token;
    }
}
