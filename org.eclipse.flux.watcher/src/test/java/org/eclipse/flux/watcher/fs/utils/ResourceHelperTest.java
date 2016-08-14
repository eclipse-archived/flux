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
package org.eclipse.flux.watcher.fs.utils;


import org.eclipse.flux.watcher.core.utils.ResourceHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Kevin Pollet
 */
public final class ResourceHelperTest {
    @Test(expected = NullPointerException.class)
    public void testSHA1WithNullBytes() {
        ResourceHelper.sha1(null);
    }

    @Test
    public void testSHA1() {
        final byte[] hello = "hello".getBytes();
        final byte[] helloWorld = "helloWorld".getBytes();

        final String helloHash = ResourceHelper.sha1(hello);
        final String helloWorldHash = ResourceHelper.sha1(helloWorld);

        Assert.assertNotNull(helloHash);
        Assert.assertNotNull(helloWorldHash);
        Assert.assertNotEquals(helloHash, helloWorldHash);
    }
}
