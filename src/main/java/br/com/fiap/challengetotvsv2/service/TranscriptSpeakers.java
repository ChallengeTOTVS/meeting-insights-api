package br.com.fiap.challengetotvsv2.service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Descobre QUEM falou um trecho olhando a própria transcrição.
 *
 * Transcrições costumam ter uma fala por linha, no formato "Nome: texto". A IA, ao citar uma
 * evidência, quase sempre copia só o texto ("Hoje usamos o ERP Omie..."), sem o nome. Antes,
 * o participante só era aceito se o nome estivesse DENTRO do trecho citado; por isso aparecia
 * "Participante não identificado" mesmo com o nome escrito na linha.
 *
 * Aqui o nome vem da transcrição, nunca da IA: procuramos a linha que contém o trecho e lemos
 * o rótulo antes dos dois-pontos. Não há adivinhação: sem rótulo, devolve null.
 */
final class TranscriptSpeakers {
    private TranscriptSpeakers() {}

    // "Eduardo Nunes:", "Camila Rocha (TOTVS):", "Patrícia:" — até 5 palavras antes dos dois-pontos.
    private static final Pattern LABEL = Pattern.compile("^([\\p{L}][\\p{L}'.\\-]*(?: [\\p{L}'.\\-()]+){0,4})\\s*:");

    private static final java.util.Set<String> HEADER_WORDS = java.util.Set.of(
            "data", "hora", "horário", "empresa", "cliente", "contato", "segmento", "participantes",
            "participante", "observação", "obs", "reunião", "transcrição", "assunto", "título", "local",
            "pauta", "produto", "produtos", "concorrente", "vendedor", "vendedora", "outros", "duração");

    static String speakerOf(String source, String quote) {
        if (source == null || quote == null || quote.isBlank()) return null;
        String q = IndividualAnalysisValidator.normalize(quote);
        // A citação pode começar com o próprio rótulo; os primeiros 60 caracteres bastam para achar a linha.
        String key = q.length() > 60 ? q.substring(0, 60) : q;
        for (String line : source.split("\\R")) {
            String n = IndividualAnalysisValidator.normalize(line);
            if (!n.contains(key)) continue;
            var m = LABEL.matcher(n);
            if (!m.find()) return null;
            String label = m.group(1).strip();
            // Linhas de cabeçalho ("Data:", "Empresa cliente:", "Observação:") não são falas.
            String first = label.split(" ")[0].toLowerCase(Locale.ROOT);
            return HEADER_WORDS.contains(first) ? null : label;
        }
        return null;
    }

    /** "Eduardo" e "Eduardo Nunes" são a mesma pessoa; comparação sem maiúsculas. */
    static boolean same(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return false;
        String x = IndividualAnalysisValidator.normalize(a).toLowerCase(Locale.ROOT);
        String y = IndividualAnalysisValidator.normalize(b).toLowerCase(Locale.ROOT);
        return x.contains(y) || y.contains(x);
    }
}
