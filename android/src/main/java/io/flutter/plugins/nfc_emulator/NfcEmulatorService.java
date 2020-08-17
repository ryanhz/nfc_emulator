package io.flutter.plugins.nfc_emulator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class NfcEmulatorService extends HostApduService {

    private static final String TAG = "NfcEmulator";
    private static final String AES = "AES";
    private static final String CIPHERMODE = "AES/CBC/PKCS5Padding";

    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";

    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String GET_DATA_APDU_HEADER = "00CA0000";

    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = hexStringToByteArray("9000");

    // "UNKNOWN" status word sent in response to invalid APDU command (0x0000)
    private static final byte[] UNKNOWN_CMD_SW = hexStringToByteArray("0000");

    private static byte[] SELECT_APDU;

    private static final byte[] GET_DATA_APDU = buildGetDataApdu();

    private SharedPreferences sharePerf;
    private Vibrator vibrator;

    private String cardAid = null;
    private String cardUid = null;
    private String aesKey = null;

    @Override
    public void onCreate() {
        super.onCreate();
        sharePerf = getSharedPreferences("NfcEmulator", Context.MODE_PRIVATE);
        cardAid = sharePerf.getString("cardAid", null);
        cardUid = sharePerf.getString("cardUid", null);
        aesKey  = sharePerf.getString("aesKey", null);
        vibrator = (Vibrator) this.getSystemService(NfcEmulatorService.VIBRATOR_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        cardAid = intent.getStringExtra("cardAid");
        cardUid = intent.getStringExtra("cardUid");
        aesKey = intent.getStringExtra("aesKey");

        SharedPreferences.Editor editor = sharePerf.edit();
        editor.putString("cardAid", cardAid);
        editor.putString("cardUid", cardUid);
        editor.putString("aesKey", aesKey);
        editor.apply();

        SELECT_APDU = buildSelectApdu(cardAid);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public byte[] processCommandApdu(byte[] bytes, Bundle bundle) {
        if(cardAid==null || cardAid.equals("") || cardUid==null || cardUid.equals("")) {
            return UNKNOWN_CMD_SW; // don't start emulator
        }

        // If the APDU matches the SELECT AID command for this service,
        // send the loyalty card account number, followed by a SELECT_OK status trailer (0x9000).
        if (Arrays.equals(SELECT_APDU, bytes)) {
            Log.i(TAG, "< SELECT_APDU: "+ byteArrayToHexString(bytes));
            String account = "";
            // Log.i(TAG,"send data1:"+account);
            byte[] accountBytes = hexStringToByteArray(account);
            byte[] response = concatArrays(accountBytes, SELECT_OK_SW);
            Log.i(TAG, "> SELECT_APDU: "+ byteArrayToHexString(bytes));
            return response;
        } else {
            byte[] decrypted = bytes;
            if(aesKey!=null) {
                try {
                    decrypted = decrypt(aesKey, bytes);
                } catch (Exception e) {
                    Log.e(TAG, "Exception in decryption", e);
                }
            }
            if (Arrays.equals(GET_DATA_APDU, decrypted)) {
                Log.i(TAG, "< GET_DATA_APDU: "+ byteArrayToHexString(decrypted));
                try {
                    byte[] bytesToSend = buildGetDataReply();
                    if(bytesToSend==null) {
                        return null;
                    }
                    if(aesKey!=null) {
                        try {
                            bytesToSend = encrypt(aesKey, bytesToSend);
                        } catch (Exception e) {
                            Log.e(TAG, "Exception in encryption", e);
                        }
                    }
                    Log.i(TAG, "> GET_DATA_APDU: "+ byteArrayToHexString(bytesToSend));
                    vibrator.vibrate(400);
                    return bytesToSend;
                } catch (Exception e) {
                    Log.e(TAG, "Exception in GET_DATA_APDU", e);
                }
            }

        }
        return UNKNOWN_CMD_SW;
    }

    @Override
    public void onDeactivated(int i) {
    }


    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    public static byte[] buildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return hexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X",
                aid.length() / 2) + aid);
    }

    /**
     * Build APDU for GET_DATA command. See ISO 7816-4.
     *
     * @return APDU for SELECT AID command
     */
    public static byte[] buildGetDataApdu() {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return hexStringToByteArray(GET_DATA_APDU_HEADER + "0FFF");
    }

    private byte[] buildGetDataReply() {
        if(0 == cardUid.length() || 32 != aesKey.length()) {
            return null;
        }
        String sCardMsg =  String.format("%02X", cardUid.length() / 2) + cardUid;
        byte[] accountBytes = hexStringToByteArray(sCardMsg);
        return concatArrays(accountBytes, SELECT_OK_SW);
    }
	
	public static byte[] encrypt(String key, byte[] clear) throws Exception {
        byte[] raw = hexStringToByteArray(key);
        SecretKeySpec keySpec = new SecretKeySpec(raw, AES);
        Cipher cipher = Cipher.getInstance(CIPHERMODE);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
        return cipher.doFinal(clear);
    }

	public static byte[] decrypt(String key, byte[] clear) throws Exception {
        byte[] raw = hexStringToByteArray(key);
        SecretKeySpec keySpec = new SecretKeySpec(raw, AES);
        Cipher cipher = Cipher.getInstance(CIPHERMODE);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
        return cipher.doFinal(clear);
    }

    /**
     * Utility method to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String byteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2]; // Each byte has two hex characters (nibbles)
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF; // Cast bytes[j] to int, treating as unsigned value
            hexChars[j * 2] = hexArray[v >>> 4]; // Select hex character from upper nibble
            hexChars[j * 2 + 1] = hexArray[v & 0x0F]; // Select hex character from lower nibble
        }
        return new String(hexChars);
    }


    /**
     * Utility method to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     * @throws java.lang.IllegalArgumentException if input length is incorrect
     */
    public static byte[] hexStringToByteArray(String s) throws IllegalArgumentException {
        int len = s.length();
        if (len % 2 == 1) {
            throw new IllegalArgumentException("Hex string must have even number of characters");
        }
        byte[] data = new byte[len / 2]; // Allocate 1 byte per 2 hex characters
        for (int i = 0; i < len; i += 2) {
            // Convert each character into a integer (base-16), then bit-shift into place
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Utility method to concatenate two byte arrays.
     * @param first First array
     * @param rest Any remaining arrays
     * @return Concatenated copy of input arrays
     */
    public static byte[] concatArrays(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

}
