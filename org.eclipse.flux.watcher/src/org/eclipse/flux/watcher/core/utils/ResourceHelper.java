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
package org.eclipse.flux.watcher.core.utils;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper providing methods to work with {@link com.codenvy.flux.watcher.core.Resource}.
 *
 * @author Kevin Pollet
 */
public final class ResourceHelper {
    private static final MessageDigest messageDigest;

    static {
        try {

            messageDigest = MessageDigest.getInstance("SHA-1");

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculates the sha1 for the given {@link java.lang.Byte} array.
     *
     * @param bytes
     *         the {@link java.lang.Byte} array.
     * @return the sha1 as an hexadecimal {@link String}, never {@code null}.
     * @throws java.lang.NullPointerException
     *         if {@code bytes} parameter is {@code null}.
     */
    public static String sha1(byte[] bytes) {
        final byte[] digest = messageDigest.digest(checkNotNull(bytes));
        return DatatypeConverter.printHexBinary(digest);
    }

    /**
     * Disable instantiation.
     */
    private ResourceHelper() {
    }
}
