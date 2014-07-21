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
	
1. Check out Orion Client code from GitHub from the forked repository https://github.com/BoykoAlex/orion.client 
2. Launch Orion Node JS application from _<Orion Client folder>/modules/orionode_ folder by executing _"npm install"_ and then _"npm start"_ commands
3. Open localhost:8081 in the browser (your locally launched Orion)
4. Navigate to Settings -> Plugins, (http://localhost:8081/settings/settings.html#,category=plugins)
5. Click on Install (top-right corner of the list of installed plugins)
6. Enter plugin URL http://localhost:3000/orion-plugin/flux.html in the popped up web widget and click submit.
  

