package Utils;

import java.security.*;
import javax.crypto.Cipher;
import java.util.Base64;

public class RSAUtils {

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        System.out.println("Generating RSA Key Pair...");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(KEY_SIZE);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        System.out.println("RSA Key Pair Generated Successfully!");
        return keyPair;
    }
    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        System.out.println("Encrypting Data [Algorithm: RSA]: " + plainText);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);
        System.out.println("Encrypted Data [Algorithm: RSA]: " + encryptedText);
        return encryptedText;
    }
    public static String decrypt(String encryptedText, PrivateKey privateKey) throws Exception {
        System.out.println("Decrypting Data [Algorithm: RSA]: " + encryptedText);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        String decryptedText = new String(decryptedBytes);
        System.out.println("Decrypted Data [Algorithm: RSA]: " + decryptedText);
        return decryptedText;
    }
    public static String publicKeyToBase64(PublicKey publicKey) {
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        System.out.println("Public Key (Base64): " + publicKeyBase64);
        return publicKeyBase64;
    }
    public static String privateKeyToBase64(PrivateKey privateKey) {
        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        System.out.println("Private Key (Base64): " + privateKeyBase64);
        return privateKeyBase64;
    }
}
