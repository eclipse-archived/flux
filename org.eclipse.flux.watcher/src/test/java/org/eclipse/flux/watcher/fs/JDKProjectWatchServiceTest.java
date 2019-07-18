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

import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.RepositoryEvent;
import org.eclipse.flux.watcher.core.RepositoryEventBus;
import org.eclipse.flux.watcher.core.RepositoryEventType;
import org.eclipse.flux.watcher.core.RepositoryEventTypes;
import org.eclipse.flux.watcher.core.RepositoryListener;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import com.google.common.base.Throwables;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.inject.Provider;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static org.eclipse.flux.watcher.core.RepositoryEventType.PROJECT_RESOURCE_CREATED;
import static org.eclipse.flux.watcher.core.RepositoryEventType.PROJECT_RESOURCE_DELETED;
import static org.eclipse.flux.watcher.core.RepositoryEventType.PROJECT_RESOURCE_MODIFIED;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.FILE;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.FOLDER;
import static org.eclipse.flux.watcher.core.Resource.ResourceType.UNKNOWN;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.WatchEvent.Kind;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link com.codenvy.flux.watcher.fs.JDKProjectWatchService} tests.
 *
 * @author Kevin Pollet
 */
public final class JDKProjectWatchServiceTest extends AbstractTest {
    private JDKProject             jdkProject;
    private JDKProjectWatchService jdkProjectWatchService;
    private RepositoryEventBus     repositoryEventBus;

    @SuppressWarnings("unchecked")
    @Before
    public void beforeTest() throws NoSuchMethodException {
        final Provider<Repository> repositoryProviderMock = mock(Provider.class);
        when(repositoryProviderMock.get()).thenAnswer(new Answer<Repository>() {
            @Override
            public Repository answer(InvocationOnMock invocationOnMock) throws Throwable {
                final Repository repositoryMock = mock(Repository.class);
                final Project projectMock = mock(Project.class);
                when(repositoryMock.getProject(PROJECT_ID)).thenReturn(projectMock);

                return repositoryMock;
            }
        });

        repositoryEventBus = new RepositoryEventBus(Collections.<RepositoryListener>emptySet(), repositoryProviderMock);
        jdkProjectWatchService = new JDKProjectWatchService(fileSystem(), repositoryEventBus);
        jdkProject = new JDKProject(fileSystem(), jdkProjectWatchService, PROJECT_ID, PROJECT_PATH);
    }

    @Test(expected = NullPointerException.class)
    public void testWatchWithNullProject() {
        jdkProjectWatchService.watch(null);
    }

    @Test(expected = NullPointerException.class)
    public void testUnwatchWithNullProject() {
        jdkProjectWatchService.unwatch(null);
    }

    @Test
    public void testKindToRepositoryEventTypeWithNullKind() throws Exception {
        final RepositoryEventType repositoryEventType = kindToRepositoryEventType(null);

        Assert.assertNull(repositoryEventType);
    }

    @Test
    public void testKindToRepositoryEventTypeWithEntryCreateKind() throws Exception {
        final RepositoryEventType repositoryEventType = kindToRepositoryEventType(ENTRY_CREATE);

        Assert.assertNotNull(repositoryEventType);
        Assert.assertEquals(PROJECT_RESOURCE_CREATED, repositoryEventType);
    }

    @Test
    public void testKindToRepositoryEventTypeWithEntryDeleteKind() throws Exception {
        final RepositoryEventType repositoryEventType = kindToRepositoryEventType(ENTRY_DELETE);

        Assert.assertNotNull(repositoryEventType);
        Assert.assertEquals(PROJECT_RESOURCE_DELETED, repositoryEventType);
    }

    @Test
    public void testKindToRepositoryEventTypeWithEntryModifyKind() throws Exception {
        final RepositoryEventType repositoryEventType = kindToRepositoryEventType(ENTRY_MODIFY);

        Assert.assertNotNull(repositoryEventType);
        Assert.assertEquals(PROJECT_RESOURCE_MODIFIED, repositoryEventType);
    }

