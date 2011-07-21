package otp;

import javax.microedition.lcdui.Display;

public final class SwitchableScreen extends Screen implements Runnable {
    static public final int MODE_START = 1;
    static public final int MODE_GENERATE = 2;
    static public final int MODE_ERROR = 3;
    static public final int MODE_READY_TO_GEN = 4;
    static public final int MODE_ENTER_PIN = 5;
    static public final int MODE_STOP = 6;

    public int currMode;
    public int pinLen;
    public HOTPMidlet midlet;
    public String resetTracking;
    public Timer timer;

    public SwitchableScreen(HOTPMidlet midlet, int pinLen) {
	super(true);
        this.midlet = midlet;
        this.pinLen = pinLen;
        this.timer = new Timer(this);
        new Thread(this.timer).start();
        resetTracking = null;
        super.update(null, null, 0);
        switchMode(MODE_START);
    }

    public final void keyPressed(int keyCode) {
        this.timer.reset();
        if (keyCode == '*')
            resetTracking = "";
        if (resetTracking != null) {
            resetTracking += (char) keyCode;
            if (resetTracking.equals("*12345#")) {
                try {
                    Config.reset();
                } catch (Exception e) {
                    handleError(e);
                }
                switchMode(MODE_START);
            } else if (resetTracking.length() >= 10) {
                resetTracking = null;
            }
        }
        super.keyPressed(keyCode);

        // if done entering pin
        if (input.length() == pinLen || pinLen==0) {
            switch (this.currMode) {
                case 1:
                case 5:
                    switchMode(MODE_GENERATE);
            }
        }
    }

    public final void handleError(Throwable t) {
        t.printStackTrace();
        String msg =
                (t.getMessage()==null ? t.toString() : t.getMessage());
        switchMode(MODE_ERROR);
        super.update("Error", msg, 0);
    }

    public final void switchMode(int mode) {
        this.currMode = mode;
        this.timer.reset();
        switch (mode) {
            case MODE_START:
            case MODE_ENTER_PIN:
                this.input = "";
                super.update("PIN?", null, pinLen);
                removeCommand(midlet.backCmd);
                removeCommand(midlet.generateCmd);
                addCommand(midlet.exitCmd);
                addCommand(midlet.counterCmd);
                return;
            case MODE_GENERATE:
                try {
                    super.update("OTP:", "Generating...", 0);
                    Display.getDisplay(midlet).callSerially(this);
                } catch (Exception e) {
                    handleError(e);
                }
                return;
            case MODE_READY_TO_GEN:
                super.update("OTP:", null, 0);
                return;
            case MODE_STOP:
                this.timer.stop();
            case MODE_ERROR:
                // TBD: decompiler error?
        }
    }

    public final void run() {
        try {
            String otp = HOTPMidlet.generate(input);
            super.update("OTP:", otp, 0);
            removeCommand(midlet.exitCmd);
            removeCommand(midlet.counterCmd);
            addCommand(midlet.backCmd);
            addCommand(midlet.generateCmd);
            return;
        } catch (Exception e) {
            handleError(e);
        }
    }
}
