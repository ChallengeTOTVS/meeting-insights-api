package br.com.fiap.challengetotvsv2.model;

import br.com.fiap.challengetotvsv2.enums.StatusReuniao;
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
@Table(name = "reuniao")
public class ReuniaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    private LocalDateTime dataReuniao;

    private String nomeArquivo;

    @Column(columnDefinition = "TEXT")
    private String transcricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Enumerated(EnumType.STRING)
    private StatusReuniao status;

    private LocalDateTime dataCriacao;

}
