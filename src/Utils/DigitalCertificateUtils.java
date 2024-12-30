package Utils;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.*;
import java.security.cert.*;

public class DigitalCertificateUtils {
    static {
        // إضافة مزود BouncyCastle
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
    // دالة لعرض تفاصيل الشهادة
    public static void printCertificateDetails(String certificatePath) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate;
            try (FileInputStream certFile = new FileInputStream(certificatePath)) {
                certificate = (X509Certificate) certFactory.generateCertificate(certFile);
            }

            System.out.println("Certificate Details:");
            System.out.println("Subject: " + certificate.getSubjectDN());
            System.out.println("Issuer: " + certificate.getIssuerDN());
            System.out.println("Serial Number: " + certificate.getSerialNumber());
            System.out.println("Valid From: " + certificate.getNotBefore());
            System.out.println("Valid Until: " + certificate.getNotAfter());
            System.out.println("Signature Algorithm: " + certificate.getSigAlgName());
        } catch (Exception e) {
            System.err.println("Error printing certificate details: " + e.getMessage());
        }
    }
    // دالة لعرض تفاصيل الشهادة الرقمية للاتصال الآمن (SSL)
    public static void printServerCertificate(SSLSocket socket) {
        try {
            SSLSession session = socket.getSession();
            X509Certificate[] serverCerts = (X509Certificate[]) session.getPeerCertificates();

            if (serverCerts != null && serverCerts.length > 0) {
                X509Certificate serverCert = serverCerts[0];
                System.out.println("Server Certificate Information:");
                System.out.println("Subject: " + serverCert.getSubjectDN());
                System.out.println("Issuer: " + serverCert.getIssuerDN());
                System.out.println("Serial Number: " + serverCert.getSerialNumber());
                System.out.println("Valid From: " + serverCert.getNotBefore());
                System.out.println("Valid Until: " + serverCert.getNotAfter());
            } else {
                System.out.println("No server certificate available.");
            }
        } catch (SSLPeerUnverifiedException e) {
            System.err.println("Could not retrieve server certificate: " + e.getMessage());
        }
    }
    // تحميل الشهادة من ملف keystore
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
    public static void main(String[] args) {
        try {
            System.out.println("Printing Certificate Details...");
            String certificatePath = "C:\\Users\\ahmad\\Documents\\certificate.crt"; // المسار إلى الشهادة
            printCertificateDetails(certificatePath);

            // مثال على استخدام SSLSocket لطباعة شهادة الخادم في حالة الاتصال الآمن
            String serverHost = "localhost"; // عنوان الخادم
            int serverPort = 3000; // منفذ الخادم

            SSLContext sslContext = loadCertificate("C:\\Users\\ahmad\\Documents\\keystore.jks", "password", "password");
            SSLSocketFactory factory = sslContext.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket(serverHost, serverPort);

            printServerCertificate(socket); // طباعة شهادة الخادم

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
//   C:\Users\ahmad\Documents\keystore.jks