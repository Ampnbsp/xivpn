package cn.gov.xivpn2.service.sharelink;

public class BadShareLinkException extends Exception {
    public BadShareLinkException(String message) {
        super(message);
    }

    public BadShareLinkException(Exception e) {
        super(e);
    }

    public BadShareLinkException(String message, Exception e) {
        super(message, e);
    }
}