    @Test(expected = NullPointerException.class)
    public void testCastWithNullEvent() throws Throwable {
        final Method castMethod = JDKProjectWatchService.class.getDeclaredMethod("cast", WatchEvent.class);
        castMethod.setAccessible(true);

        try {

            castMethod.invoke(jdkProjectWatchService, (WatchEvent)null);

        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testPathToResourceWithNullKind() throws Exception {
        pathToResource(null, jdkProject, fileSystem().getPath(PROJECT_PATH));
    }

    @Test(expected = NullPointerException.class)
    public void testPathToResourceWithNullProject() throws Exception {
        pathToResource(ENTRY_CREATE, null, fileSystem().getPath(PROJECT_PATH));
    }

    @Test(expected = NullPointerException.class)
    public void testPathToResourceWithNullResourcePath() throws Exception {
        pathToResource(ENTRY_CREATE, jdkProject, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPathToResourceWithCreateKindAndNonExistentResourcePath() throws Exception {
        pathToResource(ENTRY_CREATE, jdkProject, fileSystem().getPath("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPathToResourceWithRelativeResourcePath() throws Exception {
        pathToResource(ENTRY_CREATE, jdkProject, fileSystem().getPath(RELATIVE_PROJECT_README_FILE_PATH));
    }

    @Test
    public void testPathToResourceWithFolderPath() throws Exception {
        final Path absoluteFolderPath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_SRC_FOLDER_PATH);
        final Resource resource = pathToResource(ENTRY_CREATE, jdkProject, absoluteFolderPath);

        Assert.assertNotNull(resource);
        Assert.assertEquals(RELATIVE_PROJECT_SRC_FOLDER_PATH, resource.path());
        Assert.assertEquals(FOLDER, resource.type());
        Assert.assertEquals(getLastModifiedTime(absoluteFolderPath).toMillis(), resource.timestamp());
        Assert.assertArrayEquals(null, resource.content());
        Assert.assertNotNull(resource.hash());
    }

    @Test
    public void testPathToResourceWithFilePath() throws Exception {
        final Path absoluteFilePath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_README_FILE_PATH);
        final Resource resource = pathToResource(ENTRY_CREATE, jdkProject, absoluteFilePath);

        Assert.assertNotNull(resource);
        Assert.assertEquals(RELATIVE_PROJECT_README_FILE_PATH, resource.path());
        Assert.assertEquals(FILE, resource.type());
        Assert.assertEquals(getLastModifiedTime(absoluteFilePath).toMillis(), resource.timestamp());
        Assert.assertArrayEquals(readAllBytes(absoluteFilePath), resource.content());
        Assert.assertNotNull(resource.hash());
    }

    @Test
    public void testWatchEntryCreateFile() throws InterruptedException, IOException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ProjectResourceCreatedListener projectResourceCreatedListener = new ProjectResourceCreatedListener(countDownLatch);

        jdkProjectWatchService.watch(jdkProject);
        repositoryEventBus.addRepositoryListener(projectResourceCreatedListener);

        final Path absoluteFilePath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_HELLO_FILE_PATH);
        createFile(absoluteFilePath);

        countDownLatch.await(1, MINUTES);

        final RepositoryEvent repositoryEvent = projectResourceCreatedListener.repositoryEvent;

        Assert.assertNotNull(repositoryEvent);
        Assert.assertEquals(PROJECT_ID, repositoryEvent.project().id());
        Assert.assertEquals(PROJECT_RESOURCE_CREATED, repositoryEvent.type());
        Assert.assertEquals(RELATIVE_PROJECT_HELLO_FILE_PATH, repositoryEvent.resource().path());
        Assert.assertEquals(FILE, repositoryEvent.resource().type());
        Assert.assertEquals(getLastModifiedTime(absoluteFilePath).toMillis(), repositoryEvent.resource().timestamp());
        Assert.assertArrayEquals(readAllBytes(absoluteFilePath), repositoryEvent.resource().content());
        Assert.assertNotNull(repositoryEvent.resource().hash());
    }

    @Test
    public void testWatchEntryCreateFolder() throws InterruptedException, IOException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ProjectResourceCreatedListener projectResourceCreatedListener = new ProjectResourceCreatedListener(countDownLatch);

        jdkProjectWatchService.watch(jdkProject);
        repositoryEventBus.addRepositoryListener(projectResourceCreatedListener);

        final Path absoluteFilePath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_MAIN_FOLDER_PATH);
        createDirectory(absoluteFilePath);

        countDownLatch.await(1, MINUTES);

        final RepositoryEvent repositoryEvent = projectResourceCreatedListener.repositoryEvent;

        Assert.assertNotNull(repositoryEvent);
        Assert.assertEquals(PROJECT_RESOURCE_CREATED, repositoryEvent.type());
        Assert.assertEquals(PROJECT_ID, repositoryEvent.project().id());
        Assert.assertEquals(RELATIVE_PROJECT_MAIN_FOLDER_PATH, projectResourceCreatedListener.repositoryEvent.resource().path());
        Assert.assertEquals(FOLDER, projectResourceCreatedListener.repositoryEvent.resource().type());
        Assert.assertEquals(getLastModifiedTime(absoluteFilePath).toMillis(), repositoryEvent.resource().timestamp());
        Assert.assertArrayEquals(null, repositoryEvent.resource().content());
        Assert.assertNotNull(repositoryEvent.resource().hash());
    }

