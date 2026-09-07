package br.com.fiap.challengetotvsv2.repository;

import br.com.fiap.challengetotvsv2.model.InsightEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IInsightRepository extends JpaRepository<InsightEntity, UUID> {
    public Optional<InsightEntity> findByReuniaoId(UUID reuniaoId);
}
