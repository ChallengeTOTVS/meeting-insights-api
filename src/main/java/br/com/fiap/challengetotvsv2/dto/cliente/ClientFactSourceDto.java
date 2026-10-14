package br.com.fiap.challengetotvsv2.dto.cliente;

import java.time.LocalDateTime;
import java.util.UUID;

/** Minimal database projection for cross-participant client context. */
public record ClientFactSourceDto(UUID reuniaoId, LocalDateTime dataReuniao, String analiseIndividualJson) {}
