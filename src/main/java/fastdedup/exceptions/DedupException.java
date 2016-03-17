package fastdedup.exceptions;

/**
 * Exception to throw when MarkDuplicates program error happened.
 * @Author Wang Bingchen
 * @Date 2016/2/25.
 */
public class DedupException extends RuntimeException{

    public DedupException(final String message) {
        super(message);
    }

    public DedupException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
