package br.com.grupo5.Quality.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.agendamento.prazos.atraso-inicial-ms=3600000"
})
class FlywayPostgresqlIT {

    private static final List<String> TABELAS_ESPERADAS = List.of(
            "artefatos",
            "atividades_plano",
            "auditoria_documentos_referencia",
            "auditorias",
            "checklists",
            "comunicacoes_nc",
            "configuracoes_classificacao",
            "documentos",
            "encaminhamentos_nc",
            "escalonamentos_nc",
            "feriados_plano",
            "imagens_plano",
            "itens_checklist",
            "itens_versao_execucao",
            "nao_conformidades",
            "notificacoes",
            "participacao_plano_papeis",
            "participacoes_plano",
            "planos",
            "resolucoes_nc",
            "respostas_auditoria",
            "roles",
            "usuario_roles",
            "usuarios",
            "versoes_execucao_checklist"
    );

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("quality")
            .withUsername("quality")
            .withPassword("quality");

    @DynamicPropertySource
    static void configurarPostgres(DynamicPropertyRegistry propriedades) {
        propriedades.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        propriedades.add("spring.datasource.username", POSTGRES::getUsername);
        propriedades.add("spring.datasource.password", POSTGRES::getPassword);
        propriedades.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveAplicarMigracaoEValidarMapeamentoJpa() {
        Integer migracoesAplicadas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success",
                Integer.class
        );

        String ultimaVersao = jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success "
                        + "ORDER BY installed_rank DESC LIMIT 1",
                String.class
        );

        List<String> tabelasCriadas = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public'",
                String.class
        );

        assertEquals(18, migracoesAplicadas);
        assertEquals("18", ultimaVersao);
        assertTrue(tabelasCriadas.containsAll(TABELAS_ESPERADAS));
    }

    @Test
    void devePermitirExcluirAgregadosComSeusRelacionamentos() {
        List<String> restricoesEmCascata = List.of(
                "fk_participacao_plano_plano",
                "fk_documento_plano",
                "fk_artefato_documento",
                "fk_artefato_auditor",
                "fk_auditoria_artefato",
                "fk_auditoria_auditor",
                "fk_checklist_auditoria",
                "fk_item_checklist_checklist",
                "fk_resposta_auditoria_auditoria",
                "fk_nc_resposta",
                "fk_comunicacao_nc_nao_conformidade",
                "fk_resolucao_nc_nao_conformidade",
                "fk_escalonamento_nc_nao_conformidade",
                "fk_notificacao_nao_conformidade",
                "fk_encaminhamento_nc_nao_conformidade",
                "fk_atividade_autor",
                "fk_versao_execucao_autor"
        );

        for (String restricao : restricoesEmCascata) {
            String regra = jdbcTemplate.queryForObject(
                    "SELECT delete_rule FROM information_schema.referential_constraints "
                            + "WHERE constraint_schema = 'public' "
                            + "AND constraint_name = ?",
                    String.class,
                    restricao
            );
            assertEquals("CASCADE", regra, restricao);
        }
    }
}
