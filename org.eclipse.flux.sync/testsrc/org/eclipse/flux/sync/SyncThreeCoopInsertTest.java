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
public class SyncThreeCoopInsertTest {
	
	private SyncController sync1;
	private SyncController sync2;
	private SyncController sync3;
	private SimpleStringSyncConnector connector1;
	private SimpleStringSyncConnector connector2;
	private SimpleStringSyncConnector connector3;

	@Before
	public void setup() {
		connector1 = new SimpleStringSyncConnector("cat");
		connector2 = new SimpleStringSyncConnector("cat");
		connector3 = new SimpleStringSyncConnector("cat");

		sync1 = new SyncController("sync1", connector1);
		sync2 = new SyncController("sync2", connector2);
		sync3 = new SyncController("sync3", connector3);
	}
	
	@Test
	public void testInsertsAfterEachOther() {
		InsertOperation operation1 = sync1.createInsertOperation("s", 3);
		sync1.applyOperation(operation1);
		sync2.applyOperation(operation1);
		sync3.applyOperation(operation1);
		
		assertEquals("cats", connector1.getText());
		assertEquals("cats", connector2.getText());
		assertEquals("cats", connector3.getText());

		InsertOperation operation2 = sync2.createInsertOperation("old ", 0);
		sync2.applyOperation(operation2);
		sync1.applyOperation(operation2);
		sync3.applyOperation(operation2);
		
		assertEquals("old cats", connector1.getText());
		assertEquals("old cats", connector2.getText());
		assertEquals("old cats", connector3.getText());

		InsertOperation operation3 = sync3.createInsertOperation(" and dogs", 8);
		sync3.applyOperation(operation3);
		sync1.applyOperation(operation3);
		sync2.applyOperation(operation3);
		
		assertEquals("old cats and dogs", connector1.getText());
		assertEquals("old cats and dogs", connector2.getText());
		assertEquals("old cats and dogs", connector3.getText());
	}
	
	@Test
	public void testThreeParallelInserts() {
		InsertOperation operation1 = sync1.createInsertOperation("c", 0);
		InsertOperation operation2 = sync2.createInsertOperation("a", 1);
		InsertOperation operation3 = sync3.createInsertOperation("t", 2);

		sync1.applyOperation(operation1);
		sync2.applyOperation(operation2);
		sync3.applyOperation(operation3);
		
		assertEquals("ccat", connector1.getText());
		assertEquals("caat", connector2.getText());
		assertEquals("catt", connector3.getText());
		
		sync1.applyOperation(operation2);
		sync2.applyOperation(operation1);
		sync3.applyOperation(operation1);
		
		sync1.applyOperation(operation3);
		sync2.applyOperation(operation3);
		sync3.applyOperation(operation2);
		
		assertEquals("ccaatt", connector1.getText());
		assertEquals("ccaatt", connector2.getText());
		assertEquals("ccaatt", connector3.getText());
	}
	
}
