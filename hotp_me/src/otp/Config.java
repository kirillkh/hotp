package otp;

import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotOpenException;

public final class Config {
    static public final String PIN_LENGTH_KEY = "PIN-Length";
    static public final String DIGITS_KEY = "Digits";
    static public final String BASE_KEY = "Base";
    static public final String TRUNC_KEY = "Truncation-Offset";

    static public final String KEY_KEY = "Key";
    static public final String COUNTER_KEY = "Counter";

    public static MIDlet midlet;
    public static RecordStore store;

    public static void init(MIDlet midlet) throws RecordStoreException {
        Config.midlet = midlet;
        maybeReset(false);
    }

    public static void closeStore() throws RecordStoreException {
        if (store != null)
            store.closeRecordStore();
        store = null;
    }

    public static void reset() throws RecordStoreException {
        maybeReset(true);
    }

    // Decides, whether to reset the counterInc, stored in a record.
    // If reset param is true, resets unconditionally.
    // Otherwise: there are 2 records in the record store. The method checks,
    // whether the app has been run before, by looking at the first record and
    // comparing it to the "counterInc" property. If they match, the app assumes
    // that the second record represents the current counterInc value. Otherwise
    // it resets both records.
    private static void maybeReset(boolean reset) throws RecordStoreException {
        String storeName = "config";
        byte[] counterProp = getByteProperty(COUNTER_KEY);
        boolean noStores = (RecordStore.listRecordStores() == null);
        
        if (!reset) {
            reset = noStores;
            if (!reset) {
                store = RecordStore.openRecordStore(storeName, true);
                reset = (store.getNumRecords() < 2);
                if (!reset) {
                    byte[] counterRec = store.getRecord(1);
                    reset = !(asHexString(counterProp).equals(asHexString(counterRec)));
                }
            }
        }

        if (reset) {
            if (store != null)
                store.closeRecordStore();

            if (!noStores)
                RecordStore.deleteRecordStore(storeName);
            store = RecordStore.openRecordStore(storeName, true);
            store.addRecord(counterProp, 0, counterProp.length);
            store.addRecord(counterProp, 0, counterProp.length);
        }
    }

    private static String getProperty(String name, String defVal) {
        name = "HOTP-" + name;
        
        String prop = midlet.getAppProperty(name);
        if (prop != null)
            prop = prop.trim();

        if (prop == null || prop.length()==0) {
            if (defVal == null)
                throw new IllegalArgumentException("Invalid configuration (missing property " + name + ")");
            return defVal;
        }

        return prop;
    }

    public static int getIntProperty(String name, int defVal) {
        try {
            return Integer.parseInt(getProperty(name, null));
        } catch (IllegalArgumentException iae) {
        }
        return defVal;
    }

    public static byte[] getByteProperty(String name) {
        return fromHexString(getProperty(name, null));
    }

    static byte[] fromHexString(String source) {
        if (source.startsWith("0x") || source.startsWith("0X"))
            source = source.substring(2);
        if (source.length() % 2 != 0)
            source = '0' + source;
        
        byte[] arr = new byte[source.length() / 2];
        for (int i = 0; i < arr.length; ++i) {
            String str = source.substring(2 * i, 2 * i + 2);
            arr[i] = (byte) Integer.parseInt(str, 16);
        }
        return arr;
    }

    static String asHexString(byte[] arr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < arr.length; ++i) {
            String str = Integer.toHexString(arr[i] & 0xFF);
            if (str.length() == 1)
                sb.append('0');
            sb.append(str);
        }
        return sb.toString();
    }

    private static byte[] encode(long data) {
        byte[] arr = new byte[8];
        for (int i = 7; i >= 0; --i) {
            arr[i] = (byte) (int) (data & 0xFF);
            data >>= 8;
        }
        return arr;
    }

    private static long decode(byte[] buf) {
        long val = 0L;
        for (int i = 0; i < buf.length; ++i) {
            val = (val <<= 8) | (buf[i] & 0xFF);
        }
        return val;
    }

    public static long counterInc() throws RecordStoreException {
        long counter = getCounter();
        setCounter(counter + 1);
        return counter;
    }

    public static long getCounter() throws RecordStoreException {
        byte[] arr = store.getRecord(2);
        return decode(arr);
    }

    public static void setCounter(long counter) throws RecordStoreException {
        byte[] arr = encode(counter);
        store.setRecord(2, arr, 0, arr.length);
    }
}
