package otp;

import otp.crypto.HMac;
import otp.crypto.SHA1Digest;
import otp.crypto.KeyParameter;

/**
 * License: GPL v3
 * Author: kirillkh
 * Original code: reverse-engineered OTP generator from HUJI, which is based on
 *                RFC4226 and the BouncyCastle library (MIT license).
 */
public final class Generator {
    public HMac hmac;
    public int digits;
    public int base;
    public int trunc;
    public int bound;

    public Generator(byte[] secret, int digits, int base, int trunc) {
        this.digits = digits;
        this.base = base;
        this.trunc = trunc;

        long l = 1;
        for (int i = 0; (l != 0) && (i < digits); ++i) {
            if ((l *= base) > Integer.MAX_VALUE) {
                l = 0;
            }
        }
        bound = (int) l;

        hmac = new HMac(new SHA1Digest());
        hmac.init(new KeyParameter(secret));
    }

    public final String generate(long counter) {
        byte[] counterBytes = getCounterBytes(counter);
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
        
        String strOtp = Integer.toString(otp, base);
        while (strOtp.length() < digits) {
            strOtp = '0' + strOtp;
        }
        return strOtp;
    }

    private static byte[] getCounterBytes(long counter) {
        byte[] out = new byte[8];
        for (int i = 7; i >= 0; --i) {
            out[i] = (byte) (int) (counter & 0xFF);
            counter >>= 8;
        }
        return out;
    }
}
