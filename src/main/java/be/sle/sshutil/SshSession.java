package be.sle.sshutil;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * <p>This class tries to simplify as much what is needed to execute commands on an SSH server.
 * It will try to load a currently installed <code>id_rsa</code> private key and an <code>known_hosts</code> file
 * found in default user's <code>.ssh</code> - configuration directory.
 * </p>
 * <p>>Usage Example:
 * <code>
 *     try (SshSession session = new SshSession()) {
 *          System.out.println(session.exec('/bin/pwd'))
 *     }
 * </code>
 * </p>
 * The previous code snippet tries to log to the localhost:22 as the current logged-in user prints the output of the
 * <code>/bin/pwd</code> command.
 */
public class SshSession implements AutoCloseable, Closeable {

    private final SshEnvironment sshEnvironment;
    @SuppressWarnings("unused")
    private final String id;
    private final String remoteHost;
    private final String remoteUserName;
    private Session session;

    SshSession(SshEnvironment sshEnvironment, String remoteHost, String remoteUserName) throws JSchException {
        this.sshEnvironment = sshEnvironment;
        this.id =   remoteUserName+"@"+sshEnvironment + '#'+ sshEnvironment.getId() ;
        this.remoteHost = remoteHost;
        this.remoteUserName = remoteUserName;

        session = sshEnvironment.getJsch().getSession(remoteUserName, remoteHost,SshEnvironment.DEF_SSH_PORT);
        session.setConfig("StrictHostKeyChecking", "ask");
        session.setUserInfo(new SessionUserInfo());
    }
    @SuppressWarnings("unused")
    public String id() {
        return id;
    }

    @SuppressWarnings("unused")
    public String remoteHost() {
        return remoteHost;
    }

    @SuppressWarnings("unused")
    public String remoteUser() {
        return remoteUserName;
    }


    public int exec(String command, StringBuilder sbOut) throws JSchException, InterruptedException, IOException {
        int rc;
        try (ByteArrayOutputStream bOut = new ByteArrayOutputStream()) {
            rc = exec(command, null, bOut, null);
            sbOut.append(bOut.toString(StandardCharsets.UTF_8));
        }
        return rc;
    }
    public int exec(String command, InputStream in, OutputStream out, OutputStream err) throws JSchException, InterruptedException {
        if (!session.isConnected()) {
            session.connect();
        }

        ByteArrayOutputStream bOut = null, bErr = null;

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        try {
            if (in != null) {
                channel.setInputStream(in);
            }
            if (out != null) {
                channel.setOutputStream(out);
            } else {
                channel.setOutputStream(bOut = new ByteArrayOutputStream());
            }
            if (err != null) {
                channel.setErrStream(err);
            } else {
                channel.setErrStream(bErr = new ByteArrayOutputStream());
            }
            channel.connect();

            while (channel.isConnected()) {
                Thread.sleep(100);
            }

            if (bOut != null) {
                System.out.println(bOut.toString(StandardCharsets.UTF_8));
            }
            if (bErr != null) {
                System.err.println(bErr.toString(StandardCharsets.UTF_8));
            }
            int rc = channel.getExitStatus();
            sshEnvironment.dbg("rc -> " + rc);
            return rc;
        } finally {
            if (channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    @Override
    public void close() {
        if ((session != null) && (session.isConnected())) {
            session.disconnect();
        }
        session = null;
    }


    private class SessionUserInfo implements UserInfo  {

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public boolean promptPassword(String message) {
            return false;
        }

        @Override
        public boolean promptPassphrase(String message) {
            return true;
        }

        @Override
        public boolean promptYesNo(String message) {
            dbg("promptYesNo -> " + message);
            return false;
        }

        @Override
        public void showMessage(String message) {
            dbg("showMessage -> " + message);
        }
    }

    private void dbg(String message) {
        sshEnvironment.dbg(message);
    }
}