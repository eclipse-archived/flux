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


import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.eclipse.flux.watcher.core.Resource.ResourceType.FILE;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.FOLDER;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.UNKNOWN;

/**
 * {@link com.codenvy.flux.watcher.core.Resource} tests.
 *
 * @author Kevin Pollet
 */
public final class ResourceTest {
    private final static String RESOURCE_PATH = "/foo";

    @Test(expected = NullPointerException.class)
    public void testNewUnknownWithNullPath() {
        Resource.newUnknown(null, System.currentTimeMillis());
    }

    @Test
    public void testNewUnknown() {
        final long timestamp = System.currentTimeMillis();
        final Resource resource = Resource.newUnknown(RESOURCE_PATH, timestamp);

        Assert.assertNotNull(resource);
        Assert.assertEquals(timestamp, resource.timestamp());
        Assert.assertEquals(RESOURCE_PATH, resource.path());
        Assert.assertEquals(null, resource.content());
        Assert.assertEquals(UNKNOWN, resource.type());
        Assert.assertEquals("0", resource.hash());
    }

    @Test(expected = NullPointerException.class)
    public void testNewFolderWithNullPath() {
        Resource.newFolder(null, System.currentTimeMillis());
    }

    @Test
    public void testNewFolder() {
        final long timestamp = System.currentTimeMillis();
        final Resource resource = Resource.newFolder(RESOURCE_PATH, timestamp);

        Assert.assertNotNull(resource);
        Assert.assertEquals(timestamp, resource.timestamp());
        Assert.assertEquals(RESOURCE_PATH, resource.path());
        Assert.assertEquals(null, resource.content());
        Assert.assertEquals(FOLDER, resource.type());
        Assert.assertEquals("0", resource.hash());
    }

    @Test(expected = NullPointerException.class)
    public void testNewFileWithNullPath() {
        Resource.newFile(null, System.currentTimeMillis(), new byte[0]);
    }

    @Test
    public void testNewFile() {
        final byte[] content = "content".getBytes();
        final long timestamp = System.currentTimeMillis();
        final Resource resource = Resource.newFile(RESOURCE_PATH, timestamp, content);

        Assert.assertNotNull(resource);
        Assert.assertEquals(timestamp, resource.timestamp());
        Assert.assertEquals(RESOURCE_PATH, resource.path());
        Assert.assertTrue(Arrays.equals(content, resource.content()));
        Assert.assertEquals(FILE, resource.type());
        Assert.assertNotNull(resource.hash());
    }
}
