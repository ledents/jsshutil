package be.sle.sshutil;

public class SshException extends Exception {
  SshException(Exception e){
    super(e.getMessage(), e);
  }
}
