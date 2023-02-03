package be.sle.sshutil;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    SshSession(SshEnvironment sshEnvironment, String remoteHost, String remoteUserName) throws SshException {
        this.sshEnvironment = sshEnvironment;
        this.id =   remoteUserName+"@"+sshEnvironment + '#'+ sshEnvironment.getId() ;
        this.remoteHost = remoteHost;
        this.remoteUserName = remoteUserName;

        try {
            session = sshEnvironment.getJsch().getSession(remoteUserName, remoteHost,SshEnvironment.DEF_SSH_PORT);
        } catch (JSchException e) {
            throw new SshException(e);
        }
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


    public int exec(String command, StringBuilder sbOut) throws SshException {
        int rc;
        try (ByteArrayOutputStream bOut = new ByteArrayOutputStream()) {
            rc = exec(command, null, bOut, null);
            sbOut.append(bOut.toString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SshException(e);
        }
        return rc;
    }
    public int exec(String command, InputStream in, OutputStream out, OutputStream err) throws SshException {
        if (!session.isConnected()) {
            try {
                session.connect();
            } catch (JSchException e) {
                throw new SshException(e);
            }
        }

        ByteArrayOutputStream bOut = null, bErr = null;

        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

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
            try {
                channel.connect();
            } catch (JSchException e) {
                throw new SshException(e);
            }

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
        } catch (JSchException e) {
            throw new SshException(e);
        } catch (InterruptedException e) {
            throw new SshException(e);
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
        static final Pattern NEW_HOST_PATTERN
            = Pattern.compile("The authenticity of host '.*' can't be established\\.");

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
            System.err.println(message);
            //dbg("promptYesNo -> " + message);
            String[] msgLines = message.split("\n");
            Matcher matcher = NEW_HOST_PATTERN.matcher(msgLines[0]);
            return (matcher.matches());
        }

        @Override
        public void showMessage(String message) {
            //dbg("showMessage -> " + message);
            System.err.println(message);
        }
    }

    private void dbg(String message) {
        sshEnvironment.dbg(message);
    }
}