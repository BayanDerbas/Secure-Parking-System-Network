package Utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.io.FileInputStream;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class DigitalCertificateUtils {
    static {
        // تسجيل BouncyCastle كمزود أمني
        Security.addProvider(new BouncyCastleProvider());
    }
    /**
     * تحميل شهادة المستخدم من ملف Keystore
     */
    public static SSLContext loadCertificate(String keyStorePath, String keyStorePassword, String keyPassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream keyStoreFile = new FileInputStream(keyStorePath)) {
            keyStore.load(keyStoreFile, keyStorePassword.toCharArray());
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyPassword.toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        return sslContext;
    }
    /**
     * التحقق من شهادة X.509
     */
    public static boolean verifyCertificate(String certificatePath, String trustedCAPath) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate;
            try (FileInputStream certFile = new FileInputStream(certificatePath)) {
                certificate = (X509Certificate) certFactory.generateCertificate(certFile);
            }

            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream trustStoreFile = new FileInputStream(trustedCAPath)) {
                trustStore.load(trustStoreFile, "changeit".toCharArray());
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    ((X509TrustManager) trustManager).checkServerTrusted(new X509Certificate[]{certificate}, "RSA");
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Certificate verification failed: " + e.getMessage());
        }
        return false;
    }
    /**
     * إنشاء طلب توقيع شهادة (CSR)
     */
    public static String generateCSR(String distinguishedName, String keyPairAlgorithm, int keySize) throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(keyPairAlgorithm);
        keyPairGen.initialize(keySize);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        JcaPKCS10CertificationRequestBuilder csrBuilder =
                new JcaPKCS10CertificationRequestBuilder(new X500Principal(distinguishedName), keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        PKCS10CertificationRequest csr = csrBuilder.build(signer);

        return Base64.getEncoder().encodeToString(csr.getEncoded());
    }
    /**
     * التحقق من صحة CSR
     */
    public static boolean verifyCSR(String csrBase64, PublicKey publicKey) {
        try {
            byte[] csrBytes = Base64.getDecoder().decode(csrBase64);
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(csrBytes);

            ContentVerifierProvider verifierProvider = new JcaContentVerifierProviderBuilder().build(publicKey);
            return csr.isSignatureValid(verifierProvider);
        } catch (Exception e) {
            System.err.println("CSR verification failed: " + e.getMessage());
            return false;
        }
    }
    public static void main(String[] args) {
        try {
            System.out.println("Generating CSR...");
            String distinguishedName = "CN=Test User, OU=IT, O=Example Corp, L=City, ST=State, C=Country";
            String csr = generateCSR(distinguishedName, "RSA", 2048);
            System.out.println("CSR Generated:\n" + csr);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
