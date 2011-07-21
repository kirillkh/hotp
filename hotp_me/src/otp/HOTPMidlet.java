package otp;

import otp.crypto.RijndaelEngine;
import otp.crypto.KeyParameter;
import java.io.UnsupportedEncodingException;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStoreException;

/**
 * License: GPL v3
 * Author: kirillkh
 * Original code: reverse-engineered OTP generator from HUJI, which is based on
 *                RFC4226 and BouncyCastle library (MIT license).
 * This project contributes the CounterScreen class, which allows to see and
 * modify the counter value.
 */
public class HOTPMidlet extends MIDlet implements CommandListener {
    public Command exitCmd;
    public Command generateCmd;
    public Command backCmd;

    public Command counterCmd;
    public Command okCmd;
    public Command cancelCmd;

    public SwitchableScreen generatorScreen;
    public CounterScreen counterScreen;

    public void startApp() {
        init();
    }

    public void pauseApp() {
        stop();
    }

    public void destroyApp(boolean paramBoolean) {
        stop();
        super.notifyDestroyed();
    }

    public void commandAction(Command cmd, Displayable displ) {
        try {
            if (displ == this.generatorScreen) {
                if (cmd == this.exitCmd)
                    destroyApp(false);
                else if (cmd == this.generateCmd)
                    generatorScreen.switchMode(SwitchableScreen.MODE_GENERATE);
                else if (cmd == this.backCmd)
                    generatorScreen.switchMode(SwitchableScreen.MODE_START);
                else if(cmd == this.counterCmd) {
                    setScreen(counterScreen);
                }
            } else if(displ == counterScreen) {
                if(cmd == cancelCmd) {
                    setScreen(generatorScreen);
                } else if(cmd == okCmd) {
                    setScreen(generatorScreen);
                    Config.setCounter(counterScreen.counter);
                }
            }
        } catch (Exception e) {
            error(e);
        }
    }

    private void init() {
        try {
            Config.init(this);
            if (this.generatorScreen == null) {
                this.exitCmd = new Command("Exit", Command.EXIT, 1);
                this.generateCmd = new Command("Generate", Command.OK, 1);
                this.backCmd = new Command("Back", Command.BACK, 1);
            }

            if(counterScreen == null) {
                counterCmd = new Command("Counter", Command.OK, 1);
                okCmd = new Command("OK", Command.OK, 1);
                cancelCmd = new Command("Cancel", Command.CANCEL, 1);
                counterScreen = new CounterScreen(this);
                counterScreen.setCommandListener(this);
            }

            // needed to init the CounterScreen commands before creating GeneratorScreen
            if(generatorScreen == null) {
                int pinLen = Config.getIntProperty(Config.PIN_LENGTH_KEY, 5);
                generatorScreen = new SwitchableScreen(this, pinLen);
                generatorScreen.setCommandListener(this);
            }
            setScreen(generatorScreen);
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
            error(ex);
        }
    }


    private void setScreen(Screen screen) {
        if(screen == generatorScreen)
            generatorScreen.switchMode(SwitchableScreen.MODE_START);
        else if(screen == counterScreen)
            counterScreen.resetCounter();

        Display.getDisplay(this).setCurrent(screen);
    }


    public final void stop() {
        try {
            Config.closeStore();
            if (this.generatorScreen != null) {
                this.generatorScreen.switchMode(SwitchableScreen.MODE_STOP);
                this.generatorScreen.removeCommand(this.exitCmd);
                this.generatorScreen.removeCommand(this.generateCmd);
                this.generatorScreen.removeCommand(this.backCmd);

                this.generatorScreen.removeCommand(this.counterCmd);
                this.generatorScreen.removeCommand(this.okCmd);
                this.generatorScreen.removeCommand(this.cancelCmd);

                this.exitCmd = null;
                this.generateCmd = null;
                this.backCmd = null;

                this.counterCmd = null;
                this.okCmd = null;
                this.cancelCmd = null;

                this.generatorScreen = null;
            }
        } catch (Exception ex) {
            error(ex);
        }
    }

    public final void error(Throwable t) {
        this.generatorScreen.handleError(t);
    }

    public static String generate(String pin)
            throws UnsupportedEncodingException, RecordStoreException {
        byte[] secret = Config.getByteProperty(Config.KEY_KEY);
        int digits = Config.getIntProperty(Config.DIGITS_KEY, 5);
        int base = Config.getIntProperty(Config.BASE_KEY, 10);
        int trunc = Config.getIntProperty(Config.TRUNC_KEY, -1);

        String str = "blablablabla";
        long counter = Config.counterInc();
        pin += str.substring(0, 16 - pin.length());
        byte[] pinArr = pin.getBytes("ISO8859_1");
        
        RijndaelEngine engine = new RijndaelEngine(128);
        engine.init(false, new KeyParameter(pinArr));
        for (int i=0; i<secret.length; i+=16) {
            engine.processBlock(secret, i, secret, i);
        }
        return new Generator(secret, digits, base, trunc).generate(counter);
    }
}
