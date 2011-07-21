package hotp;

import hotp.crypto.HMac;
import hotp.crypto.KeyParameter;
import hotp.crypto.SHA1Digest;
import hotp.crypto.RijndaelEngine;
import java.io.UnsupportedEncodingException;

/**
 * License: GPL v3
 * Author: kirillkh
 * Original code: reverse-engineered OTP generator from HUJI, which is based on
 *                RFC4226 and the BouncyCastle library (MIT license).
 */
public class Generator {
    private HMac hmac;
    private int digits;
    private int trunc;
    private int bound;

    public Generator(String key, int digits, int trunc, String pin) {
        this.digits = digits;
        this.trunc = trunc;

        byte[] secret = fromHexString(key);

        // yes! it's really "blablablabla" in the original code
        String str = "blablablabla";
        byte[] pinArr = null;
        try {
            pinArr = (pin += str.substring(0, 16 - pin.length())).getBytes("ISO8859_1");
        } catch (UnsupportedEncodingException ex) {
            // should never happen
            ex.printStackTrace();
        }
        RijndaelEngine rij = new RijndaelEngine(128);
        rij.init(false, new KeyParameter(pinArr));
        for (int i = 0; i < secret.length; i += 16) {
            rij.processBlock(secret, i, secret, i);
        }

        initBound();
        hmac = new HMac(new SHA1Digest());
        hmac.init(new KeyParameter(secret));
    }

    private void initBound() {
        long l = 1;
        for (int i = 0; (l != 0) && (i < digits); ++i) {
            if ((l *= 10) > Integer.MAX_VALUE) {
                l = 0;
            }
        }
        bound = (int) l;
    }

    public String generate(long counter) {
        byte[] counterBytes = counterToBytes(counter);
        return generate(counterBytes);
    }

    private String generate(byte[] counterBytes) {
        byte[] arr = new byte[hmac.getMacSize()];
        hmac.reset();
        hmac.update(counterBytes, 0, counterBytes.length);
        hmac.doFinal(arr, 0);
        int i = (trunc < 0) ? arr[arr.length - 1] & 0xF : trunc;
        int otp = (arr[i++] & 0x7F) << 24 |
                  (arr[i++] & 0xFF) << 16 |
                  (arr[i++] & 0xFF) << 8 |
                   arr[i] & 0xFF;
        if (bound > 0)
            otp %= bound;
        String str = Integer.toString(otp);
        while (str.length() < digits) {
            str = '0' + str;
        }
        return str;
    }

    private static byte[] counterToBytes(long counter) {
        byte[] out = new byte[8];
        for (int i = 7; i >= 0; --i) {
            out[i] = (byte) (int) (counter & 0xFF);
            counter >>= 8;
        }
        return out;
    }

    private static byte[] fromHexString(String source) {
        if (source.startsWith("0x") || source.startsWith("0X"))
            source = source.substring(2);
        if (source.length() % 2 != 0)
            source = '0' + source;

        byte[] buf = new byte[source.length() / 2];
        for (int i = 0; i < buf.length; ++i) {
            String str = source.substring(2 * i, 2 * i + 2);
            buf[i] = (byte) Integer.parseInt(str, 16);
        }
        return buf;
    }

    static String asHexString(byte[] buf) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; ++i) {
            String str = Integer.toHexString(buf[i] & 0xFF);
            if (str.length() == 1)
                sb.append('0');
            sb.append(str);
        }
        return sb.toString();
    }
}
