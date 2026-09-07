package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.cliente.ClienteRequestDto;
import br.com.fiap.challengetotvsv2.dto.cliente.ClienteResponseDto;
import br.com.fiap.challengetotvsv2.exception.ClienteNotFoundException;
import br.com.fiap.challengetotvsv2.model.ClienteEntity;
import br.com.fiap.challengetotvsv2.repository.IClienteRepository;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@Builder
@Service
// tem como funcao criar a regra de negocio, pegar os dados da entity, transformar em dto e enviar para o controller
public class ClienteService {

    private final IClienteRepository clienteRepository;

    public ClienteResponseDto cadastrarCliente(ClienteRequestDto clienteRequestDto){
        ClienteEntity cliente = ClienteEntity.builder()
                .nome(clienteRequestDto.nome())
                .empresa(clienteRequestDto.empresa())
                .email(clienteRequestDto.email())
                .segmento(clienteRequestDto.segmento())
                .build();

        ClienteEntity clienteSalvo = clienteRepository.save(cliente);

        return ClienteResponseDto.builder()
                .id(clienteSalvo.getId())
                .nome(clienteSalvo.getNome())
                .empresa(clienteSalvo.getEmpresa())
                .email(clienteSalvo.getEmail())
                .segmento(clienteSalvo.getSegmento())
                .build();
    }

    public List<ClienteResponseDto> listarTodos(){

        return clienteRepository.findAll()
                .stream()
                .map(cliente -> ClienteResponseDto.builder()
                        .id(cliente.getId())
                        .nome(cliente.getNome())
                        .email(cliente.getEmail())
                        .empresa(cliente.getEmpresa())
                        .segmento(cliente.getSegmento())
                        .build())
                .toList();
    };

    public ClienteResponseDto buscarPorId(UUID id){
        ClienteEntity cliente = clienteRepository.findById(id).orElseThrow(() -> new ClienteNotFoundException(id));

        return ClienteResponseDto.builder()
                .id(cliente.getId())
                .nome(cliente.getNome())
                .empresa(cliente.getEmpresa())
                .email(cliente.getEmail())
                .segmento(cliente.getSegmento())
                .build();
    }

}
