package org.springframework.social.showcase.flux;

import java.security.Principal;

import javax.inject.Inject;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CloudfoundryController {

	private CloudFoundryManager cfm;
	
	@Inject
	public CloudfoundryController(CloudFoundryManager cfm) {
		this.cfm = cfm;
	}
	
	@Inject
	private Environment env;

	/**
	 * Presents a form to deploy a project to CF
	 */
	@RequestMapping("/cloudfoundry/deploy/{project}")
	public String deploy(Principal currentUser, @PathVariable String project, Model model) {
		CloudFoundry cf = cfm.getConnection(currentUser);
		if (cf==null) {
			//Not yet logged into CF. 
			//TODO: Could we use spring-social connection manager for this? It seems like it should be possible
			return "redirect:/cloudfoundry/login";
		}
		model.addAttribute("flux.project", project);
		return "cloudfoundry/deploy";
	}
	
	@RequestMapping(value="/cloudfoundry/processLogin")
	public String doLogin(Principal currentUser, Model model,
		@RequestParam(required=false, value="cf_login") String login, 
		@RequestParam(required=false, value="cf_password") String password
	) throws Exception {
		CloudFoundry cf = new CloudFoundry(env.getProperty("cloudfoundry.url", "https://api.run.pivotal.io/"));
		if (cf.login(login, password)) {
			cfm.putConnection(currentUser, cf);
			return "redirect:/cloudfoundry/selectspace";
		} else {
			return "redirect:/cloudfoundry/login?error=bad_credentials";
		}
	}
	
	@RequestMapping("/cloudfoundry/login")
	public String login() {
		return "cloudfoundry/login";
	}

}
