package br.com.fiap.challengetotvsv2.exception;

import java.util.UUID;

public class ClienteNotFoundException extends RuntimeException {
    public ClienteNotFoundException(UUID id) {
        super("Cliente não encontrado com o id: " + id);
    }
}
