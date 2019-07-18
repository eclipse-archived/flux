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
package org.eclipse.flux.watcher.fs;


import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import com.google.common.collect.Sets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Set;

import static org.eclipse.flux.watcher.core.Resource.ResourceType.FILE;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.FOLDER;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link com.codenvy.flux.watcher.fs.JDKProject} tests.
 *
 * @author Kevin Pollet
 */
public final class JDKProjectTest extends AbstractTest {
    private JDKProject             project;
    private JDKProjectWatchService jdkProjectWatchServiceMock;

    @Before
    public void beforeTest() throws IOException {
        jdkProjectWatchServiceMock = mock(JDKProjectWatchService.class);
        project = new JDKProject(fileSystem(), jdkProjectWatchServiceMock, PROJECT_ID, PROJECT_PATH);
    }

    @Test
    public void testSetSynchronizedWithTrue() {
        project.setSynchronized(true);
        verify(jdkProjectWatchServiceMock, times(1)).watch(any(Project.class));
        verify(jdkProjectWatchServiceMock, times(0)).unwatch(any(Project.class));
    }

    @Test
    public void testSetSynchronizedWithFalse() {
        project.setSynchronized(false);
        verify(jdkProjectWatchServiceMock, times(0)).watch(any(Project.class));
        verify(jdkProjectWatchServiceMock, times(1)).unwatch(any(Project.class));
    }

    @Test
    public void testGetResources() {
        final Set<Resource> resources = project.getResources();
        final Set<String> paths = Sets.newHashSet(RELATIVE_PROJECT_SRC_FOLDER_PATH, RELATIVE_PROJECT_README_FILE_PATH);

        Assert.assertNotNull(resources);
        Assert.assertEquals(2, resources.size());

        for (Resource oneResource : resources) {
            if (paths.contains(oneResource.path())) {
                paths.remove(oneResource.path());
            }
        }

        Assert.assertTrue(paths.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testGetResourceWithNullResourcePath() {
        project.getResource(null);
    }

    @Test
    public void testGetResourceWithNonExistentResourcePath() {
        final Resource resource = project.getResource("foo");

        Assert.assertNull(resource);
    }

    @Test
    public void testGetResourceWithFilePath() throws IOException {
        final Path absoluteResourcePath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_README_FILE_PATH);
        final Resource resource = project.getResource(RELATIVE_PROJECT_README_FILE_PATH);

        Assert.assertNotNull(resource);
        Assert.assertEquals(RELATIVE_PROJECT_README_FILE_PATH, resource.path());
        Assert.assertEquals(FILE, resource.type());
        Assert.assertEquals(getLastModifiedTime(absoluteResourcePath).toMillis(), resource.timestamp());
        Assert.assertArrayEquals(readAllBytes(absoluteResourcePath), resource.content());
        Assert.assertNotNull(resource.hash());
    }

    @Test
    public void testGetResourceWithFolderPath() throws IOException {
        final Path absoluteFolderPath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_SRC_FOLDER_PATH);
        final Resource resource = project.getResource(RELATIVE_PROJECT_SRC_FOLDER_PATH);

        Assert.assertNotNull(resource);
        Assert.assertEquals(RELATIVE_PROJECT_SRC_FOLDER_PATH, resource.path());
        Assert.assertEquals(FOLDER, resource.type());
        Assert.assertEquals(getLastModifiedTime(absoluteFolderPath).toMillis(), resource.timestamp());
        Assert.assertEquals(null, resource.content());
        Assert.assertNotNull(resource.hash());
    }

    @Test(expected = NullPointerException.class)
    public void testCreateResourceWithNullResource() {
        project.createResource(null);
    }

    @Test
    public void testCreateResourceWithFolderResource() throws IOException {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 26);
        calendar.set(Calendar.MONTH, 8);
        calendar.set(Calendar.YEAR, 1984);

        final Path absoluteFolderPath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_MAIN_FOLDER_PATH);
        project.createResource(Resource.newFolder(RELATIVE_PROJECT_MAIN_FOLDER_PATH, calendar.getTimeInMillis()));

        Assert.assertTrue(exists(absoluteFolderPath));
        Assert.assertTrue(isDirectory(absoluteFolderPath));
        Assert.assertEquals(calendar.getTimeInMillis(), getLastModifiedTime(absoluteFolderPath).toMillis());
    }

    @Test
    public void testCreateResourceWithFileResource() throws IOException {
        final byte[] helloFileContent = "helloWorld".getBytes();
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 26);
        calendar.set(Calendar.MONTH, 8);
        calendar.set(Calendar.YEAR, 1984);

        final Path absoluteFilePath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_HELLO_FILE_PATH);
        project.createResource(Resource.newFile(RELATIVE_PROJECT_HELLO_FILE_PATH, calendar.getTimeInMillis(), helloFileContent));

        Assert.assertTrue(exists(absoluteFilePath));
        Assert.assertFalse(isDirectory(absoluteFilePath));
        Assert.assertArrayEquals(readAllBytes(absoluteFilePath), helloFileContent);
        Assert.assertEquals(calendar.getTimeInMillis(), getLastModifiedTime(absoluteFilePath).toMillis());
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateResourceWithNullResource() {
        project.updateResource(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateResourceWithFolderResource() {
        project.updateResource(Resource.newFolder(RELATIVE_PROJECT_MAIN_FOLDER_PATH, System.currentTimeMillis()));
    }

    @Test
    public void testUpdateResource() throws IOException {
        final byte[] readmeContent = "readme".getBytes();
        final long timestamp = System.currentTimeMillis();
        final Path resourcePath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_README_FILE_PATH);
        final Resource resource = Resource.newFile(RELATIVE_PROJECT_README_FILE_PATH, timestamp, readmeContent);

        Assert.assertArrayEquals(new byte[0], Files.readAllBytes(resourcePath));

        project.updateResource(resource);

        Assert.assertEquals(timestamp, getLastModifiedTime(resourcePath).toMillis());
        Assert.assertArrayEquals(readmeContent, readAllBytes(resourcePath));
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteResourceWithNullResource() {
        project.deleteResource(null);
    }

    @Test
    public void testDeleteResourceWithEmptyFolderResource() throws IOException {
        final Path absoluteFolderPath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_SRC_FOLDER_PATH);
        project.deleteResource(Resource.newFolder(RELATIVE_PROJECT_SRC_FOLDER_PATH, System.currentTimeMillis()));

        Assert.assertFalse(exists(absoluteFolderPath));
    }

    @Test
    public void testDeleteResourceWithNonEmptyFolderResource() throws IOException {
        final Path absoluteFilePath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_HELLO_FILE_PATH);
        createFile(absoluteFilePath);

        final Path absoluteFolderPath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_SRC_FOLDER_PATH);
        project.deleteResource(Resource.newFolder(RELATIVE_PROJECT_SRC_FOLDER_PATH, System.currentTimeMillis()));

        Assert.assertFalse(exists(absoluteFolderPath));
    }

    @Test
    public void testDeleteResourceWithFileResource() throws IOException {
        final Path absoluteFilePath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_README_FILE_PATH);
        project
                .deleteResource(Resource.newFile(RELATIVE_PROJECT_README_FILE_PATH, System.currentTimeMillis(), new byte[0]));

        Assert.assertFalse(exists(absoluteFilePath));
    }
}