package org.activityinfo.gcloud

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sqladmin.SQLAdmin
import com.google.api.services.sqladmin.model.SslCertsInsertRequest
import org.activityinfo.store.mysql.MySqlServer
import org.bouncycastle.openssl.PEMReader
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.security.KeyPair
import java.security.KeyStore
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory

/**
 * Connects to the given project id and obtains the IP address of the
 * MySQL database as well as a client certificate that can be used to connect
 * to the database.
 */
class FetchCloudSqlConfiguration extends DefaultTask {

    @Input
    String projectId = "unset"

    @Input
    String instanceId
    
    public String getCertificateName() {
        def name = System.getProperty("user.name") + "-" + InetAddress.getLocalHost().getHostName()
        return name.trim()
                .replaceAll("([0-9])", { (('A' as char) + Integer.parseInt(it[0])) as char })
                .replaceAll("[^A-Za-z._ -]", "")
    }
    
    public File getClientKeyFile() {
        return new File(getProjectDir(), "client-key.pem")
    }

    public File getClientCertFile() {
        return new File(getProjectDir(), "client-cert.pem")
    }

    public File getServerCertFile() {
        return new File(getProjectDir(), "server-ca.pem")
    }

    public File getProjectDir() {
        File cloudSql = new File(project.gradle.gradleUserHomeDir, "cloudsql")
        return new File( cloudSql, projectId)
    }

    public File getKeyStoreFile() {
        return new File(getProjectDir(), "keystore")
    }
    
    @TaskAction
    def fetch() {
        def httpTransport =  GoogleNetHttpTransport.newTrustedTransport()
        def jsonFactory = new JacksonFactory()
        def credentials = GoogleCredential
                .fromStream(openJsonCredentials())
                .createScoped([
                    'https://www.googleapis.com/auth/sqlservice.admin', 
                    'https://www.googleapis.com/auth/cloud-platform' ])
        
        def client = new SQLAdmin.Builder(httpTransport, jsonFactory, credentials)
                .setApplicationName("ActivityInfo")
                .build();
        
        def instance = client.instances().get(projectId, instanceId).execute();
        
        projectDir.mkdirs()
        
        serverCertFile.write instance.getServerCaCert().getCert()
        
        // Create a client certificate if necessary
        if(!clientKeyFile.exists()) {
            def cert = client.sslCerts()
                    .insert(projectId, instanceId, new SslCertsInsertRequest(commonName: certificateName))
                    .execute()
            
            clientKeyFile.write cert.getClientCert().getCertPrivateKey()
            clientCertFile.write cert.getClientCert().getCertInfo().getCert()
        }
        writeJavaKeyStore()
        
        project.environment.database.name = 'activityinfo'
        project.environment.database.server = new MySqlServer(
                host: instance.getIpAddresses().first().getIpAddress(),
                username: 'root',
                password: 'root',
                keyStore: keyStoreFile)
    }

    /**
     * Write the SSL certificates to a Java Key Store that can be subsequently used
     * by the MySQL JDBC Driver
     */
    def writeJavaKeyStore() {
        
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());


        def ks = KeyStore.getInstance(KeyStore.getDefaultType());
        def password = "notasecret".toCharArray()
        ks.load(null, password)

        def serverCertificate = parseCertificate(serverCertFile.text)
        def clientCertificate = parseCertificate(clientCertFile.text)

        ks.setCertificateEntry("mysqlServerCACert", serverCertificate)
        ks.setCertificateEntry("mysqlClientCertificate", clientCertificate)
        ks.setKeyEntry("mysqlClientKey", parseKey(clientKeyFile.text), "notasecret".toCharArray(), [ clientCertificate ] as Certificate[])
        
        FileOutputStream fos = new FileOutputStream(getKeyStoreFile());
        ks.store(fos, password);
        fos.close();
    }

    private Certificate parseCertificate(String cert) {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(new ByteArrayInputStream(cert.getBytes()));
    }
    
    private def parseKey(String key) {
        PEMReader reader = new PEMReader(new StringReader(key))
        def keyPair = reader.readObject() as KeyPair
        return keyPair.getPrivate()
    }

    InputStream openJsonCredentials() {
        
        if(System.getenv("SERVICE_KEY")) {
            return new FileInputStream(System.getenv("SERVICE_KEY"))
        }
        
        File homeDir = new File(project.gradle.gradleUserHomeDir, "serviceKeys")
        if(homeDir.exists() && homeDir.listFiles()) {
            for (File file : homeDir.listFiles()) {
                if (file.name.startsWith(projectId) && file.name.endsWith('.json')) {
                    return file.newInputStream()
                }
            }
        }
        throw new RuntimeException("No service key found for ${projectId}: create and download a" +
                " service key from the Google console and copy it to ${homeDir.absolutePath}")
    }
}
