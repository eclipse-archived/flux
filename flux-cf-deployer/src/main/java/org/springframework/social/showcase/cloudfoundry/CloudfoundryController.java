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
package org.springframework.social.showcase.cloudfoundry;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.core.env.Environment;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.showcase.flux.support.Flux;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CloudfoundryController {

	private CloudFoundryManager cfm;
	
	@Inject
	public CloudfoundryController(CloudFoundryManager cfm) {
		this.cfm = cfm;
	}
	
	@Inject
	private ConnectionRepository connectionRepository;
	
	@Inject
	private Environment env;

	/**
	 * Presents an overview of flux projects that can be synched to cloudfoundry.
	 */
	@RequestMapping("/cloudfoundry/deploy")
	public String deploy(Principal currentUser, Model model) throws Exception {
		CloudFoundry cf = cfm.getConnection(currentUser);
		if (cf==null) {
			return "redirect:/cloudfoundry/login";
		}
		Flux flux = flux();
		if (flux==null) {
			return "redirect:/singin/flux";
		}
		
		//The page should show a list of flux project with their deployment status.
		List<String> projects = flux.getProjects();
		model.addAttribute("user", cf.getUser());
		model.addAttribute("projects", flux.getProjects());
		model.addAttribute("spaces", cf.getSpaces());
		List<DeploymentConfig> deployments = new ArrayList<DeploymentConfig>();
		for (String pname : projects) {
			DeploymentConfig deployConf = cf.getDeploymentConfig(pname);
			deployments.add(deployConf);
		}
		model.addAttribute("deployments", deployments);
		
		return "cloudfoundry/deploy";
	}

	@RequestMapping(value="/cloudfoundry/deploy/{project}")
	public String deployProject(
			Principal currentUser, 
			@RequestParam Map<String,String> params,
			@PathVariable("project")String fluxProjectName, 
			Model model
	) throws Exception {
		CloudFoundry cf = cfm.getConnection(currentUser);
		if (cf==null) {
			return "redirect:/cloudfoundry/login";
		}
		Flux flux = flux();
		if (flux==null) {
			return "redirect:/singin/flux";
		}
		
		String setSpace = params.get("space");
		if (setSpace!=null) {
			//Handle form submission...
			boolean setSynch = "on".equals(params.get("synch"));
			DeploymentConfig config = cf.getDeploymentConfig(fluxProjectName);
			config.setCfSpace(setSpace);
			config.setActivated(setSynch);
			cf.apply(config);
			model.addAttribute("info_message", "Deployment config for '"+fluxProjectName+"' saved");
		} 
		
		model.addAttribute("fluxProjectName", fluxProjectName);
		model.addAttribute("spaces", cf.getSpaces());
		model.addAttribute("deployment", cf.getDeploymentConfig(fluxProjectName));
				
		return "cloudfoundry/deploy-project";
	}
	

	private Flux flux() {
		Connection<Flux> connection = connectionRepository.findPrimaryConnection(Flux.class);
		if (connection!=null) {
			return connection.getApi();
		}
		return null;
	}

	@RequestMapping(value="/cloudfoundry")
	public String profile(Principal currentUser, Model model) {
		CloudFoundry cf = cfm.getConnection(currentUser);
		if (cf==null) {
			return "redirect:/cloudfoundry/login";
		}
		model.addAttribute("user", cf.getUser());
		model.addAttribute("spaces", cf.getSpaces());
		return "cloudfoundry/profile";
	}
	
	@RequestMapping(value="/cloudfoundry/processLogin")
	public String doLogin(Principal currentUser, Model model,
		@RequestParam(required=false, value="cf_login") String login, 
		@RequestParam(required=false, value="cf_password") String password
	) throws Exception {
		Connection<Flux> flux = connectionRepository.findPrimaryConnection(Flux.class);

		CloudFoundry cf = new CloudFoundry(
				flux.getApi().getMessagingConnector(),
				env.getProperty("cloudfoundry.url", "https://api.run.pivotal.io/"));
		if (cf.login(login, password)) {
			cfm.putConnection(currentUser, cf);
			return "redirect:/cloudfoundry";
		} else {
			return "redirect:/cloudfoundry/login?error=bad_credentials";
		}
	}
	
	@RequestMapping("/cloudfoundry/login")
	public String login() {
		return "cloudfoundry/login";
	}

}
