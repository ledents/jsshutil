package be.sle.sshutil.example;

import be.sle.sshutil.SshEnvironment;
import be.sle.sshutil.SshSession;

public class CodeSamples {
    public static void main(String[] args) throws Exception {
        echoCmdPID();
    }
    static void echoCmdPID() throws Exception {
        SshEnvironment sshEnv = new SshEnvironment();
        try (SshSession localhost = sshEnv.openSession("localhost")) {
            StringBuilder sbPid = new StringBuilder();
            if (localhost.exec("echo $$", sbPid) == 0) {
                System.out.println("Remote shell pid -> " + sbPid);
            }
        }
    }
}
