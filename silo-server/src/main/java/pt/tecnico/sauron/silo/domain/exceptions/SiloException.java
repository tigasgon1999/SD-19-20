package pt.tecnico.sauron.silo.domain.exceptions;

public class SiloException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final ErrorMessage errorMessage;

    public SiloException(ErrorMessage errorMessage) {
        super(errorMessage.label);
        this.errorMessage = errorMessage;
    }

    public SiloException(ErrorMessage errorMessage, String arg) {
        super(String.format(errorMessage.label, arg));
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }
}