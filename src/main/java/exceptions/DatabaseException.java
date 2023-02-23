package exceptions;

public class DatabaseException extends BaseException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, int status) {
        super(message, status);
    }
}
