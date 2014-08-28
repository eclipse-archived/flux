/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

@Controller
public class FluxProjectsController {

	@Inject
	private ConnectionRepository connectionRepository;
	
	@RequestMapping(value="/flux/projects", method=RequestMethod.GET)
	public String projects(Principal currentUser, Model model) throws Exception {
		Connection<Flux> connection = connectionRepository.findPrimaryConnection(Flux.class);
		model.addAttribute("profile", connection.getApi().getUserProfile());
		model.addAttribute("fluxToken", connection.getApi().getAccessToken());
		model.addAttribute("projects", connection.getApi().getProjects());
		return "flux/projects";
	}
	
}
