package io.github.bxo.auth;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;

import io.github.bxo.auth.totp.TOTP;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

public class GoogleAuthenticator {

    public static String getRandomSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        String secretKey = base32.encodeToString(bytes);
        // make the secret key more human-readable by lower-casing and
        // inserting spaces between each group of 4 characters
        return secretKey.toLowerCase().replaceAll("(.{4})(?=.{4})", "$1 ");
    }

    /**
     * @param secretKey Base32 encoded secret key (may have optional whitespace)
     * @param account The user's account name. e.g. an email address or a username
     * @param issuer The organization managing this account
     * @return
     *
     * @see https://github.com/google/google-authenticator/wiki/Key-Uri-Format
     */
    public static String getGoogleAuthenticatorBarCode(String secretKey, String account, String issuer) {
        String normalizedBase32Key = secretKey.replace(" ", "").toUpperCase();
        try {
            return "otpauth://totp/"
                    + URLEncoder.encode(issuer + ":" + account, "UTF-8").replace("+", "%20")
                    + "?secret=" + URLEncoder.encode(normalizedBase32Key, "UTF-8").replace("+", "%20")
                    + "&issuer=" + URLEncoder.encode(issuer, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void createQRCode(String barCodeData, String filePath, int height, int width)
            throws WriterException, IOException {
        BitMatrix matrix = new MultiFormatWriter().encode(barCodeData, BarcodeFormat.QR_CODE,
                width, height);
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            MatrixToImageWriter.writeToStream(matrix, "png", out);
        }
    }

    public static String getTOTPCode(String secretKey) {
        String normalizedBase32Key = secretKey.replace(" ", "").toUpperCase();
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(normalizedBase32Key);
        String hexKey = Hex.encodeHexString(bytes);
        long time = (System.currentTimeMillis() / 1000) / 30;
        String hexTime = Long.toHexString(time);
        return TOTP.generateTOTP(hexKey, hexTime, "6");
    }

    public static void main(String[] args) throws Exception {
        // required for generating the PNG file on a server with no graphics hardware
        System.setProperty("java.awt.headless", "true");

        String secretKey = getRandomSecretKey();
        String barCode = getGoogleAuthenticatorBarCode(secretKey, "test@example.com", "Example Company");
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (!tmpDir.endsWith(File.separator)) {
            tmpDir += File.separator;
        }
        String qrCodePath = tmpDir + "2fa-qr-code.png";
        createQRCode(barCode, qrCodePath, 400, 400);

        System.out.println("\nConfigure the Google Authenticator App by scanning the following QR code image:\n");
        System.out.println(qrCodePath + "\n");
        System.out.println("or by manually entering the secret key:\n");
        System.out.println(secretKey + "\n");
        System.out.println("Then verify that the 6 digit codes generated by Google Authenticator\n"
                + "are synchronized with the following (ctrl-c to exit at any time):\n");

        String lastCode = null;
        while (true) {
            String code = getTOTPCode(secretKey);
            if (!code.equals(lastCode)) {
                System.out.println(code);
            }
            lastCode = code;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {};
        }
    }
}
