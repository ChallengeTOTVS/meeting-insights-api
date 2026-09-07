package br.com.fiap.challengetotvsv2.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "insights")
public class InsightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reuniao_id", nullable = false, unique = true)
    private ReuniaoEntity reuniao;

    private Double scoreChurn;

    private String nivelRisco;

    @Column(columnDefinition = "TEXT")
    private String justificativaRisco;

    private String sentimentoGeral;

    @Column(columnDefinition = "TEXT")
    private String resumo;

    @Column(columnDefinition = "TEXT")
    private String topicosPrincipais;

    @Column(columnDefinition = "TEXT")
    private String objecoes;

    @Column(columnDefinition = "TEXT")
    private String acoesRecomendadas;

    private LocalDateTime dataCriacao;

}
