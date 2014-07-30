
Note: org.eclipse.flux.parent is the parent of all Flux Maven modules, including build modules. The
purpose of parent is to list common configuration like repositories, but should not contain
configuration that is specific to any particular module. 

For example, headless.releng project builds headless Eclipse. The list of Flux eclipse plug-ins and other maven projects
(like Flux feature and product projects) should be listed in headless.releng, not flux.parent.

Headless eclipse is built using tycho and by defining an Eclipse product. This product is defined in a separate maven
project, headless.product, from the headless build project, as the build project is an aggregator project, and needs to have a "pom" packaging,
which is different than the product project, which uses "eclipse-repository" packaging.


1. To build Flux headless Eclipse:

- Change directory into headless.releng directory
- run: mvn clean package
- Flux headless product will then appear in the target/products folder in the headless.product project

2. To edit the build by adding/removing dependencies:

- Edit the list of plug-ins in feature.xml in headless.feature project (or create a separate feature project and add that feature
to the list of features in the .product file in headless.product project).
- If necessary, add new repositories to resolve the plug-ins in flux.parent

3. To edit start levels of plug-ins:

- Edit the .product file in headless.product file.

4. To add new Flux plug-ins to the build that are Maven projects and should form part
of the headless product:

- In the new Flux plug-in, make sure the parent in the pom file uses relative path 
to point to flux.parent, which is the parent of all modules including those built by the headless Eclipse project.
- Add the module to the pom file in headless.releng


