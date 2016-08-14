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
import org.eclipse.flux.watcher.core.RepositoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests Google Guice bootstrap.
 *
 * @author Kevin Pollet
 */
public final class JDKProjectModuleTest {
    @Test
    public void testBootstrap() {
        final Injector injector = Guice.createInjector(new RepositoryModule(), new JDKProjectModule());

        Assert.assertNotNull(injector.getInstance(Repository.class));
    }
}
