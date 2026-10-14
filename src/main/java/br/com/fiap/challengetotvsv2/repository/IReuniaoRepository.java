package br.com.fiap.challengetotvsv2.repository;

import br.com.fiap.challengetotvsv2.model.ReuniaoEntity;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.dto.cliente.ClientFactSourceDto;
import br.com.fiap.challengetotvsv2.enums.StatusReuniao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IReuniaoRepository extends JpaRepository<ReuniaoEntity, UUID> {
    @Query("select count(r.id) from ReuniaoEntity r where r.cliente.id = :clienteId and r.status = :status")
    long contarPorClienteEStatus(@Param("clienteId") UUID clienteId, @Param("status") StatusReuniao status);

    @Query("""
            select new br.com.fiap.challengetotvsv2.dto.cliente.ClientFactSourceDto(
                r.id, r.dataReuniao, i.analiseIndividualJson)
            from ReuniaoEntity r join InsightEntity i on i.reuniao.id = r.id
            where r.cliente.id = :clienteId and r.status = :status
              and i.analiseIndividualJson is not null
            order by r.dataReuniao asc, r.id asc
            """)
    List<ClientFactSourceDto> buscarFatosCompartilhaveis(@Param("clienteId") UUID clienteId,
            @Param("status") StatusReuniao status);

    //criar find by id do cliente
    List<ReuniaoEntity> findByClienteId(UUID clienteId);

    // find by id do usuario
    List<ReuniaoEntity> findByUsuarioId(UUID usuarioId);

    //pega o id da reuniao e o id do usuario presente naquela reuniao(FK na tabela) e traz a reuniao que tem esses dois indices
    @Query("""
            select reuniao from ReuniaoEntity reuniao
            join fetch reuniao.cliente
            join fetch reuniao.usuario
            where reuniao.id = :id and reuniao.usuario.id = :usuarioId
            """)
    Optional<ReuniaoEntity> findByIdAndUsuarioId(
            @Param("id") UUID id,
            @Param("usuarioId") UUID usuarioId
    );


}
