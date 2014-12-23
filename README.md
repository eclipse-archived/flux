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

### RabbitMQ

  The current prototype uses RabbitMQ to relay messages between client connections. As such having access to
  a message broker is required to run the prototype.
  
  Follow the [instructions for installing RabbitMQ](https://www.rabbitmq.com/download.html) on your machine.
  The default 'out of the box' configuration should do just fine for running the prototype locally.

### Running the node-js server

  The node.js-based server can be found in the "node.server" folder. Switch to that directory and install the
  necessary dependencies via npm:
  
  ```
  npm install
  ```
  
  Then you can start the node.js server application:
  
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
  
  One of the projects "org.eclipse.flux.client.java.osgi" requires some jar files to be downloaded and/or
  built before everything will compile in an Eclipse workspace. To produce these jars in the right
  places do the following:
  
       cd org.eclipse.flux.headless.releng
       mvn clean install -Dmaven.test.skip=true
           
  Next import at least the following projects into the workspace:
  
  - org.eclipse.flux.client.java.osgi
  - org.eclipse.flux.client.java (optional, source-code for the client jar embedded inside 
	org.eclipse.flux.client.java.osgi)
  - org.eclipse.flux.core
  - org.eclipse.flux.ide.integration.repository
  - org.eclipse.flux.jdt.service
  - org.eclipse.flux.releng
  - org.eclipse.flux.ui.integration
  
  The "org.flux.eclipse.releng" project contains a target platform definition. 
  Set the contained target definition as your target platform. After that everything should compile fine.
  
  If you get errors about missing jars on buildpath, maybe you skipped the step of running mvn build on
  the commandline. No problem, just run it now and refresh the "client.java.osgi" project. The
  errors should go away.
  
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

Get a github client ID and secret [here](https://github.com/settings/applications/new)
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
       memory: 1024M
       host: flux
       env:
         FLUX_GITHUB_CLIENT_ID: ...put Flux client id here...
         FLUX_GITHUB_CLIENT_SECRET: ...put Flux client secret here...  
         
 If you register a new Client ID / Secret on github make sure to set the
 callback url to:
 
     https://flux.cfapps.io/auth/github/callback
 
 The prototype uses RabbitMQ to relay messages between clients. So you must create
 and bind a RabbitMQ service instance to your cloudfoundry app for it to work.
 
## Running Headless JDT Service Locally
JDT service provider can be run locally such that JDT services that it provides to users would also be running as local processes.

1. Navigate to _org.eclipse.flux.headless.releng_ folder and execute _"mvn clean package"_
2. Navigate to __org.eclipse.flux.service.common__ via console and execute _"mvn clean install"_
3. Navigate to __org.eclipse.flux.jdt.service.provider__ via console and execute _"mvn clean package"_
4. Either start __org.eclipse.flux.jdt.service.provider__ as Java application or launch it as _"java -jar org.eclipse.flux.jdt.service.provider-0.0.1-SNAPSHOT-jar-with-dependencies.jar"_ and provide the following program arguments:
  * __-host__ Flux messaging server URL (default: http://localhost:3000)
  * __-user__ Flux "admin" user id (Default: defaultuser)
  * __-password__ Flux "admin" user client secret (Default: empty string)
  * __-app__ Absolute path to the folder where built JDT service is located. For example the absolute path to *org.eclipse.flux.headless.product/target/products/org.eclipse.flux.headless/linux/gtk/x86_64* (Default: relative path "../org.eclipse.flux.headless.product/target/products/org.eclipse.flux.headless/macosx/cocoa/x86_64")
  * __-poolSize__ Number of JDT services ready for use for any user being up all the time waiting for a user to be assigned (Default: 3)  
5. Once started you should see the log messages about service pool being populated and then that it is successfully populated.

If JDT Service Provider app is stopped you'll find JDT Service processes from the pool of services still running. These processes will be active for 2 hours at most if service is not being used by the web client UI. Feel free to stop these Java processes manually. If JDT service provider application is started it would check if there JDT services processes already running and if they are it would only start the number of JDT services needed to get the defined __poolSize__ number.

## Running Headless JDT Service on Cloud Foundry
JDT service provider can be built and deployed on Cloud Foundry. JDT service provider application would deploy JDT service application on Cloud Foundry once started. Each user requiring a JDT service would have an instance of JDT service application running on the Cloud Foundry

1. Navigate to _org.eclipse.flux.headless.releng_ folder and execute _"mvn clean package"_
2. Navigate from current folder into *"../org.eclipse.flux.headless.product/target/products/org.eclipse.flux.headless/linux/gtk/x86_64"*
3. Execute *"jar xf plugins/org.eclipse.equinox.launcher_1.3.0.v20140415-2008.jar"*
4. Delete the META-INF folder
5. Execute *"jar cfe org.eclipse.flux.jdt.jar org.eclipse.equinox.launcher.Main ."*
6. Locate __org.eclipse.flux.jdt.service.provider/config/sample-manifest.yml__. Rename it to __manifest.yml__, open it with text editor and fill in the values for the follwoing environment variables:
    * __FLUX_HOST__ URL of the Flux server
    * __FLUX_ADMIN_TOKEN__ the secret token for the admin user
    * __FLUX_CF_CONTROLLER_URL__ the Cloud Foundry controller URL
    * __FLUX_CF_USER_ID__ user id to login to Cloud Foundry controller
    * __FLUX_CF_PASSWORD__ password to login to Cloud Foundry controller
    * __FLUX_CF_SPACE__ Cloud Foundry space to deploy the JDT service provider application and spawned JDT services to
    * __FLUX_CF_ORG__ Cloud Foundry "org" within the space to deploy the JDT service provider application and spawned JDT services to
    * __FLUX_SERVICE_APP_ID__ (optional) the id for the JDT service application on the Cloud Foundry
    * __FLUX_SERVICE_POOL_SIZE__ number of JDT services ready for use for any user being up all the time waiting for a user to be assigned
    * __FLUX_SERVICE_MAX_INSTANCES__ maximum number of JDT services that can be active at any moment of time including JDT service that are serving users and those not assigned a user (service pool)
7. Navigate to __org.eclipse.flux.service.common__ via console and execute _"mvn clean install"_
8. Navigate to __org.eclipse.flux.jdt.service.provider__ via console and execute _"mvn clean package"_ twice (BUG: copying of org.eclipse.flux.jdt.jar happens after the build)
9. Navigate to __org.eclipse.flux.jdt.service.provider/target__ folder via console and execute _"cf p"_

Note that after shutting down the JDT service provider app on Cloud Foundry instances of JDT service application would need to be shut down manually.

## Git-Crypt

 Note that the repository already contains some working 'manifest.yml*' files to
 deploy the production version flux to cloudfoundry. However these files are encrypted 
 with [git-crypt](https://github.com/AGWA/git-crypt). These files are only needed
 if you need to deploy new versions of flux to their official homes on cfapps.io.
  
 If you are committer, and need to deploy a new version into production, you can 
 ask another committer for the `flux.key` file and setup git-crypt. 
 
 For information on installing git-crypt on your system see 
 [git-crypt/install.md](https://github.com/AGWA/git-crypt/blob/master/INSTALL.md).
 
 Once you got the key-file put it somewhere safe on your system. It is a good idea to 
 make the key file only readable by you alone. Then `cd` into your flux repo clone
 and enter this command to 'unlock' the encrypted files (on older version of git-crypt):
 
    git-crypt init <path-to-key-file>
    
 On newer version of git-crypt you can use:
 
    git-crypt unlock <path-to-key-file>
 
 Once git-crypt is configured like this, you can forget about it. To you, the files 
 will now work as if they are not encrypted, but they will be encrypted when 
 they are committed to the repo.
       
## Status

  This is prototype work and by no means meant to be used in production. It misses important features, good
  error handling, user authentication, and extensive unit testing.

## License

  Dual licensed under EPL 1.0 & EDL 1.0.

