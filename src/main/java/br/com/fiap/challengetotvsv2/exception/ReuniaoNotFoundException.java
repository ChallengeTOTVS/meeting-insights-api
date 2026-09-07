package br.com.fiap.challengetotvsv2.exception;

import java.util.UUID;

public class ReuniaoNotFoundException extends RuntimeException {
    public ReuniaoNotFoundException(UUID id) {
        super("Reuniao não encontrada com o id: " + id);
    }
}
