package br.com.fiap.challengetotvsv2.repository;

import br.com.fiap.challengetotvsv2.model.RagIndexEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IRagIndexRepository extends JpaRepository<RagIndexEntity, UUID> {

    Optional<RagIndexEntity> findByReuniaoId(UUID reuniaoId);
}
