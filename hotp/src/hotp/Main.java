package hotp;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.lang.System.exit;
import static java.lang.System.out;
import static hotp.Main.Prop.*;

/**
 * License: GPL v3
 * Author: kirillkh
 * Original code: reverse-engineered OTP generator from HUJI, which is based on
 *                RFC4226 and BouncyCastle library (MIT license).
 * The reverse-engineered OTP code is in the Generator class. Everything else
 * in the package, excluding Main, belongs to the BouncyCastle.
 */
public class Main {
    static public enum Prop {
        // conf-only properties
        PIN("pin"), ORIG_COUNTER("orig-counter"),
        // JAD properties
        COUNTER("HOTP-Counter"), DIGITS("HOTP-Digits"), SECRET("HOTP-Key");

        public final String key;

        Prop(String key) {
            this.key = key;
        }
    }
    
    static private final String HOME_CONF = ".hotp.conf";


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws UnsupportedEncodingException {
        if(args.length == 0 || args[0].equals("make")) {
            handleMake(args);
        } else if(args[0].equals("inc")) {
            handleInc(args);
        } else if(args[0].equals("set")) {
            handleSet(args);
        } else if(args[0].equals("init")) {
            handleInit(args);
        } else if(args[0].equals("show")) {
            handleShow(args);
        } else {
            usage();
        }
    }


    static private void handleInit(String[] args) {
        if(args.length < 3 || 4 < args.length)
            usage();

        String pin = args[1];
        String jad = args[2];
        String conf = (args.length == 4 ? args[3] : homeConf());
        initConf(pin, jad, conf);
    }
    

    static private void handleMake(String[] args) {
        if(args.length > 3)
            usage();
        
        boolean inc=false;
        String target = null;
        for(int i=1; i<args.length; i++) {
            if(args[i].equals("-i"))
                inc = true;
            else if(i == args.length-1)
                target = args[i];
            else
                usage();
        }

        if(target == null)
            target = homeConf();

        String otp = make(target);
        out.println(otp);
        out.flush();
        if(inc)
            inc(target);
    }

    static private void handleInc(String[] args) {
        if(args.length > 3)
            usage();
        
        boolean print=true;
        String target = null;
        for(int i=1; i<args.length; i++) {
            if(args[i].equals("-np"))
                print = false;
            else if(i == args.length-1)
                target = args[i];
            else
                usage();
        }

        if(target == null)
            target = homeConf();

        String counter = inc(target);
        if(print)
            out.println(counter);
    }


    static private void handleSet(String[] args) {
        if(args.length < 2 || 3 < args.length)
            usage();

        String counter = args[1];
        String conf = (args.length == 2 ? homeConf() : args[2]);

        Map<String,String> map = parseConf(conf, true);
        put(map, COUNTER, counter);
        updateConf(map, conf);
    }


    static private void handleShow(String[] args) {
        if(args.length < 2 || 3 < args.length)
            usage();

        Prop prop = null;
        if(args[1].equals("curr"))
            prop = COUNTER;
        else if(args[1].equals("orig"))
            prop = ORIG_COUNTER;
        else
            usage();

        String conf = (args.length == 2 ? homeConf() : args[2]);

        Map<String,String> map = parseConf(conf, true);
        out.println(get(map, prop));
    }


    static private void initConf(String pin, String jad, String conf) {
        Map<String,String> map = parseConf(jad, false);
        put(map, PIN, pin);
        put(map, ORIG_COUNTER, get(map, COUNTER));
        validateConf(map, "JAD");
        updateConf(map, conf);
    }


    static private String make(String conf) {
        Map<String,String> map = parseConf(conf, true);
        try {
            long counter = parseHex(get(map, COUNTER));
            int digits = Integer.parseInt(get(map, DIGITS));
            String secret = get(map, SECRET);
            String pin = get(map, PIN);

            return new Generator(secret, digits, -1, pin).generate(counter);
        } catch(NumberFormatException ex) {
            return error("wrong format in conf", ex, null);
        }
    }

    static private String inc(String conf) {
        Map<String,String> map = parseConf(conf, true);
        try {
            long counter = parseHex(get(map, COUNTER)) + 1;
            String hex = Long.toHexString(counter);
            put(map, COUNTER, hex);
            updateConf(map, conf);
            return hex;
        } catch(NumberFormatException ex) {
            return error("wrong format in conf", ex, null);
        }
    }


