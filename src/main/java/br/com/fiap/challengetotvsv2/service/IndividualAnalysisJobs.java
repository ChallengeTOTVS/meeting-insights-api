package br.com.fiap.challengetotvsv2.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IndividualAnalysisJobs {
    private final JdbcTemplate jdbc;
    @PostConstruct
    void initializeStorage() {
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("db/migration/V3__individual_analysis.sql"))
                .execute(java.util.Objects.requireNonNull(jdbc.getDataSource()));
    }
    public boolean claim(UUID id) {
        return jdbc.update("INSERT INTO individual_analysis_job (reuniao_id,versao,status) VALUES (?,2,'PROCESSANDO') ON CONFLICT (reuniao_id,versao) DO NOTHING", id) == 1;
    }
    public String status(UUID id) {
        var rows = jdbc.query("SELECT CASE WHEN status='PROCESSANDO' AND criado_em < CURRENT_TIMESTAMP - INTERVAL '15 minutes' THEN 'INTERROMPIDO' ELSE status END FROM individual_analysis_job WHERE reuniao_id=? AND versao=2",
                (rs, n) -> rs.getString(1), id);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public void finish(UUID id, String status) {
        jdbc.update("UPDATE individual_analysis_job SET status=?, atualizado_em=CURRENT_TIMESTAMP WHERE reuniao_id=? AND versao=2", status, id);
    }
}
