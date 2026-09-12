package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.exception.RagVectorStoreException;
import br.com.fiap.challengetotvsv2.exception.TranscricaoVaziaException;
import br.com.fiap.challengetotvsv2.model.RagIndexEntity;
import br.com.fiap.challengetotvsv2.model.ReuniaoEntity;
import br.com.fiap.challengetotvsv2.repository.IRagIndexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RagService {

    private static final String DOCUMENT_TYPE = "TRANSCRICAO";
    private static final int CHUNK_SIZE = 500;
    private static final int TOP_K_PER_QUERY = 4;
    private static final double SIMILARITY_THRESHOLD = 0.55;

    private final VectorStore vectorStore;
    private final IRagIndexRepository ragIndexRepository;

    @Transactional
    public void indexarReuniao(ReuniaoEntity reuniao) {
        validarTranscricao(reuniao);

        String hash = calcularHash(reuniao.getTranscricao());
        Optional<RagIndexEntity> indiceExistente = ragIndexRepository.findByReuniaoId(reuniao.getId());
        if (indiceExistente.isPresent() && indiceExistente.get().getHashTranscricao().equals(hash)) {
            return;
        }

        List<Document> chunks = dividirEmChunks(reuniao, hash);
        if (chunks.isEmpty()) {
            throw new TranscricaoVaziaException();
        }

        try {
            indiceExistente.ifPresent(this::removerChunksAnteriores);
            vectorStore.add(chunks);

            RagIndexEntity indice = indiceExistente.orElseGet(RagIndexEntity::new);
            indice.setReuniao(reuniao);
            indice.setHashTranscricao(hash);
            indice.setQuantidadeChunks(chunks.size());
            indice.setDataIndexacao(java.time.LocalDateTime.now());
            ragIndexRepository.save(indice);
        } catch (Exception exception) {
            throw new RagVectorStoreException("Não foi possível indexar a transcrição no pgvector", exception);
        }
    }

    public List<Document> buscarContextoHistorico(ReuniaoEntity reuniao) {
        validarTranscricao(reuniao);
        String filtro = "clienteId == '%s' && usuarioId == '%s' && tipoDocumento == '%s' && "
                + "reuniaoId != '%s' && dataReuniao < '%s'";
        String expressaoFiltro = filtro.formatted(
                reuniao.getCliente().getId(),
                reuniao.getUsuario().getId(),
                DOCUMENT_TYPE,
                reuniao.getId(),
                reuniao.getDataReuniao()
        );

        try {
            Map<String, Document> encontrados = new LinkedHashMap<>();
            for (String consulta : consultasHistoricas(reuniao)) {
                List<Document> documentos = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(consulta)
                        .topK(TOP_K_PER_QUERY)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .filterExpression(expressaoFiltro)
                        .build());
                documentos.forEach(documento -> encontrados.putIfAbsent(documento.getId(), documento));
            }
            return encontrados.values().stream().limit(8).toList();
        } catch (Exception exception) {
            throw new RagVectorStoreException("Não foi possível recuperar o histórico no pgvector", exception);
        }
    }

    List<Document> dividirEmChunks(ReuniaoEntity reuniao, String hashTranscricao) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reuniaoId", reuniao.getId().toString());
        metadata.put("clienteId", reuniao.getCliente().getId().toString());
        metadata.put("usuarioId", reuniao.getUsuario().getId().toString());
        metadata.put("dataReuniao", reuniao.getDataReuniao().toString());
        metadata.put("titulo", reuniao.getTitulo());
        metadata.put("nomeCliente", reuniao.getCliente().getNome());
        metadata.put("tipoDocumento", DOCUMENT_TYPE);

        Document original = new Document(reuniao.getTranscricao(), metadata);
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(CHUNK_SIZE)
                .withMinChunkSizeChars(300)
                .withMinChunkLengthToEmbed(20)
                .withMaxNumChunks(500)
                .withKeepSeparator(true)
                .build();

        List<Document> documentosDivididos = splitter.apply(List.of(original));
        List<Document> chunks = new ArrayList<>();
        for (int indice = 0; indice < documentosDivididos.size(); indice++) {
            Document chunk = documentosDivididos.get(indice);
            Map<String, Object> metadadosChunk = new HashMap<>(chunk.getMetadata());
            metadadosChunk.put("chunkIndex", indice);
            metadadosChunk.put("referencia", referencia(reuniao.getId(), indice));
            chunks.add(new Document(idDoChunk(reuniao.getId(), hashTranscricao, indice), chunk.getText(), metadadosChunk));
        }
        return chunks;
    }

    private List<String> consultasHistoricas(ReuniaoEntity reuniao) {
        String trechoAtual = reuniao.getTranscricao().length() > 1600
                ? reuniao.getTranscricao().substring(0, 1600)
                : reuniao.getTranscricao();
        return List.of(
                reuniao.getTitulo() + "\n" + trechoAtual,
                "objeções, dúvidas repetidas, pendências, problemas sem resolução, "
                        + "risco de churn, sentimento e prioridades do cliente",
                "compromissos, prazos, responsáveis, proposta comercial, preço, concorrentes e próximos passos"
        );
    }

    private void removerChunksAnteriores(RagIndexEntity indice) {
        List<String> ids = IntStream.range(0, indice.getQuantidadeChunks())
                .mapToObj(chunk -> idDoChunk(indice.getReuniao().getId(), indice.getHashTranscricao(), chunk))
                .toList();
        vectorStore.delete(ids);
    }

    private void validarTranscricao(ReuniaoEntity reuniao) {
        if (reuniao.getTranscricao() == null || reuniao.getTranscricao().isBlank()) {
            throw new TranscricaoVaziaException();
        }
    }

    private String calcularHash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível", exception);
        }
    }

    private String idDoChunk(UUID reuniaoId, String hash, int chunkIndex) {
        return UUID.nameUUIDFromBytes((reuniaoId + ":" + hash + ":" + chunkIndex)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String referencia(UUID reuniaoId, int chunkIndex) {
        return "R-" + reuniaoId + "-C-" + chunkIndex;
    }
}
