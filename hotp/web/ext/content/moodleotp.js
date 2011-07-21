// ==UserScript==
// @name           moodle-otp
// @namespace      huji
// @description    Fills in the OTP password, that is required to log into the Moodle system.
// @include        http://httpdyn.cs.huji.ac.il/moodles/*/login/index.php
// @include        http://moodle.cs.huji.ac.il/*/login/index.php
// ==/UserScript==

(function() {
    var prefman = new moodleotp_PrefManager();

    var sh_switch = prefman.getValue("shell", "/bin/sh -c").split(" ");
    var sh = sh_switch[0];
    var cmd_switches = sh_switch.slice(1);
    
    var hotp_jar = prefman.getValue("hotp_jar", "~/" + "\".ssh/otp/hotp.jar\"");
    var conf = prefman.getValue("conf", "");

    
    function fill() {
        var passField = document.getElementById("password");
        var otp = generate();
        passField.value = otp;
    
        var form = unsafeWindow.document.getElementById('login');
        form.onsubmit = increment;
    };

    hotp = function(cmd) {
        var temp_file = null;
        try {
            // create a nsILocalFile for the executable
            var sh_file = createInstance("@mozilla.org/file/local;1", Components.interfaces.nsILocalFile);
            sh_file.initWithPath(sh);
            if(!sh_file.exists() || !sh_file.isExecutable())
                return null;

            // create a nsIProcess
            var process = createInstance("@mozilla.org/process/util;1", Components.interfaces.nsIProcess);
            process.init(sh_file);
        
            // Run the process.
            var args = cmd_switches.concat(["java -jar " + hotp_jar + " " + cmd]);
            process.run(true, args, args.length);
            return process.exitValue;
        } catch(e) {
            // do nothing for now
            return -1;
        }
    };

    generate = function() {
        var otp = null;
        try {
            // temp file
            temp_file = getService("@mozilla.org/file/directory_service;1", Components.interfaces.nsIProperties).
                                 get("TmpD", Components.interfaces.nsIFile);
            temp_file.append("hotp.tmp");
            temp_file.createUnique(Components.interfaces.nsIFile.NORMAL_FILE_TYPE, 0600);

            var exit_val = hotp("make " + conf + " > " + temp_file.path);

            if(exit_val == 0)
                otp = read_otp(temp_file);
        } catch(e) {
            // do nothing for now
        }

        try {
            if(temp_file != null && temp_file.exists())
                temp_file.remove(false);
        } catch(e) {
        }

        return otp;
    };
    
    increment = function() {
        hotp("inc " + conf);
    };

    // we use a temporary file to transfer the otp from the generator; this is currently the only way to do it from JS
    read_otp = function(file) {
        var otp = null;
        var fstream = createInstance("@mozilla.org/network/file-input-stream;1", Components.interfaces.nsIFileInputStream);
        var cstream = createInstance("@mozilla.org/intl/converter-input-stream;1", Components.interfaces.nsIConverterInputStream);
        try {
            fstream.init(file, -1, 0, 0);
            cstream.init(fstream, "UTF-8", 0, 0);

            str = {};
            cstream.readString(-1, str); // read the whole file and put it in str.value
            otp = str.value;
        } catch(e) {
            // ignore for now...
        }
        try {
            cstream.close();
        } catch(e) {
        }

        return otp;
    };

    createInstance = function(clazz, iface) {
        return Components.classes[clazz].createInstance(iface);
    };
    
    getService = function(clazz, iface) {
        return Components.classes[clazz].getService(iface);
    };
    
    fill();
}) ();

