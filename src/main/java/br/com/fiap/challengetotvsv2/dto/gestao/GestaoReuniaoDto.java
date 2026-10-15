package br.com.fiap.challengetotvsv2.dto.gestao;

import br.com.fiap.challengetotvsv2.enums.StatusReuniao;

import java.time.LocalDateTime;
import java.util.UUID;

/** Linha da lista "Reuniões da equipe" do administrador (sem a transcrição, para ficar leve). */
public record GestaoReuniaoDto(UUID id, String titulo, LocalDateTime dataReuniao, UUID clienteId, String cliente,
                               String empresa, UUID vendedorId, String vendedor, StatusReuniao status,
                               Integer risco, String nivelRisco, String sentimento, int pendenciasAbertas) {}
