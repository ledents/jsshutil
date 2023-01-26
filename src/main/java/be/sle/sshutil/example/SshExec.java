package be.sle.sshutil.example;

import be.sle.sshutil.SshEnvironment;
import be.sle.sshutil.SshSession;
import com.jcraft.jsch.JSchException;
import java.io.IOException;

public class SshExec {
    public static void main(String[] args) throws JSchException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(arg);
        }
        if (sb.isEmpty()) {
            System.err.println("""
                    Syntax:  SshExec <command>
                    A <command> must be passed as argument !!! 
                    """);
        }

        SshEnvironment sshEnvironment = new SshEnvironment();

        try (SshSession session = sshEnvironment.openSession("localhost")) {
            StringBuilder sbOut = new StringBuilder();
            int rc = session.exec("echo $$", sbOut);
            System.out.println(sbOut);
            System.exit(rc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
