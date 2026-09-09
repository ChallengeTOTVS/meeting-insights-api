package br.com.fiap.challengetotvsv2.repository;

import br.com.fiap.challengetotvsv2.model.ReuniaoEntity;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IReuniaoRepository extends JpaRepository<ReuniaoEntity, UUID> {
    //criar find by id do cliente
    List<ReuniaoEntity> findByClienteId(UUID clienteId);

    // find by id do usuario
    List<ReuniaoEntity> findByUsuarioId(UUID usuarioId);

    //pega o id da reuniao e o id do usuario presente naquela reuniao(FK na tabela) e traz a reuniao que tem esses dois indices
    Optional<ReuniaoEntity> findByIdAndUsuarioId(UUID id, UUID usuarioId);


}
