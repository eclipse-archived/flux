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
package org.eclipse.flux.client.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.flux.client.util.Assert;

/**
 * Manages a mapping messageType -> DeliveryType 
 */
public class DeliveryTypes {
	
	public static final DeliveryTypes DEFAULTS = new DeliveryTypes();
	
	/**
	 * Caches the already computed delivery type mappings. Can also be
	 * used to explicitly register a delivery type for a given messageType
	 * (in case the computedDefault is not what is wanted).
	 */
	private Map<String, DeliveryType> map = new HashMap<>();
	
	public synchronized DeliveryType get(String messageType) {
		DeliveryType dt = map.get(messageType);
		if (dt==null) {
			dt = computeDefault(messageType);
			if (dt!=null) {
				map.put(messageType, dt);
			}
		}
		Assert.assertTrue(dt!=null);
		return dt;
	}

	protected DeliveryType computeDefault(String messageType) {
		if (messageType.endsWith("Request")) {
			return DeliveryType.REQUEST;
		} else if (messageType.endsWith("Response")) {
			return DeliveryType.RESPONSE;
		}
		return DeliveryType.BROADCAST;
	}
	
	public synchronized DeliveryTypes put(String messageType, DeliveryType deliveryType) {
		this.map.put(messageType, deliveryType);
		return this;
	}

}
