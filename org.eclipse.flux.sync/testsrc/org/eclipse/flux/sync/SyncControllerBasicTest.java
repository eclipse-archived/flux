/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.sync;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Martin Lippert
 */
public class SyncControllerBasicTest {
	
	private SyncController sync;
	private SimpleStringSyncConnector connector;

	@Before
	public void setup() {
		connector = new SimpleStringSyncConnector("cat");
		sync = new SyncController("sync1", connector);
	}
	
	@Test
	public void testParticipantID() {
		assertEquals("sync1", sync.getID());
	}

	@Test
	public void testInitialText() {
		assertEquals("cat", connector.getText());
	}

	@Test
	public void testLastOperationID() {
		assertNull(sync.getLastOperationID());
		
		InsertOperation operation = sync.createInsertOperation("s", 3);
		assertNull(sync.getLastOperationID());
		
		sync.applyOperation(operation);
		assertEquals(operation.getId(), sync.getLastOperationID());
	}

}
