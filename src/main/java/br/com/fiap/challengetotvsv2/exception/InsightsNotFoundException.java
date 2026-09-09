package br.com.fiap.challengetotvsv2.exception;

import java.util.UUID;

public class InsightsNotFoundException extends RuntimeException {

    public InsightsNotFoundException(UUID reuniaoId) {
        super("Insights não encontrados para a reunião: " + reuniaoId);
    }
}
