# jsshutil
Simple Java Helper for connecting to an SSH server and execute commands.

```java
SshEnvironment sshEnv = new SshEnvironment();

try (SshSession localhost = sshEnv.openSession('localhost')) {
  StringBuilder sbPid = new StringBuilder();
  if (localhost.exec("echo $$", sbPid) == 0) {
      System.out.println("SSH executing environment pid -> " + sbPid);
  } 
}
```
The previous code snippet using ssh to the local host and return the **pid** of the shell executing remote command.

