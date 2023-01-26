package be.sle.sshutil;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>This class provides an SSH environment which can be used to open sessions to different remoteHosts.
 *
 * </p>
 */
public class SshEnvironment {
  public static final byte[] NO_PASSPHRASE = null;
  public static final String DEFAULT_IDENTITY_FILENAME = "id_rsa";
  public static final int DEF_SSH_PORT = 22;
  public static final String DEF_SSH_USERNAME = System.getProperty("user.name");
  public static final Path HOME_PATH = Paths.get(System.getProperty("user.home"));

  public static final Path SSH_CFG_PATH = HOME_PATH.resolve(".ssh");

  private static final Path IDENTITY_PATH = SSH_CFG_PATH.resolve("id_rsa");
  private static final Path KNOWN_HOSTS_PATH = SSH_CFG_PATH.resolve("known_hosts");

  private static int nextId = 0;

  private final int id;

  private final String defaultUsername;

  private boolean dbgIsOn = false;

  private final JSch jsch;

  @SuppressWarnings("unused")
  public SshEnvironment() throws JSchException {
    this(DEF_SSH_USERNAME, DEFAULT_IDENTITY_FILENAME, NO_PASSPHRASE);
  }

  @SuppressWarnings("unused")
  public SshEnvironment(byte[] passphrase) throws JSchException {
    this(DEF_SSH_USERNAME, DEFAULT_IDENTITY_FILENAME, passphrase);
  }

  @SuppressWarnings("unused")
  public SshEnvironment(String defaultUsername, byte[] passphrase) throws JSchException {
    this(defaultUsername, DEFAULT_IDENTITY_FILENAME, passphrase);
  }
  public SshEnvironment(String defaultUsername, String identityKeyFilename, byte[] passphrase) throws JSchException {
    this.id = nextId();
    this.defaultUsername = defaultUsername;
    //this.passphrase = passphrase;
    jsch = new JSch();
    loadIdentity(identityKeyFilename, passphrase);
    if (Files.exists(KNOWN_HOSTS_PATH)) {
      jsch.setKnownHosts(KNOWN_HOSTS_PATH.toString());
    }
  }

  public int getId() {
    return id;
  }

  public SshSession openSession(String server) throws JSchException {
    return new SshSession(this, server, defaultUsername);
  }

  @SuppressWarnings("unused")
  public SshSession openSession(String server, String remoteUsername) throws JSchException {
    return new SshSession(this, server, remoteUsername);
  }

  @SuppressWarnings("unused")
  public SshEnvironment dbgOn() {
    dbgIsOn = true;
    return this;
  }

  @SuppressWarnings("unused")
  public  SshEnvironment dbgOff() {
    dbgIsOn = false;
    return this;
  }

  @SuppressWarnings("unused")
  public SshEnvironment loadDiscoveredIdentity() throws JSchException {
    if (Files.exists(IDENTITY_PATH)) {
      return loadIdentity(IDENTITY_PATH);
    }
    return this;
  }

  public SshEnvironment loadIdentity(String identityFileName, byte[] passphrase) throws JSchException {
    return loadIdentity(SSH_CFG_PATH.resolve(identityFileName), passphrase);
  }

  public SshEnvironment loadIdentity(Path path) throws JSchException {
    return loadIdentity(path, null);
  }
  public SshEnvironment loadIdentity(Path path, byte[] passphrase) throws JSchException {
    if (!Files.exists(path)) {
      System.err.printf("Error: identity file [%s] not found!%n", path);
    }
    if (passphrase != NO_PASSPHRASE) {
      dbg("Loading identity: " + path + " with given passphrase ...");
      jsch.addIdentity(path.toString(), passphrase);
    } else {
      dbg("Loading identity: " + path + "...");
      jsch.addIdentity(path.toString());
    }
    if (dbgIsOn) {
      dbg("#Identities loaded -> " + jsch.getIdentityRepository().getIdentities().size());
      for (Object name : jsch.getIdentityNames() ) {
        dbg("identity -> " + name);
      }
    }
    return this;
  }

  private static synchronized int  nextId() {
    return nextId++;
  }

  JSch getJsch() {
    return jsch;
  }

  void dbg(String msg) {
    if (dbgIsOn) {
      System.err.println("(DBG) - " +msg);
    }
  }
}
