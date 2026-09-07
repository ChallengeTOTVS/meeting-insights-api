package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.reuniao.ReuniaoRequestDto;
import br.com.fiap.challengetotvsv2.dto.reuniao.ReuniaoResponseDto;
import br.com.fiap.challengetotvsv2.enums.StatusReuniao;
import br.com.fiap.challengetotvsv2.exception.ClienteNotFoundException;
import br.com.fiap.challengetotvsv2.exception.ReuniaoNotFoundException;
import br.com.fiap.challengetotvsv2.model.ClienteEntity;
import br.com.fiap.challengetotvsv2.model.ReuniaoEntity;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.IClienteRepository;
import br.com.fiap.challengetotvsv2.repository.IReuniaoRepository;
import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class ReuniaoService {

    private final IClienteRepository clienteRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IReuniaoRepository reuniaoRepository;

    public ReuniaoResponseDto cadastrarReuniao(ReuniaoRequestDto reuniaoRequestDto) {

        String emailUsuario = SecurityContextHolder.getContext().getAuthentication().getName();

        UsuarioEntity usuario = usuarioRepository.findByEmail((emailUsuario))
                .orElseThrow(() -> new RuntimeException("Usuario autenticado não encontrado"));

        ClienteEntity cliente = clienteRepository.findById(reuniaoRequestDto.clienteId())
                .orElseThrow(()-> new ClienteNotFoundException(reuniaoRequestDto.clienteId()));

        ReuniaoEntity reuniao = ReuniaoEntity.builder()
                .titulo(reuniaoRequestDto.titulo())
                .dataReuniao(reuniaoRequestDto.dataReuniao())
                .transcricao(reuniaoRequestDto.transcricao())
                .cliente(cliente)
                .usuario(usuario)
                .status(StatusReuniao.PENDENTE)
                .dataCriacao(LocalDateTime.now())
                .build();

        ReuniaoEntity reuniaoSalva = reuniaoRepository.save(reuniao);

        return converterParaResponse(reuniaoSalva);
    }

    private ReuniaoResponseDto converterParaResponse(ReuniaoEntity reuniao) {

        return ReuniaoResponseDto.builder()
                .id(reuniao.getId())
                .titulo(reuniao.getTitulo())
                .dataReuniao(reuniao.getDataReuniao())
                .nomeArquivo(reuniao.getNomeArquivo())
                .transcricao(reuniao.getTranscricao())
                .clienteId(reuniao.getCliente().getId())
                .clienteNome(reuniao.getCliente().getNome())
                .usuarioId(reuniao.getUsuario().getId())
                .usuarioNome(reuniao.getUsuario().getNome())
                .status(reuniao.getStatus())
                .dataCriacao(reuniao.getDataCriacao())
                .build();
    }

    public List<ReuniaoResponseDto> listarMinhasReunioes() {
        String emailUsuario = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        UsuarioEntity usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(()-> new RuntimeException("Usuario autenticado não encontrado"));

        return reuniaoRepository.findByUsuarioId(usuario.getId())
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    public ReuniaoResponseDto buscarReuniaoPorId(UUID id) {
        String emailUsuario = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        UsuarioEntity usuario = reuniaoRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RuntimeException("Usuário autenticado não encontrado"));

        ReuniaoEntity reuniao = reuniaoRepository
                .findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new ReuniaoNotFoundException(id));

        return converterParaResponse(reuniao);
    }


}
