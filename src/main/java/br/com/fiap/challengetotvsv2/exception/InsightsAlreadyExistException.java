package br.com.fiap.challengetotvsv2.exception;

import java.util.UUID;

public class InsightsAlreadyExistException extends RuntimeException {
    public InsightsAlreadyExistException(UUID reuniaoId) {
        super("Insights já foram gerados para essa reunião" + reuniaoId);
    }
}
