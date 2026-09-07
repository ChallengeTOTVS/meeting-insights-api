package br.com.fiap.challengetotvsv2.repository;

import br.com.fiap.challengetotvsv2.model.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IClienteRepository extends JpaRepository<ClienteEntity, UUID> {


}
