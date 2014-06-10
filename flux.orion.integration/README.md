# Eclipse Flux Orion Integration

  This modules integrates Flux into Orion. Flux features to integrate:
  * File System
  * Collaborative (Google-docs like) editing in Orion editor
  * Content Assist proposals
  * Validation

## Basic Design

	The design is based on Orion's plugins extension mechanism. See https://wiki.eclipse.org/Orion/Documentation/Developer_Guide

### The File System

	orion.core.file extension is used to mount Flux file system, i.e. Flux contents.

### The Collaborative Editing

### The Content Assist Proposals

### Validation
  
## Running the prototype

	Pre-conditions: Flux node server and eclipse application are up and running
	
	1. Import flux.orion.integration project into your workspace on OrionHub (https://orionhub.org)
	2. Launch the project created above as a web-site (https://wiki.eclipse.org/Orion/Documentation/User_Guide/Getting_started#Launching_your_project_as_a_website)
	3. Launch Orion Node server application (the dev version from source from the Git repository)
	4. Open localhost:8081 in the browser (your locally launched Orion)
	5. Navigate to Settings -> Plugins, (http://localhost:8081/settings/settings.html#,category=plugins)
	6. Click on Install (top-right corner of the list of installed plugins)
	7. Enter plugin URL in the popped up web widget and click submit. Plugin URL is the URL of the flux.html on your web-site on Orion from step 3. (For me it is http://flux.orionhub.org:8080/flux.html)
  

