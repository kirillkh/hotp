package otp;

public final class Timer implements Runnable {

    private SwitchableScreen screen;
    private boolean stop;
    private long start;

    public Timer(SwitchableScreen screen) {
        this.screen = screen;
        this.stop = false;
        reset();
    }

    public final void stop() {
        synchronized (this) {
            this.stop = true;
            super.notify();
            return;
        }
    }

    public final void reset() {
        start = System.currentTimeMillis();
    }

    public final void run() {
        while (!this.stop) {
            try {
                long curr = System.currentTimeMillis();
                if ((curr >= this.start + 30000L) && (screen.currMode == SwitchableScreen.MODE_GENERATE))
                    this.screen.switchMode(SwitchableScreen.MODE_READY_TO_GEN);
                if (curr >= this.start + 120000L)
                    this.screen.switchMode(SwitchableScreen.MODE_ENTER_PIN);

                long toWait = 30000L - (curr - this.start);
                if (toWait <= 0L)
                    toWait = 90000L - (curr - this.start);
                if (toWait <= 0L)
                    toWait = 30000L;

                synchronized (this) {
                    try {
                        if (!this.stop)
                            super.wait(toWait);
                    } catch (InterruptedException ie) {
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}