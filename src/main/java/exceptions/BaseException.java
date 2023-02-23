package exceptions;

public class BaseException extends Exception {
    String message;

    int status = 50000;

    public BaseException(String message) {
        this.message = message;
    }

    public BaseException(String message, int status) {
        this.message = message;
        this.status = status;
    }
    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }
}
