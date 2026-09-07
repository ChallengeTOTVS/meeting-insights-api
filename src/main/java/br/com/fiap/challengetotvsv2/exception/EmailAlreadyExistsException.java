package br.com.fiap.challengetotvsv2.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("O e-mail já está cadastrado: " + email);
    }
}