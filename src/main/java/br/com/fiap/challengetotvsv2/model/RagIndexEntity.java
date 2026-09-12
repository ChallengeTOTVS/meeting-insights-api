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
@Table(name = "rag_index")
public class RagIndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reuniao_id", nullable = false, unique = true)
    private ReuniaoEntity reuniao;

    @Column(nullable = false, length = 64)
    private String hashTranscricao;

    @Column(nullable = false)
    private Integer quantidadeChunks;

    @Column(nullable = false)
    private LocalDateTime dataIndexacao;
}
