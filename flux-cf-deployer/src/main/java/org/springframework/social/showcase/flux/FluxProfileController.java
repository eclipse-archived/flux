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
package org.springframework.social.showcase.flux;

import java.security.Principal;

import javax.inject.Inject;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.showcase.flux.support.Flux;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Kris De Volder
 */
@Controller
public class FluxProfileController {

	@Inject
	private ConnectionRepository connectionRepository;
	
	@RequestMapping(value="/flux", method=RequestMethod.GET)
	public String projects(Principal currentUser, Model model) throws Exception {
		Connection<Flux> connection = connectionRepository.findPrimaryConnection(Flux.class);
		model.addAttribute("profile", connection.getApi().getUserProfile());
		model.addAttribute("fluxToken", connection.getApi().getAccessToken());
		model.addAttribute("projects", connection.getApi().getProjects());
		return "flux/profile";
	}
	
	
	
}
