package markduplicates;

/**
 * Exception to throw when MarkDuplicates program error happened.
 * @Author Wang Bingchen
 * @Date 2016/2/25.
 */
public class PicardException extends RuntimeException{

    public PicardException(final String message) {
        super(message);
    }

    public PicardException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
