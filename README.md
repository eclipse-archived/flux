# Eclipse Flux

  Project "Eclipse Flux" is prototype work to evaluate possible future cloud-based developer
  tooling.

  The underlying idea is based on a Git-aware project sync mechanism (you can think of it as
  DropBox for code) that keeps projects in sync across different machines and services. While the
  storage and syncing mechanism is cloud-based, clients to this mechanism can be anything (ranging
  from a plugin for Eclipse, a file watcher process on your machine, a browser application, a
  headless service running somewhere else in the cloud, etc.).

## The basic design

  The fundamental design idea behind this project is the differentiation between the resources of
  the projects and additional services that can work on those resources.

  The resources are usually the artifacts of your project (everything that is or would be committed
  into a Git repo). The backbone sync mechanism stores those resources and helps clients to keep track
  of changes (so that syncing is possible).

  The services can act as clients of this resource sync backbone and operate on those synced resources.
  The nature of those clients can be very different from each other. Examples include:

  * Eclipse plugin to automatically sync projects in your workspace with the sync service that is running in the cloud
  * a file watcher process that does the same as the Eclipse plugin, but for files on your disc
  * an IntelliJ plugin that does the same as the Eclipse plugin, but plugs into IntelliJ
  * a web editor that allows you to edit and save resources using a browser only
  * a service that compiles Java resources and stores compilation results (like errors and warnings) back into the cloud as metadata
  * a service that runs JUnit tests automatically whenever a resource changes
  * a service that keeps a search index up-to-date
  * much more...

  The underlying mechanisms are language neutral. Specific services can provide different services for
  different languages or language versions.

## The Eclipse UI plugin

  The Eclipse UI plugin (org.eclipse.flux.ui.integration) allows you to sync the projects in your workspace with
  the cloud-based sync backbone mechanism. It is the central element to provide a smooth and seamless transition
  from using Eclipse towards using more and more cloud-based tooling.

## The Eclipse JDT service

  The Eclipse JDT service project (org.eclipse.flux.jdt.service) is the service that provides a number of
  Java related services (like reconciling, navigation, rename in file, and content-assist). It can be used within
  a running Eclipse IDE (in that case the running Eclipse IDE serves also as the host for the JDT cloud service)
  or within a headless environment on a cloud machine.

## The web editor

  The current web editor is a prototype to allow users to edit synced projects using a browser only. The editor
  listens to metadata changes and displays them while typing and provides content-assist for Java by sending
  a content-asists request into the cloud and by reacting to the first response to this request.

  The editor is implemented based on the Eclipse Orion editor component at the moment.

## Technical background

  The current focus of the prototype work is to figure out what is possible to realize on top of this design
  and what makes sense to develop further.

  The sync backbone is implemented on top of node.js, socket.io, and websockets, and provides a channel to
  exchange JSON messages among the participants. The rest is implemented on top of this idea of sending around
  (broadcasting or one-to-one) those JSON messages asynchronously.
  
  In addition to that the node.js server process also includes a backup repository that either stores resources
  in memory or in a MongoDB, if one is up and running.
  
## Running the prototype

### Running the node-js server

  The node.js-based server can be found in the "node.server" folder. Switch to that directory and install the
  necessary dependencies via npm:
  
  ```
  npm install
  ```
  
  Then you can star the node.js server application:
  
  ```
  npm start
  ```
  
  This runs the node.js-based messaging server that does not only contain the websocket-based messaging
  implementation, but also an in-memory backup repository that keeps track of connected projects.
  
  In case you have a MongoDB running, the in-memory repository is replaced by a MongoDB-based implementation
  that reads and writes your projects from/to a MongoDB database.
  
