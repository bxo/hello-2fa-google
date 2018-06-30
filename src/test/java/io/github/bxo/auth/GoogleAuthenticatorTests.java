package io.github.bxo.auth;

import com.google.zxing.WriterException;
import org.junit.Test;

import java.io.IOException;

public class GoogleAuthenticatorTests {

    private String testKey = "ft74 jjj7 4mst eliv 5rfu 4kkr eecw qptj";


    @Test
    public void testRandomSecretKey(){

        String sk = GoogleAuthenticator.getRandomSecretKey();
        System.out.println(sk);
    }

    @Test
    public void testCreateQRCode() throws IOException, WriterException {

        GoogleAuthenticator.createQRCode("abc","./qr.png",400,400);

    }

    @Test
    public void testTOTPCode(){

        String lastCode = null;
        while (true) {
            String code = GoogleAuthenticator.getTOTPCode(testKey);
            if (!code.equals(lastCode)) {
                // output a new 6 digit code
                System.out.println(code);
            }
            lastCode = code;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {};
        }
    }

    @Test
    public void testGoogleAuthenticatorBarCode() throws IOException, WriterException {
        String barCode = GoogleAuthenticator.getGoogleAuthenticatorBarCode(testKey,"bxo","github");
        GoogleAuthenticator.createQRCode(barCode,"./qr.png",400,400);

    }

}
