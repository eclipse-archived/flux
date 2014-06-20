# Eclipse Flux Orion Integration

  This modules integrates Flux into Orion. Flux features to integrate:
  * File System
  * Live (Google-docs like) editing in Orion editor
  * Content Assist proposals
  * Validation

## Basic Design

The design is based on Orion's plugins extension mechanism. See https://wiki.eclipse.org/Orion/Documentation/Developer_Guide

### The File System

**orion.core.file** extension is used to mount Flux file system, i.e. Flux contents.

### The Collaborative Editing
Live or Collaborative editing means a Google Doc like editing of resources. In other words the same resource from a project connected to Flux server opened in Eclipse or some kind of web client (Orion in our case) is shared between all entities perfroming edits of the resource. Working copy is the same between all editors.
A new plugin extension has been introduced into Orion with id **orion.edit.live**. The plugin JS module is required to have two functions:

*	__startEdit(editorContext, options, markerService)__ starts the live edit session
*	__endEdit(resourceUrl)__ ends the live edit session

The major difference of this plug-in is that it keeps the *editorContext* available for as long as the client needs it, e.g. until *endEdit(...)* call is made. Editor context object is needed for updating contents of the editor based on messages received from Flux.
In addition, the *startEdit(...)* function provides markerService object that would help to display problems being received from Flux server generated from editing a resource (reconciling in Eclipse).


### The Content Assist Proposals
Content assist is contributed via the **orion.edit.contentAssist** plug-in extension. The plug-in extension under the hood broadcasts the message via the Flux messaging service that content assist is requested. Once the answer with proposals is received the Orion's service request is full-filled and the result is the array of proposals received from Flux transformed into Orion-friendly proposal's format. 


### Validation
Validation is the ability to provide various resource markers from Eclipse to Orion editor via Flux messaging system.
Orion's **orion.edit.validator** plug-in extension has been used for that. The message is broadcasted by the plug-in via the Flux messaging system to compute problems for a resource. Once the message with problems is received the Orion's service request is full-filled with the list markers objects. The plug-in's implementations converts Flux marker data objects into Orion's format for marker data objects.

 
## Running the prototype

Pre-conditions: Flux node server and eclipse application are up and running
	
1. Import flux.orion.integration project into your workspace on OrionHub (https://orionhub.org)
2. Launch the project created above as a web-site (https://wiki.eclipse.org/Orion/Documentation/User_Guide/Getting_started#Launching_your_project_as_a_website)
3. Check out Orion Client code from GitHub from the forked repository https://github.com/BoykoAlex/orion.client 
3. Launch Orion Node JS application from a subfolder of the locartion where Orion Client code has been cloned
4. Open localhost:8081 in the browser (your locally launched Orion)
5. Navigate to Settings -> Plugins, (http://localhost:8081/settings/settings.html#,category=plugins)
6. Click on Install (top-right corner of the list of installed plugins)
7. Enter plugin URL in the popped up web widget and click submit. Plugin URL is the URL of the flux.html on your web-site on Orion from step 3. (For me it is http://flux.orionhub.org:8080/flux.html)
  

