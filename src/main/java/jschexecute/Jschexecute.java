package main.java.jschexecute;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.Channel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class Jschexecute {
    private static String username;
    private static String password;
    private static String host;
    private static String command;
    public static void main(String[] args) throws Exception {
        byte data[] = new byte[50];

        System.out.print("Enter username: ");
        System.in.read(data);
        username = new String(data, StandardCharsets.UTF_8).trim();

        System.out.print("Password: ");
        System.in.read(data);
        password = new String(data, StandardCharsets.UTF_8).trim();

        System.out.print("Enter host: ");
        System.in.read(data);
        host = new String(data, StandardCharsets.UTF_8).trim();

        System.out.println("Enter command:");
        System.in.read(data);
        command = new String(data, StandardCharsets.UTF_8).trim();

        listFolderStructure1(username, password, host, 22, command);
        //listFolderStructure2(username, password, host, 22, 30, command);
    }

    /*
      JSch implementation
     */
    public static void listFolderStructure1(String username, String password,
                                           String host, int port, String command) throws Exception {

        Session session = null;
        ChannelExec channel = null;

        try {
            System.out.println(host);
            session = new JSch().getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            channel.setOutputStream(responseStream);
            channel.connect();

            while (channel.isConnected()) {
                Thread.sleep(100);
            }

            String responseString = new String(responseStream.toByteArray());
            System.out.println(responseString);
        } catch (Exception e) {
            System.out.println(e);

        } finally {
            if (session != null) {
                session.disconnect();
            }
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /*
      Apache MINA SSHD implementation
     */
    public static void listFolderStructure2(String username, String password,
                                            String host,
                                            int port,
                                            long defaultTimeoutSeconds,
                                            String command) throws IOException, IOException {

        SshClient client = SshClient.setUpDefaultClient();
        client.start();

        try (ClientSession session = client.connect(username, host, port)
                .verify(defaultTimeoutSeconds, TimeUnit.SECONDS).getSession()) {
            session.addPasswordIdentity(password);
            session.auth().verify(defaultTimeoutSeconds, TimeUnit.SECONDS);

            try (ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                 ClientChannel channel = session.createChannel(Channel.CHANNEL_SHELL)) {
                channel.setOut(responseStream);
                try {
                    channel.open().verify(defaultTimeoutSeconds, TimeUnit.SECONDS);
                    try (OutputStream pipedIn = channel.getInvertedIn()) {
                        pipedIn.write(command.getBytes());
                        pipedIn.flush();
                    }

                    channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED),
                            TimeUnit.SECONDS.toMillis(defaultTimeoutSeconds));
                    String responseString = new String(responseStream.toByteArray());
                    System.out.println(responseString);
                } finally {
                    channel.close(false);
                }
            }
        } finally {
            client.stop();
        }
    }
}