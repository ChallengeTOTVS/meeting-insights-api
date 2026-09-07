package br.com.fiap.challengetotvsv2.repository;

import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IUsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByEmail(String email);

    Boolean existsByEmail(String email);
}
