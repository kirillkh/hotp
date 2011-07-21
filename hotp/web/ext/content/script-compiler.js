
var moodleotp_gmCompiler={
	// getUrlContents adapted from Greasemonkey Compiler
	// http://www.letitblog.com/code/python/greasemonkey.py.txt
	// used under GPL permission
	//
	// most everything else below based heavily off of Greasemonkey
	// http://greasemonkey.devjavu.com/
	// used under GPL permission

	getUrlContents: function(aUrl){
	    var ioService=Components.classes["@mozilla.org/network/io-service;1"]
		.getService(Components.interfaces.nsIIOService);
	    var scriptableStream=Components
		.classes["@mozilla.org/scriptableinputstream;1"]
		.getService(Components.interfaces.nsIScriptableInputStream);
	    var unicodeConverter=Components
		.classes["@mozilla.org/intl/scriptableunicodeconverter"]
		.createInstance(Components.interfaces.nsIScriptableUnicodeConverter);
	    unicodeConverter.charset="UTF-8";

	    var channel=ioService.newChannel(aUrl, null, null);
	    var input=channel.open();
	    scriptableStream.init(input);
	    var str=scriptableStream.read(input.available());
	    scriptableStream.close();
	    input.close();

	    try {
		return unicodeConverter.ConvertToUnicode(str);
	    } catch (e) {
		return str;
	    }
	},

	isGreasemonkeyable: function(url) {
	    var scheme=Components.classes["@mozilla.org/network/io-service;1"]
		.getService(Components.interfaces.nsIIOService)
		.extractScheme(url);
	    return ((scheme == "http" || scheme == "https" || scheme == "file") &&
		!/hiddenWindow\.html$/.test(url));
	},

	contentLoad: function(e) {
	    var unsafeWin=e.target.defaultView;
	    if (unsafeWin.wrappedJSObject) unsafeWin=unsafeWin.wrappedJSObject;

	    var unsafeLoc=new XPCNativeWrapper(unsafeWin, "location").location;
	    var href=new XPCNativeWrapper(unsafeLoc, "href").href;

	    if (moodleotp_gmCompiler.isGreasemonkeyable(href)
    		&& ( (/^http:\/\/httpdyn\.cs\.huji\.ac\.il\/moodles\/.*\/login\/index\.php/.test(href)) ||
		         (/^http:\/\/moodle\.cs\.huji\.ac\.il\/.*\/login\/index\.php/.test(href)) )) {
		var script=moodleotp_gmCompiler.getUrlContents('chrome://moodleotp/content/moodleotp.js');
		moodleotp_gmCompiler.injectScript(script, href, unsafeWin);
	    }
	},

	injectScript: function(script, url, unsafeContentWin) {
	    var sandbox, script, logger, storage, xmlhttpRequester;
	    var safeWin=new XPCNativeWrapper(unsafeContentWin);

//	    sandbox=new Components.utils.Sandbox(safeWin);
	    var clazz = function() {
		this.invoke = function() {
		};
	    };
	    sandbox = new clazz();

	    var storage=new moodleotp_ScriptStorage();
//	    xmlhttpRequester=new moodleotp_xmlhttpRequester(unsafeContentWin, window//appSvc.hiddenDOMWindow);
	    xmlhttpRequester=new moodleotp_xmlhttpRequester(unsafeContentWin, window);

	    sandbox.window=safeWin;
	    sandbox.document=sandbox.window.document;
	    sandbox.unsafeWindow=unsafeContentWin;

	    // patch missing properties on xpcnw
	    sandbox.XPathResult=Components.interfaces.nsIDOMXPathResult;

	    // add our own APIs
	    sandbox.GM_addStyle=function(css) { moodleotp_gmCompiler.addStyle(sandbox.document, css) };
	    sandbox.GM_setValue=moodleotp_gmCompiler.hitch(storage, "setValue");
	    sandbox.GM_getValue=moodleotp_gmCompiler.hitch(storage, "getValue");
	    sandbox.GM_openInTab=moodleotp_gmCompiler.hitch(this, "openInTab", unsafeContentWin);
	    sandbox.GM_xmlhttpRequest=moodleotp_gmCompiler.hitch(xmlhttpRequester, "contentStartRequest");
	    //unsupported
	    sandbox.GM_registerMenuCommand=function(){};
	    sandbox.GM_log=function(){};
	    sandbox.GM_getResourceURL=function(){};
	    sandbox.GM_getResourceText=function(){};

	    sandbox.__proto__=sandbox.window;

	    try {
//		this.evalInSandbox(
//		    "(function(){"+script+"})()",
//		    url,
//		    sandbox);
//		sandbox.invoke();
		with(sandbox) { eval("(function(){" + script + "}) ()"); }
	    } catch (e) {
		var e2=new Error(typeof e=="string" ? e : e.message);
		e2.fileName=script.filename;
		e2.lineNumber=0;
		//GM_logError(e2);
		alert(e2);
	    }
	},

	evalInSandbox: function(code, codebase, sandbox) {
	    if (Components.utils && Components.utils.Sandbox) {
		// DP beta+
		Components.utils.evalInSandbox(code, sandbox);
	    } else if (Components.utils && Components.utils.evalInSandbox) {
		// DP alphas
		Components.utils.evalInSandbox(code, codebase, sandbox);
	    } else if (Sandbox) {
		// 1.0.x
		evalInSandbox(code, sandbox, codebase);
	    } else {
		throw new Error("Could not create sandbox.");
	    }
	},
	
	openInTab: function(unsafeContentWin, url) {
	    var tabBrowser = getBrowser(), browser, isMyWindow = false;
	    for (var i = 0; browser = tabBrowser.browsers[i]; i++)
		if (browser.contentWindow == unsafeContentWin) {
		    isMyWindow = true;
		    break;
		}
	    if (!isMyWindow) return;
	 
	    var loadInBackground, sendReferrer, referrer = null;
	    loadInBackground = tabBrowser.mPrefs.getBoolPref("browser.tabs.loadInBackground");
	    sendReferrer = tabBrowser.mPrefs.getIntPref("network.http.sendRefererHeader");
	    if (sendReferrer) {
		var ios = Components.classes["@mozilla.org/network/io-service;1"]
		                    .getService(Components.interfaces.nsIIOService);
		referrer = ios.newURI(content.document.location.href, null, null);
	    }
	    tabBrowser.loadOneTab(url, referrer, null, null, loadInBackground);
	},

	// NF: is this even used? there's a hitch below!
	hitch: function(obj, meth) {
	    var unsafeTop = new XPCNativeWrapper(unsafeContentWin, "top").top;

	    // NF: huh????? in ALL of them????
	    for (var i = 0; i < this.browserWindows.length; i++) {
		// NF: WTF is url? no such param
		this.browserWindows[i].openInTab(unsafeTop, url);
	    }
	},

	apiLeakCheck: function(allowedCaller) {
	    var stack=Components.stack;

	    var leaked=false;
	    do {
		if (2==stack.language) {
		    if ('chrome'!=stack.filename.substr(0, 6) &&
		        allowedCaller!=stack.filename 
		    ) {
		        leaked=true;
		        break;
		    }
		}

		stack=stack.caller;
	    } while (stack);

	    return leaked;
	},

	hitch: function(obj, meth) {
	    if (!obj[meth]) {
		throw "method '" + meth + "' does not exist on object '" + obj + "'";
	    }

	    var hitchCaller=Components.stack.caller.filename;
	    var staticArgs = Array.prototype.splice.call(arguments, 2, arguments.length);

	    return function() {
		if (moodleotp_gmCompiler.apiLeakCheck(hitchCaller)) {
		    return;
		}
		
		// make a copy of staticArgs (don't modify it because it gets reused for
		// every invocation).
		var args = staticArgs.concat();

		// add all the new arguments
		for (var i = 0; i < arguments.length; i++) {
		    args.push(arguments[i]);
		}

		// invoke the original function with the correct this obj and the combined
		// list of static and dynamic arguments.
		return obj[meth].apply(obj, args);
	    };
	},

	addStyle:function(doc, css) {
	    var head, style;
	    head = doc.getElementsByTagName('head')[0];
	    if (!head) { return; }
	    style = doc.createElement('style');
	    style.type = 'text/css';
	    style.innerHTML = css;
	    head.appendChild(style);
	},

	onLoad: function() {
	    var appcontent=window.document.getElementById("appcontent");
	    if (appcontent && !appcontent.greased_moodleotp_gmCompiler) {
		appcontent.greased_moodleotp_gmCompiler=true;
		appcontent.addEventListener("DOMContentLoaded", moodleotp_gmCompiler.contentLoad, false);
	    }
	},

	onUnLoad: function() {
	    //remove now unnecessary listeners
	    window.removeEventListener('load', moodleotp_gmCompiler.onLoad, false);
	    window.removeEventListener('unload', moodleotp_gmCompiler.onUnLoad, false);
	    window.document.getElementById("appcontent")
		.removeEventListener("DOMContentLoaded", moodleotp_gmCompiler.contentLoad, false);
	},
}; //object moodleotp_gmCompiler


function moodleotp_ScriptStorage() {
    this.prefMan=new moodleotp_PrefManager();
}
moodleotp_ScriptStorage.prototype.setValue = function(name, val) {
    this.prefMan.setValue(name, val);
}
moodleotp_ScriptStorage.prototype.getValue = function(name, defVal) {
    return this.prefMan.getValue(name, defVal);
}


window.addEventListener('load', moodleotp_gmCompiler.onLoad, false);
window.addEventListener('unload', moodleotp_gmCompiler.onUnLoad, false);
