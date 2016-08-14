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

import static org.eclipse.flux.watcher.core.FluxMessageType.CONNECT_TO_CHANNEL;

/**
 * {@link com.codenvy.flux.watcher.core.FluxMessageType} tests.
 *
 * @author Kevin Pollet
 */
public final class FluxMessageTypeTest {
    @Test(expected = NullPointerException.class)
    public void testFromTypeWithNullType() {
        FluxMessageType.fromType(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromTypeWithNonExistentType() {
        FluxMessageType.fromType("foo");
    }

    @Test
    public void testFromType() {
        final FluxMessageType fluxMessageType = FluxMessageType.fromType(CONNECT_TO_CHANNEL.value());

        Assert.assertEquals(CONNECT_TO_CHANNEL, fluxMessageType);
    }
}
