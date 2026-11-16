package pe.com.edu.prisma.common.exception;

/**
 * The request is well-formed but its content can't be used as sent (e.g. an
 * uploaded spreadsheet with invalid rows): a 400 whose message is shown to
 * the user, so it must be plain Spanish and carry no personal data.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String userFacingMessage) {
        super(userFacingMessage);
    }
}
