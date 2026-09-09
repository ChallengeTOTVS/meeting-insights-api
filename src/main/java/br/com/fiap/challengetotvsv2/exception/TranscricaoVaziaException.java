package br.com.fiap.challengetotvsv2.exception;

public class TranscricaoVaziaException extends RuntimeException {

    public TranscricaoVaziaException() {
        super("A reunião não possui transcrição para análise");
    }
}
