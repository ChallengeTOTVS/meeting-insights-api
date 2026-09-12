-- Additive only. Existing insight values are never overwritten or regenerated.
ALTER TABLE IF EXISTS insights ADD COLUMN IF NOT EXISTS analise_individual_json TEXT;
CREATE TABLE IF NOT EXISTS individual_analysis_job (
 reuniao_id UUID NOT NULL,
 versao INTEGER NOT NULL,
 status VARCHAR(24) NOT NULL,
 criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY (reuniao_id, versao)
);