    static private void updateConf(Map<String,String> map, String conf) {
        try {
            File tmp = File.createTempFile("hotp.conf", null);
            writeConf(map, tmp);
            File confFile = new File(conf);
            File confOld = new File(conf + "~");
            confFile.renameTo(confOld);
            for(int i=0; i<20 && confFile.exists(); i++) {
                Thread.sleep(50);
            }
            if(confFile.exists())
                throw new IOException("can't delete the old file");
            tmp.renameTo(confFile);
        } catch(IOException ioe) {
            error("can't write conf", ioe, null);
        } catch(InterruptedException ie) {
        }
    }

    static private void usage() {
        out.println("Usage:");
        out.println("hotp init <PIN> <SOURCE_JAD> [CONF]");
        out.println("    creates CONF with data from SOURCE_JAD and with the specified PIN.");
        out.println();
        out.println("hotp [make [-i] [CONF]]");
        out.println("    generates the OTP");
        out.println("    If -i is passed, increments the counter.");
        out.println();
        out.println("hotp show <curr|orig> [CONF]");
        out.println("    prints either current, or original counter value.");
        out.println();
        out.println("hotp set <COUNTER> [CONF]");
            out.println("    resets the counter in the CONF file to the specified value.");
        out.println();
        out.println("hotp inc [-np] [CONF]");
        out.println("    increments the counter in the CONF file and prints it after incrementing.");
        out.println("    If -np is passed, doesn't print the new counter.");
        out.println();
        out.println("If no CONF is specified, the $HOME/" + HOME_CONF + " file will be used.");
        exit(0);
    }


    static private void put(Map<String,String> map, Prop prop, String val) {
        map.put(prop.key, val);
    }

    static private String get(Map<String,String> map, Prop prop) {
        return map.get(prop.key);
    }


    static private Map<String,String> parseConf(String conf, boolean validate) {
        BufferedReader in = null;
        Map<String,String> map = new TreeMap<String,String>();
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(conf), Charset.forName("UTF8")));
            for(int i=0; ; i++) {
                String line = in.readLine();
                if(line == null)
                    break;
                if(!parseLine(line, map))
                    error("wrong conf format at line" + i, null, null);
            }
        } catch(EOFException ex) {
        } catch(IOException ex) {
            return error("can't read conf", ex, in);
        } finally {
            close(in);
        }

        if(validate)
            validateConf(map, "conf");
        return map;
    }


    static private void validateConf(Map<String,String> map, String source) {
        for(Prop prop : values()) {
            if(!map.containsKey(prop.key))
                error("invalid " + source + ": the " + prop.key + " property is not present", null, null);
        }
    }


    static private void writeConf(Map<String,String> map, File conf) throws IOException {
        PrintStream out = null;
        try {
            out = new PrintStream(conf);
            for(Map.Entry<String,String> e : map.entrySet()) {
                out.println(e.getKey() + ": " + e.getValue());
            }
        } finally {
            close(out);
        }
    }


    static Pattern ptn = Pattern.compile("^\\s*([^:]*[^\\s:])\\s*:\\s*(.*)\\s*$");

    static boolean parseLine(String line, Map<String,String> map) {
        Matcher mtc = ptn.matcher(line);
        if(!mtc.matches())
            return false;
        String key = mtc.group(1);
        String val = mtc.group(2);
        if(val.trim().equals(""))
            return false;
        map.put(key, val);
        return true;
    }


    static private String homeConf() {
        return System.getProperty("user.home") + "/" + HOME_CONF;
    }


    static <R> R error(String msg, Throwable t, Closeable c) {
        System.err.println(msg);
        if(t != null)
            t.printStackTrace();
        close(c);
        try {
            File err = File.createTempFile("hotp.err", "tmp");
            PrintStream os = new PrintStream(err);
            os.print(msg);
            if(t != null)
                t.printStackTrace(os);
            close(os);
        } catch(IOException e) {
        }
        exit(1);
        return null;
    }

    static void close(Closeable c) {
        if(c != null) {
            try { c.close(); } catch(Throwable t2) {}
        }
    }

    static long parseHex(String num) {
        if ((num.startsWith("0x")) || (num.startsWith("0X")))
            num = num.substring(2);

        if(num.length() == 16) {
            // Long can't parse unsigned > 7f{15}
            long lo = Long.parseLong(num.substring(1), 16);
            long hi = Character.digit(num.charAt(0), 16);
            return (hi<<60) | lo;
        }

        return Long.parseLong(num, 16);
    }
}
