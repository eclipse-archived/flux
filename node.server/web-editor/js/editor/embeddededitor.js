/*******************************************************************************
 * @license
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * Copyright (c) 2012 VMware, Inc.
 * Copyright (c) 2013, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html).
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andrew Eisenberg - rename jsContentAssist to jsTemplateContentAssist
 *     Martin Lippert - flight prototype work
 *******************************************************************************/
/*global examples orion:true window define*/
/*jslint browser:true devel:true*/

define([
	"require",
	"socket",
	"orion/editor/textView",
	"orion/keyBinding",
	"editor/textview/textStyler",
	"orion/editor/textMateStyler",
	"orion/editor/htmlGrammar",
	"orion/editor/editor",
	"orion/editor/editorFeatures",
	"orion/editor/contentAssist",
	"editor/javaContentAssist",
	"orion/editor/linkedMode",
	"editor/sha1"],

function(require, socket, mTextView, mKeyBinding, mTextStyler, mTextMateStyler, mHtmlGrammar, mEditor, mEditorFeatures, mContentAssist, mJavaContentAssist, mLinkedMode) {
	var editorDomNode = document.getElementById("editor");

	var textViewFactory = function() {
		return new mTextView.TextView({
			parent: editorDomNode,
			tabSize: 4
		});
	};

	var contentAssist;
	var contentAssistFactory = {
		createContentAssistMode: function(editor) {
			contentAssist = new mContentAssist.ContentAssist(editor.getTextView());
			var contentAssistWidget = new mContentAssist.ContentAssistWidget(contentAssist);
			var result = new mContentAssist.ContentAssistMode(contentAssist, contentAssistWidget);
			contentAssist.setMode(result);
			return result;
		}
	};

	socket.on('connect', function() {
		connected();
	});

	socket.on('disconnect', function() {
		disconnected();
	});

	var javaContentAssistProvider = new mJavaContentAssist.JavaContentAssistProvider(socket);
	javaContentAssistProvider.setSocket(socket);

	// Canned highlighters for js, java, and css. Grammar-based highlighter for html
	var syntaxHighlighter = {
		styler: null,

		highlight: function(fileName, editor) {
			if (this.styler) {
				this.styler.destroy();
				this.styler = null;
			}
			if (fileName) {
				var splits = fileName.split(".");
				var extension = splits.pop().toLowerCase();
				var textView = editor.getTextView();
				var annotationModel = editor.getAnnotationModel();
				if (splits.length > 0) {
					switch(extension) {
						case "js":
						case "java":
						case "class":
						case "css":
							this.styler = new mTextStyler.TextStyler(textView, extension, annotationModel);
							break;
						case "html":
							this.styler = new mTextMateStyler.TextMateStyler(textView, new mHtmlGrammar.HtmlGrammar());
							break;
					}
				}
			}
		}
	};

	var annotationFactory = new mEditorFeatures.AnnotationFactory();

	var linkedMode;

	var keyBindingFactory = function(editor, keyModeStack, undoStack, contentAssist) {

		// Create keybindings for generic editing
		var genericBindings = new mEditorFeatures.TextActions(editor, undoStack);
		keyModeStack.push(genericBindings);

		// Linked Mode
		linkedMode = new mLinkedMode.LinkedMode(editor, undoStack, contentAssist);
		keyModeStack.push(linkedMode);

		// create keybindings for source editing
		var codeBindings = new mEditorFeatures.SourceCodeActions(editor, undoStack, contentAssist, linkedMode);
		keyModeStack.push(codeBindings);

		// save binding
		editor.getTextView().setKeyBinding(new mKeyBinding.KeyBinding("s", true), "save");
		editor.getTextView().setAction("save", function(){
				save(editor);
				return true;
		});

		//Navigate to declaration (F3)
		editor.getTextView().setKeyBinding(new mKeyBinding.KeyBinding(114), "navigate");
		editor.getTextView().setAction("navigate", function(){
				navigate(editor);
				return true;
		});

		//Rename in file (F4)
		editor.getTextView().setKeyBinding(new mKeyBinding.KeyBinding(115), "renameinfile");
		editor.getTextView().setAction("renameinfile", function(){
				renameInFile(editor);
				return true;
		});

	};

	var dirtyIndicator = "";
	var status = "";

	var statusReporter = function(message, isError) {
		/*if (isError) {
			status =  "ERROR: " + message;
		} else {
			status = message;
		}
		document.getElementById("status").innerHTML = dirtyIndicator + status;*/
	};

	var editor = new mEditor.Editor({
		textViewFactory: textViewFactory,
		undoStackFactory: new mEditorFeatures.UndoFactory(),
		annotationFactory: annotationFactory,
		lineNumberRulerFactory: new mEditorFeatures.LineNumberRulerFactory(),
		contentAssistFactory: contentAssistFactory,
		keyBindingFactory: keyBindingFactory,
		statusReporter: statusReporter,
		domNode: editorDomNode
	});

	editor.addEventListener("DirtyChanged", function(evt) {
		if (editor.isDirty()) {
			dirtyIndicator = "*";
			window.document.title = "*" + fileShortName;
		} else {
			dirtyIndicator = "";
			window.document.title = fileShortName;
		}

		// alert("Dirty changes: " + editor.__javaObject);
		// document.getElementById("status").innerHTML = dirtyIndicator + status;
	});

	editor.installTextView();

	contentAssist.addEventListener("Activating", function() {
		contentAssist.setProviders([javaContentAssistProvider]);
	});

	window.onbeforeunload = function() {
		if (editor.isDirty()) {
			 return "There are unsaved changes.";
		}
	};

	window.onhashchange = function() {
		console.log("hash changed: " + window.location.hash);
		start();
	};

	socket.on('liveMetadataChanged', function (data) {
		if (username === data.username && project === data.project && resource === data.resource && data.problems !== undefined) {
			var markers = [];
			var i;
			for(i = 0; i < data.problems.length; i++) {
				var lineOffset = editor.getModel().getLineStart(data.problems[i].line - 1);

				console.log(lineOffset);

				markers[i] = {
					'description' : data.problems[i].description,
					'line' : data.problems[i].line,
					'severity' : data.problems[i].severity,
					'start' : (data.problems[i].start - lineOffset) + 1,
					'end' : data.problems[i].end - lineOffset
				};
			}

			editor.showProblems(markers);
		}
		console.log(data);
	});

	socket.on('navigationresponse', function (data) {
		if (username === data.username && project === data.project && resource === data.resource && data.navigation !== undefined) {
			var navigationTarget = data.navigation;
			if (navigationTarget.project === project && navigationTarget.resource === resource) {
				var offset = navigationTarget.offset;
				var length = navigationTarget.length;

				editor.setSelection(offset, offset + length, true);
			}
			else {
				var baseURL = window.location.origin + window.location.pathname;
				var resourceID = data.username + "/" + navigationTarget.project + "/" + navigationTarget.resource;

				if (navigationTarget.offset !== undefined) {
					resourceID += '#offset=' + navigationTarget.offset;
				}

				if (navigationTarget.length !== undefined) {
					resourceID += '#length=' + navigationTarget.length;
				}

				window.location.hash = resourceID;
			}
		}
		console.log(data);
	});

	socket.on('renameinfileresponse', function (data) {
		if (username === data.username && project === data.project && resource === data.resource && data.references !== undefined) {
			var references = data.references;

			var positionGroups = [];
			positionGroups.push({
				'positions' : references
			});

			var linkedModeModel = {
				groups: positionGroups,
				escapePosition: 0
			};
			linkedMode.enterLinkedMode(linkedModeModel);
		}
		console.log(data);
	});

	var username = "defaultuser";

	var filePath;
	var project;
	var resource;
	var fileShortName;

	var jumpTo;

	var lastSavePointContent = '';
	var lastSavePointHash = '';
	var lastSavePointTimestamp = 0;

	function connected() {
		if (username) {
			socket.emit('connectToChannel', {
				'channel' : username
			}, function(answer) {
			});
		}
	}

	function disconnected() {
	}

	function start() {
		console.log('Starting');
		filePath = window.location.href.split('#')[1];

		if (filePath !== undefined) {
			var sections = filePath.split('/');
			var newUsername = sections[0] || "defaultuser";
			var newProject = sections[1];
			sections.splice(0,2);
			var newResource = sections.join('/');

			if (newUsername !== username || newProject !== project || newResource !== resource) {
				if (username !== undefined && newUsername !== username) {
					socket.emit('disconnectFromChannel', {
						'channel' : username
					});
				}

				username = newUsername;
				project = newProject;
				resource = newResource;
				fileShortName = sections[sections.length - 1];
				jumpTo = extractJumpToInformation(window.location.hash);

				editor.getTextView().removeEventListener("ModelChanged", sendModelChanged);

				lastSavePointContent = '';
				lastSavePointHash = '';
				lastSavePointTimestamp = 0;

				socket.emit('connectToChannel', {
					'channel' : username
				}, function(answer) {
					if (answer.connectedToChannel) {
						console.log({
							'callback_id' : 0,
							'username' : username,
							'project' : project,
							'resource' : resource
						});
						socket.emit('getResourceRequest', {
							'callback_id' : 0,
							'username' : username,
							'project' : project,
							'resource' : resource
						});
					}
				});
			}
			else {
				jumpTo = extractJumpToInformation(window.location.hash);
				jump(jumpTo);
			}
		}
	}

	function extractJumpToInformation(hash) {
		var hashValues = hash.split('#');
		var offset;
		var length;

		var i;
		for (i = 0; i < hashValues.length; i++) {
			var param = hashValues[i];
			var pieces = param.split('=');
			if (pieces.length == 2) {
				if (pieces[0] === 'offset' && !isNaN(parseInt(pieces[1]))) {
					offset = parseInt(pieces[1]);
				}
				else if (pieces[0] === 'length' && !isNaN(parseInt(pieces[1]))) {
					length = parseInt(pieces[1]);
				}
			}
		}

		if (offset !== undefined) {
			return {
				'offset' : offset,
				'length' : (length !== undefined ? length : 0)
			};
		}
	}

	function jump(selection) {
		if (selection !== undefined) {
			editor.setSelection(selection.offset, selection.offset + selection.length, true);
		}
	}

	start();

	socket.on('getResourceResponse', function(data) {
		if (lastSavePointTimestamp !== 0 && lastSavePointHash !== '') {
			return;
		}

		if (data.username !== username) {
			return;
		}

		var text = data.content;

		editor.setInput(fileShortName, null, text);
		syntaxHighlighter.highlight(fileShortName, editor);
		window.document.title = fileShortName;

		if (data.readonly) {
			editor.getTextView().setOptions({'readonly' : true});
		}
		else {
			editor.getTextView().setOptions({'readonly' : false});
		}

		javaContentAssistProvider.setProject(project);
		javaContentAssistProvider.setResourcePath(resource);
		javaContentAssistProvider.setUsername(username);

		lastSavePointContent = text;
		lastSavePointHash = data.hash;
		lastSavePointTimestamp = data.timestamp;

		jump(jumpTo);

		socket.emit('liveResourceStarted', {
			'callback_id' : 0,
			'username' : username,
			'project' : project,
			'resource' : resource,
			'hash' : lastSavePointHash,
			'timestamp' : lastSavePointTimestamp
		});

		editor.getTextView().addEventListener("ModelChanged", sendModelChanged);
	});

	socket.on('liveResourceStartedResponse', function(data) {
		if (data.username === username && data.project === project && data.resource === resource && data.callback_id !== undefined) {
			if (lastSavePointTimestamp === data.savePointTimestamp && lastSavePointHash === data.savePointHash) {
				var currentEditorContent = editor.getText();
				var currentEditorContentHash = CryptoJS.SHA1(currentEditorContent).toString(CryptoJS.enc.Hex);

				if (currentEditorContentHash === data.savePointHash) {
					editor.getTextView().removeEventListener("ModelChanged", sendModelChanged);
					editor.getModel().setText(data.liveContent);
					editor.getTextView().addEventListener("ModelChanged", sendModelChanged);
				}
			}
		}
	});

	socket.on('liveResourceStarted', function(data) {
		if (data.username === username && data.project === project && data.resource === resource && data.callback_id !== undefined) {

			if ((data.hash === undefined || data.hash === lastSavePointHash) &&
					data.timestamp === undefined || data.timestamp === lastSavePointTimestamp) {

				socket.emit('liveResourceStartedResponse', {
					'callback_id'        : data.callback_id,
					'requestSenderID'    : data.requestSenderID,
					'username'           : data.username,
					'project'            : data.project,
					'resource'           : data.resource,
					'savePointTimestamp' : lastSavePointTimestamp,
					'savePointHash'      : lastSavePointHash,
					'liveContent'        : editor.getText()
				});
			}
		}
	});

	socket.on('getLiveResourcesRequest', function(data) {
		if (data.callback_id
				&& (!data.projectRegEx || new RegExp(data.projectRegEx).test(project))
				&& (!data.resourceRegEx || new RegExp(data.resourceRegEx).test(resource))) {
			var liveEditUnits = {};
			liveEditUnits[project] = [{
				'resource'           : resource,
				'savePointTimestamp' : lastSavePointTimestamp,
				'savePointHash'      : lastSavePointHash
			}];
			socket.emit('getLiveResourcesResponse', {
				'callback_id'        : data.callback_id,
				'liveEditUnits'      : liveEditUnits,
				'requestSenderID'    : data.requestSenderID,
				'username'           : username
			});
		}
	});

	function sendModelChanged(evt) {
		var changeData = {
			'username' : username,
			'project' : project,
			'resource' : resource,
			'offset' : evt.start,
			'removedCharCount' : evt.removedCharCount
		};

		if (evt.addedCharCount > 0) {
			changeData.addedCharacters = editor.getModel().getText(evt.start, evt.start + evt.addedCharCount);
		}
		else {
			changeData.addedCharacters = "";
		}

		socket.emit('liveResourceChanged', changeData);
	}

	socket.on('liveResourceChanged', function(data) {
		if (data.username === username && data.project === project && data.resource === resource) {
			var text = data.addedCharacters !== undefined ? data.addedCharacters : "";
			editor.getTextView().removeEventListener("ModelChanged", sendModelChanged);
			editor.getModel().setText(text, data.offset, data.offset + data.removedCharCount);
			editor.getTextView().addEventListener("ModelChanged", sendModelChanged);
		}
	});

	socket.on('getResourceRequest', function(data) {
		if (data.username === username && data.project === project && data.resource === resource && data.callback_id !== undefined) {

			if ((data.hash === undefined || data.hash === lastSavePointHash) &&
					data.timestamp === undefined || data.timestamp === lastSavePointTimestamp) {

				socket.emit('getResourceResponse', {
					'callback_id'       : data.callback_id,
					'requestSenderID'   : data.requestSenderID,
					'username'          : data.username,
					'project'           : project,
					'resource'          : resource,
					'timestamp'         : lastSavePointTimestamp,
					'hash'              : lastSavePointHash,
					'content'           : lastSavePointContent
				});
			}
		}
	});

	socket.on('resourceStored', function(data) {
		if (data.username === username && data.project === project && data.resource === resource) {

			var currentEditorContent = editor.getText();
			var currentEditorContentHash = CryptoJS.SHA1(currentEditorContent).toString(CryptoJS.enc.Hex);

			if (data.hash === currentEditorContentHash) {
				lastSavePointContent = currentEditorContent;
				lastSavePointHash = data.hash;
				lastSavePointTimestamp = data.timestamp;
				editor.setDirty(false);
			}
		}
	});

	function save(editor) {
		setTimeout(function() {
			lastSavePointContent = editor.getText();
			lastSavePointHash = CryptoJS.SHA1(lastSavePointContent).toString(CryptoJS.enc.Hex);
			lastSavePointTimestamp = Date.now();

			socket.emit('resourceChanged', {
				'username' : username,
				'project' : project,
				'resource' : resource,
				'timestamp' : lastSavePointTimestamp,
				'hash' : lastSavePointHash
			});

			// we don't reset the dirty flag here because we don't know whether this resource will
			// be stored by another participant in the system. Instead we wait for the "resourceStored"
			// message to arrive
		}, 0);
	}

	function navigate(editor) {
		setTimeout(function() {
			var selection = editor.getSelection();
			var offset = selection.start;
			var length = selection.end - selection.start;

			socket.emit('navigationrequest', {
				'username' : username,
				'project' : project,
				'resource' : resource,
				'offset' : offset,
				'length' : length,
				'callback_id' : 0
			});
		}, 0);
	}

	function renameInFile(editor) {
		setTimeout(function() {
			var selection = editor.getSelection();
			var offset = selection.start;
			var length = selection.end - selection.start;

			socket.emit('renameinfilerequest', {
				'username' : username,
				'project' : project,
				'resource' : resource,
				'offset' : offset,
				'length' : length,
				'callback_id' : 0
			});
		}, 0);
	}
});