    @Test
    public void testWatchEntryModifyFile() throws InterruptedException, IOException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ProjectResourceModifiedListener projectResourceModifiedListener = new ProjectResourceModifiedListener(countDownLatch);

        jdkProjectWatchService.watch(jdkProject);
        repositoryEventBus.addRepositoryListener(projectResourceModifiedListener);

        final String content = "README";
        final Path absoluteFilePath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_README_FILE_PATH);
        write(absoluteFilePath, content.getBytes());

        countDownLatch.await(1, MINUTES);

        final RepositoryEvent repositoryEvent = projectResourceModifiedListener.repositoryEvent;

        Assert.assertNotNull(repositoryEvent);
        Assert.assertEquals(PROJECT_RESOURCE_MODIFIED, repositoryEvent.type());
        Assert.assertEquals(PROJECT_ID, repositoryEvent.project().id());
        Assert.assertEquals(RELATIVE_PROJECT_README_FILE_PATH, repositoryEvent.resource().path());
        Assert.assertEquals(FILE, repositoryEvent.resource().type());
        Assert.assertEquals(getLastModifiedTime(absoluteFilePath).toMillis(), repositoryEvent.resource().timestamp());
        Assert.assertArrayEquals(content.getBytes(), repositoryEvent.resource().content());
        Assert.assertNotNull(repositoryEvent.resource().hash());
    }

    @Test
    public void testWatchEntryDeleteFile() throws InterruptedException, IOException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ProjectResourceDeletedListener projectResourceDeletedListener = new ProjectResourceDeletedListener(countDownLatch);

        jdkProjectWatchService.watch(jdkProject);
        repositoryEventBus.addRepositoryListener(projectResourceDeletedListener);

        final Path absoluteFilePath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_README_FILE_PATH);
        delete(absoluteFilePath);

        countDownLatch.await(1, MINUTES);

        final RepositoryEvent repositoryEvent = projectResourceDeletedListener.repositoryEvent;

        Assert.assertNotNull(repositoryEvent);
        Assert.assertEquals(PROJECT_RESOURCE_DELETED, repositoryEvent.type());
        Assert.assertEquals(PROJECT_ID, repositoryEvent.project().id());
        Assert.assertEquals(RELATIVE_PROJECT_README_FILE_PATH, repositoryEvent.resource().path());
        Assert.assertEquals(UNKNOWN, projectResourceDeletedListener.repositoryEvent.resource().type());
        Assert.assertArrayEquals(null, repositoryEvent.resource().content());
        Assert.assertNotNull(repositoryEvent.resource().hash());
    }

    @Test
    public void testWatchEntryDeleteFolder() throws InterruptedException, IOException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ProjectResourceDeletedListener projectResourceDeletedListener = new ProjectResourceDeletedListener(countDownLatch);

        jdkProjectWatchService.watch(jdkProject);
        repositoryEventBus.addRepositoryListener(projectResourceDeletedListener);

        final Path absoluteFolderPath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_SRC_FOLDER_PATH);
        delete(absoluteFolderPath);

        countDownLatch.await(1, MINUTES);

        final RepositoryEvent repositoryEvent = projectResourceDeletedListener.repositoryEvent;

        Assert.assertNotNull(repositoryEvent);
        Assert.assertEquals(PROJECT_RESOURCE_DELETED, repositoryEvent.type());
        Assert.assertEquals(PROJECT_ID, repositoryEvent.project().id());
        Assert.assertEquals(RELATIVE_PROJECT_SRC_FOLDER_PATH, repositoryEvent.resource().path());
        Assert.assertEquals(UNKNOWN, repositoryEvent.resource().type());
        Assert.assertArrayEquals(null, repositoryEvent.resource().content());
        Assert.assertNotNull(repositoryEvent.resource().hash());
    }

    @Test
    public void testUnwatch() throws IOException, InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final ProjectResourceDeletedListener projectResourceDeletedListener = new ProjectResourceDeletedListener(countDownLatch);

        jdkProjectWatchService.watch(jdkProject);
        repositoryEventBus.addRepositoryListener(projectResourceDeletedListener);
        jdkProjectWatchService.unwatch(jdkProject);

        final Path absoluteFolderPath = fileSystem().getPath(PROJECT_PATH).resolve(RELATIVE_PROJECT_SRC_FOLDER_PATH);
        delete(absoluteFolderPath);

        countDownLatch.await(30, SECONDS);

        Assert.assertNull(projectResourceDeletedListener.repositoryEvent);
    }

    private RepositoryEventType kindToRepositoryEventType(Kind<?> kind) throws Exception {
        final Method kindToRepositoryEventTypeMethod =
                JDKProjectWatchService.class.getDeclaredMethod("kindToRepositoryEventType", Kind.class);
        kindToRepositoryEventTypeMethod.setAccessible(true);

        try {

            return (RepositoryEventType)kindToRepositoryEventTypeMethod.invoke(jdkProjectWatchService, kind);

        } catch (InvocationTargetException e) {
            throw Throwables.propagate(e.getCause());
        }
    }

    private Resource pathToResource(Kind<Path> kind, Project project, Path resourcePath) throws Exception {
        final Method pathToResourceMethod =
                JDKProjectWatchService.class.getDeclaredMethod("pathToResource", Kind.class, Project.class, Path.class);
        pathToResourceMethod.setAccessible(true);

        try {

            return (Resource)pathToResourceMethod.invoke(jdkProjectWatchService, kind, project, resourcePath);

        } catch (InvocationTargetException e) {
            throw Throwables.propagate(e.getCause());
        }
    }

    private static abstract class AbstractRepositoryListener implements RepositoryListener {
        private final CountDownLatch  countDownLatch;
        public        RepositoryEvent repositoryEvent;

        public AbstractRepositoryListener(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
            this.repositoryEvent = null;
        }

        @Override
        public void onEvent(RepositoryEvent event) {
            try {

                if (repositoryEvent != null) {
                    throw new IllegalStateException();
                }
                repositoryEvent = event;

            } finally {
                countDownLatch.countDown();
            }
        }
    }

    @RepositoryEventTypes(PROJECT_RESOURCE_CREATED)
    private static class ProjectResourceCreatedListener extends AbstractRepositoryListener {
        public ProjectResourceCreatedListener(CountDownLatch countDownLatch) {
            super(countDownLatch);
        }
    }

    @RepositoryEventTypes(PROJECT_RESOURCE_MODIFIED)
    private static class ProjectResourceModifiedListener extends AbstractRepositoryListener {
        public ProjectResourceModifiedListener(CountDownLatch countDownLatch) {
            super(countDownLatch);
        }
    }

    @RepositoryEventTypes(PROJECT_RESOURCE_DELETED)
    private static class ProjectResourceDeletedListener extends AbstractRepositoryListener {
        public ProjectResourceDeletedListener(CountDownLatch countDownLatch) {
            super(countDownLatch);
        }
    }
}
