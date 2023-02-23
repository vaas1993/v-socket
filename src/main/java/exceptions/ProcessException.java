package exceptions;

public class ProcessException extends BaseException {
    public ProcessException(String message) {
        super(message);
    }

    public ProcessException(String message, int status) {
        super(message, status);
    }
}
