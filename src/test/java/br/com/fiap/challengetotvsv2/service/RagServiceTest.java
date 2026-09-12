package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.exception.TranscricaoVaziaException;
import br.com.fiap.challengetotvsv2.model.ClienteEntity;
import br.com.fiap.challengetotvsv2.model.RagIndexEntity;
import br.com.fiap.challengetotvsv2.model.ReuniaoEntity;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.IRagIndexRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private IRagIndexRepository ragIndexRepository;

    @InjectMocks
    private RagService ragService;

    @Test
    void deveDividirTranscricaoLongaEmChunksComMetadadosDeSeguranca() {
        ReuniaoEntity reuniao = reuniaoComTranscricao("Frase sobre integração. ".repeat(1_200));

        List<Document> chunks = ragService.dividirEmChunks(reuniao, "hash-de-teste");

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.getMetadata())
                    .containsEntry("reuniaoId", reuniao.getId().toString())
                    .containsEntry("clienteId", reuniao.getCliente().getId().toString())
                    .containsEntry("usuarioId", reuniao.getUsuario().getId().toString())
                    .containsEntry("tipoDocumento", "TRANSCRICAO")
                    .containsKeys("dataReuniao", "titulo", "nomeCliente", "chunkIndex", "referencia");
        });
    }

    @Test
    void deveIndexarUmaReuniao() {
        ReuniaoEntity reuniao = reuniaoComTranscricao("Cliente pediu uma proposta com detalhes da integração.");
        when(ragIndexRepository.findByReuniaoId(reuniao.getId())).thenReturn(Optional.empty());

        ragService.indexarReuniao(reuniao);

        verify(vectorStore).add(anyList());
        ArgumentCaptor<RagIndexEntity> indice = ArgumentCaptor.forClass(RagIndexEntity.class);
        verify(ragIndexRepository).save(indice.capture());
        assertThat(indice.getValue().getQuantidadeChunks()).isEqualTo(1);
        assertThat(indice.getValue().getHashTranscricao()).hasSize(64);
    }

    @Test
    void naoDeveDuplicarEmbeddingQuandoATranscricaoNaoMudou() throws Exception {
        ReuniaoEntity reuniao = reuniaoComTranscricao("A mesma transcrição que já foi indexada.");
        RagIndexEntity indice = RagIndexEntity.builder()
                .reuniao(reuniao)
                .hashTranscricao(hash(reuniao.getTranscricao()))
                .quantidadeChunks(1)
                .dataIndexacao(LocalDateTime.now())
                .build();
        when(ragIndexRepository.findByReuniaoId(reuniao.getId())).thenReturn(Optional.of(indice));

        ragService.indexarReuniao(reuniao);

        verifyNoInteractions(vectorStore);
        verify(ragIndexRepository, never()).save(any());
    }

    @Test
    void deveAplicarClienteEUsuarioNoFiltroDaBuscaHistorica() {
        ReuniaoEntity reuniao = reuniaoComTranscricao("A integração falhou novamente e o cliente pediu retorno.");
        Document resultado = new Document("Trecho histórico", java.util.Map.of(
                "referencia", "R-anterior-C-0",
                "reuniaoId", UUID.randomUUID().toString(),
                "clienteId", reuniao.getCliente().getId().toString(),
                "usuarioId", reuniao.getUsuario().getId().toString(),
                "tipoDocumento", "TRANSCRICAO"
        ));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(resultado));

        List<Document> encontrados = ragService.buscarContextoHistorico(reuniao);

        assertThat(encontrados).containsExactly(resultado);
        ArgumentCaptor<SearchRequest> busca = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore, times(3)).similaritySearch(busca.capture());
        assertThat(busca.getAllValues()).allSatisfy(request ->
                assertThat(request.getFilterExpression().toString())
                        .contains(reuniao.getCliente().getId().toString())
                        .contains(reuniao.getUsuario().getId().toString())
                        .contains(reuniao.getId().toString())
        );
    }

    @Test
    void deveRecusarReuniaoSemTranscricao() {
        ReuniaoEntity reuniao = reuniaoComTranscricao("   ");

        assertThatThrownBy(() -> ragService.indexarReuniao(reuniao))
                .isInstanceOf(TranscricaoVaziaException.class);
    }

    private ReuniaoEntity reuniaoComTranscricao(String transcricao) {
        ClienteEntity cliente = ClienteEntity.builder().id(UUID.randomUUID()).nome("Cliente A").empresa("Empresa A").build();
        UsuarioEntity usuario = UsuarioEntity.builder().id(UUID.randomUUID()).email("vendedor@empresa.com").build();
        return ReuniaoEntity.builder()
                .id(UUID.randomUUID())
                .titulo("Reunião de acompanhamento")
                .dataReuniao(LocalDateTime.of(2026, 9, 10, 10, 0))
                .transcricao(transcricao)
                .cliente(cliente)
                .usuario(usuario)
                .build();
    }

    private String hash(String valor) throws Exception {
        return java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
    }
}
