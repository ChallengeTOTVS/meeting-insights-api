package br.com.fiap.challengetotvsv2.repository;

import br.com.fiap.challengetotvsv2.model.InsightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface IInsightRepository extends JpaRepository<InsightEntity, UUID> {
    public Optional<InsightEntity> findByReuniaoId(UUID reuniaoId);

    @Query("""
            select insight from InsightEntity insight
            join fetch insight.reuniao reuniao
            where reuniao.cliente.id = :clienteId and reuniao.usuario.id = :usuarioId
            order by reuniao.dataReuniao asc
            """)
    List<InsightEntity> findHistoricoComReuniaoPorClienteEUsuario(
            @Param("clienteId") UUID clienteId,
            @Param("usuarioId") UUID usuarioId
    );
}
