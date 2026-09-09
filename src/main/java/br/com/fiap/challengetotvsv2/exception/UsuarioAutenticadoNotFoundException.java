package br.com.fiap.challengetotvsv2.exception;

public class UsuarioAutenticadoNotFoundException extends RuntimeException {
    public UsuarioAutenticadoNotFoundException() {
        super("Usuario autenticado não encontrado");
    }
}
