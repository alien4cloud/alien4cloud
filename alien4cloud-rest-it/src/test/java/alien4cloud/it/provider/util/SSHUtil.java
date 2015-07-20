package alien4cloud.it.provider.util;

import java.io.FileReader;
import java.io.IOException;
import java.security.KeyPair;
import java.security.Security;

import lombok.extern.slf4j.Slf4j;

import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.ScpClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.common.future.SshFutureListener;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

@Slf4j
public class SSHUtil {

    private static KeyPair loadKeyPair(String pemFile) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            PEMParser pemParser = new PEMParser(new FileReader(pemFile));
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            Object object = pemParser.readObject();
            return converter.getKeyPair((PEMKeyPair) object);
        } catch (Exception e) {
            log.error("Could not load key pair", e);
            throw new RuntimeException("Could not load key pair", e);
        }
    }

    private static ClientSession connect(SshClient client, final String user, KeyPair keyPair, final String ip, final int port) throws IOException,
            InterruptedException {
        ClientSession session = client.connect(user, ip, port).await().getSession();
        int authState = ClientSession.WAIT_AUTH;
        while ((authState & ClientSession.WAIT_AUTH) != 0) {
            session.addPublicKeyIdentity(keyPair);
            log.info("Authenticating to " + user + "@" + ip);
            AuthFuture authFuture = session.auth();
            authFuture.addListener(new SshFutureListener<AuthFuture>() {
                @Override
                public void operationComplete(AuthFuture authFuture) {
                    log.info("Authentication completed with " + (authFuture.isSuccess() ? "success" : "failure") + " for " + user + "@" + ip + ":" + port);
                }
            });
            authState = session.waitFor(ClientSession.WAIT_AUTH | ClientSession.CLOSED | ClientSession.AUTHED, 0);
        }

        if ((authState & ClientSession.CLOSED) != 0) {
            throw new IOException("Authentication failed for " + user + "@" + ip);
        }
        return session;
    }

    private interface DoWithScpAction {
        void doScpAction(ScpClient scpClient) throws IOException;
    }

    private static void doWithScp(String user, String ip, int port, String pemPath, DoWithScpAction doWithScpAction) throws IOException, InterruptedException {
        SshClient client = SshClient.setUpDefaultClient();
        ClientSession session = null;
        try {
            client.start();
            session = connect(client, user, loadKeyPair(pemPath), ip, port);
            ScpClient scpClient = session.createScpClient();
            doWithScpAction.doScpAction(scpClient);
        } finally {
            if (session != null) {
                session.close(false);
            }
            client.close(false);
        }
    }

    public static void download(String user, String ip, int port, String pemPath, final String remote, final String local) throws IOException,
            InterruptedException {
        doWithScp(user, ip, port, pemPath, new DoWithScpAction() {
            @Override
            public void doScpAction(ScpClient scpClient) throws IOException {
                scpClient.download(remote, local, ScpClient.Option.Recursive);
            }
        });
    }

    public static void upload(String user, String ip, int port, String pemPath, final String remote, final String local) throws IOException,
            InterruptedException {
        doWithScp(user, ip, port, pemPath, new DoWithScpAction() {
            @Override
            public void doScpAction(ScpClient scpClient) throws IOException {
                scpClient.upload(local, remote, ScpClient.Option.Recursive);
            }
        });
    }
}
