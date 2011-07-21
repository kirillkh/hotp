package otp;

import javax.microedition.rms.RecordStoreException;

/**
 *
 * @author kirillkh
 */
public class CounterScreen extends Screen {
    private HOTPMidlet midlet;
    long counter;

    public CounterScreen(HOTPMidlet midlet) {
	super(false);
        this.midlet = midlet;
        super.update(null, null, 0);
        init();
    }

    private void init() {
        addCommand(midlet.cancelCmd);
        addCommand(midlet.okCmd);
        resetCounter();
    }

    public void resetCounter() {
        try {
            counter = Config.getCounter();
            update(counter);
        } catch (RecordStoreException ex) {
            handleError(ex);
        }
    }

    public final void keyPressed(int keyCode) {
        if(keyCode == '2') {
            update(++counter);
        } else if(keyCode == '8') {
            update(--counter);
        }
    }

    private void update(long counter) {
        super.update("Counter", Long.toString(counter, 16), 0);
    }

    public final void handleError(Throwable t) {
        t.printStackTrace();
        String msg =
                (t.getMessage()==null ? t.toString() : t.getMessage());
        super.update("Error", msg, 0);
    }
}