### Running the Eclipse plugin

  At the moment there is no update site available from which you can install the Eclipse plugins into
  an existing Eclipse installation. Instead you have to import all three projects into a workspace and
  start a runtime workbench from there. 
  
  So please import all three Eclipse projects into an empty workspace. There is a forth project that is
  called "eclipse.releng" that contains a target platform definition. Please import that project as well and
  set the contained target definition as your target platform. After that everything should compile fine.
  
  If you want the JDT service to run inside your Eclipse IDE (instead of as a headless service), you should
  set a start level of 4 and auto-start:true for the org.eclipse.flux.jdt.service bundle in your launch
  configuration. This will startup all the JDT services inside your Eclipse IDE.

  In case you target the locally running node server, you don't have to specify anything. The node server will
  listen on port 3000 and the Eclipse plugin will use http://localhost:3000 for all the server
  communication. In case you have the server running somewhere else, you can set this system property in the
  launch config of your runtime workbench to direct the plugin towards the right server:
  
  ```
  -Dflux-host=https://flight627.cfapps.io:4443
  ```
  
  To enable the 'live edit' connector that syncs between the webeditor and the eclipse editor as you type,
  add the following system property:
  
  ```
  -Dflux-eclipse-editor-connect=true
  ```
  
  Once you are running your runtime workbench and the node server you can:
  
     - create a test project
     - Use context menu 'Flux >> Connect' to connect it to Flux.
     - open a resource in the web-editor at a url like the following:
         http://localhost:3000/client/html/editor.html#defaultuser/test-project/src/flux/test/Main.java
         
## Authentication

The prototype supports user authentication via github using oauth. Some setup is required to make it work.

### Github Client ID and Secret

Get a github client ID and secret [here](https://github.com/settings/applications/new
and define two environment variables `FLUX_GITHUB_CLIENT_ID` and `FLUX_GITHUB_CLIENT_SECRET`
When you start the server and these variables are defined, Flux authentication will be enabled.

If the environment variables are undefined Flux will run in 'no authentication' mode:
the user 'defaultuser' is treated as if always logged in.

### Authenticating the Web Client

Open the page at http://localhost:3000/ you should be automatically redirected to github to sign on.
After that the client will be in an authenticated session tied to the user-id you logged in as.

### Authenticating the Eclipse / Java Client

The Eclipse client currently is not able to use oauth (oauth is more geared towards browser-based
applications). Instead it uses a github user-id and github "Personal Access Token" to authenticate. 
The server verifies the validity of the token via github rest API. 

You provide these credentials, as system properties when running the 
Eclipse/Java process. For example:

    -Dflux.user.name=kdvolder
    -Dflux.user.token=<get-your-own>

You can generate (and revoke) tokens [here](https://github.com/settings/applications).
Click the 'Generate New Token' button next to 'Personal Access Tokens'.

## Flux on CloudFoundry

### Accessing Flux on CloudFoundry

  An experimental prototype is deployed to 'flux.cfapps.io'. This is a moving target.
  
  It provides a basic landing page that shows logged-in user's projects and 
  a list of files in each project (not organized in any way). Clicking one of 
  the files opens an embedded orion editor connected to the Flux message bus.
  
  Currently there is no way to create projects or files via this basic UI. 
  So to get files / projects into it you have to run an Eclipse instance connected to
  Flux (see "Running The Eclipse Plugin" above). To connect to Flux on CF, 
  set these system properties:
  
       -Dflux-eclipse-editor-connect=true
       -Dflux-host=https://flux.cfapps.io:4443
       -Dflux.user.name=...your github login id...
       -Dflux.user.token=...github personal access token...

### Deploying to CloudFoundry

 To deploy Flux to Cloudfoundry requires the Flux github client-id and secret.
 If deployed without this, flux will run in 'authentication disabled' mode
 where everyone is treated as 'defaultuser' and shares the same workspace.
 
 To deploy, create a `node.server/manifest.yml`:
 
     ---
     applications:
     - name: flux
       memory: 512M
       host: flux
       env:
         FLUX_GITHUB_CLIENT_ID: ...put Flux client id here...
         FLUX_GITHUB_CLIENT_SECRET: ...put Flux client secret here...  
         
 If you register a new Client ID / Secret on github make sure to set the
 callback url to:
 
     https://flux.cfapps.io/auth/github/callback
 
       
## Status

  This is prototype work and by no means meant to be used in production. It misses important features, good
  error handling, user authentication, and extensive unit testing.

## License

  Dual licensed under EPL 1.0 & EDL 1.0.

