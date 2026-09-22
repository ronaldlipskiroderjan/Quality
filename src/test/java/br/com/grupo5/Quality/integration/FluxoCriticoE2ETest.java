package br.com.grupo5.Quality.integration;

import br.com.grupo5.Quality.QualityApplication;
import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;
import br.com.grupo5.Quality.database.enums.DecisaoValidacao;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.ResultadoItem;
import br.com.grupo5.Quality.dto.request.ArtefatoRequestDTO;
import br.com.grupo5.Quality.dto.request.AtualizarNaoConformidadeRequestDTO;
import br.com.grupo5.Quality.dto.request.AtualizarItemVersaoExecucaoRequestDTO;
import br.com.grupo5.Quality.dto.request.CriarNaoConformidadeRequestDTO;
import br.com.grupo5.Quality.dto.request.InformarResolucaoRequestDTO;
import br.com.grupo5.Quality.dto.request.ItemChecklistRequestDTO;
import br.com.grupo5.Quality.dto.request.NovoParticipanteRequestDTO;
import br.com.grupo5.Quality.dto.request.PapeisParticipanteRequestDTO;
import br.com.grupo5.Quality.dto.request.PlanoRequestDTO;
import br.com.grupo5.Quality.dto.request.RespostaAuditoriaRequestDTO;
import br.com.grupo5.Quality.dto.request.SalvarVersaoExecucaoRequestDTO;
import br.com.grupo5.Quality.dto.request.UsuarioLoginRequestDTO;
import br.com.grupo5.Quality.dto.request.UsuarioRequestDTO;
import br.com.grupo5.Quality.dto.request.UsuarioUpdateRequestDTO;
import br.com.grupo5.Quality.dto.request.ValidarResolucaoRequestDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = QualityApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:quality-e2e;"
                + "MODE=PostgreSQL;DB_CLOSE_DELAY=-1;"
                + "DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
@DirtiesContext
class FluxoCriticoE2ETest {

    private static final String SENHA = "SenhaSegura123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveExecutarFluxoCriticoDoMvpPorHttp() throws Exception {
        String sufixo = UUID.randomUUID().toString();
        String emailGestor = "qualidade-" + sufixo + "@quality.test";
        String emailAuditor = "auditor-" + sufixo + "@quality.test";
        String emailResponsavel = "responsavel-" + sufixo + "@quality.test";

        registrar("Responsável de qualidade", emailGestor);
        registrar("Auditor do plano", emailAuditor);
        registrar("Responsável pela resolução", emailResponsavel);
        String tokenGestor = autenticar(emailGestor);
        String tokenAuditor = autenticar(emailAuditor);
        String tokenResponsavel = autenticar(emailResponsavel);

        String emailGestorAtualizado = "conta-" + sufixo + "@quality.test";
        JsonNode perfilAtualizado = putJson(
                "/v1/usuarios/me",
                tokenGestor,
                new UsuarioUpdateRequestDTO(
                        "Responsável de qualidade atualizado",
                        emailGestorAtualizado
                ),
                200
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                emailGestorAtualizado,
                perfilAtualizado.path("email").asText()
        );
        MockMultipartFile fotoPerfil = new MockMultipartFile(
                "imagem",
                "perfil.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3, 4}
        );
        mockMvc.perform(multipart("/v1/usuarios/me/imagem")
                        .file(fotoPerfil)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/v1/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temImagem").value(true));
        mockMvc.perform(get("/v1/usuarios/me/imagem")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[]{1, 2, 3, 4}));

        JsonNode plano = postJson(
                "/v1/planos",
                tokenGestor,
                new PlanoRequestDTO(
                        "Projeto auditado",
                        "1.0",
                        "Verificar a qualidade do produto",
                        "Auditoria do fluxo crítico do MVP"
                ),
                201
        );
        UUID planoId = uuid(plano, "id");

        JsonNode participantes = getJson(
                "/v1/planos/" + planoId + "/participantes",
                tokenGestor,
                200
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "AUDITOR_RESPONSAVEL_QUALIDADE",
                participantes.path("conteudo").get(0).path("papel").asText()
        );

        JsonNode auditor = postJson(
                "/v1/planos/" + planoId + "/participantes",
                tokenGestor,
                new NovoParticipanteRequestDTO(
                        emailAuditor,
                        PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
                ),
                201
        );
        UUID auditorParticipacaoId = uuid(auditor, "id");

        JsonNode responsavel = postJson(
                "/v1/planos/" + planoId + "/participantes",
                tokenGestor,
                new NovoParticipanteRequestDTO(
                        emailResponsavel,
                        Set.of(PapelPlano.MEMBRO_EQUIPE_RESOLUCAO)
                ),
                201
        );
        UUID responsavelParticipacaoId = uuid(responsavel, "id");

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "especificacao.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Requisitos do produto".getBytes(StandardCharsets.UTF_8)
        );
        String documentoJson = mockMvc.perform(multipart(
                                "/v1/planos/{planoId}/documentos",
                                planoId
                        )
                        .file(arquivo)
                        .param("nome", "Especificação")
                        .param("versao", "1.0")
                        .param("classificacao", "AUDITADO")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(tokenGestor)
                        ))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID documentoId = uuid(
                objectMapper.readTree(documentoJson),
                "id"
        );

        MockMultipartFile referencia = new MockMultipartFile(
                "arquivo",
                "norma.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Norma de qualidade".getBytes(StandardCharsets.UTF_8)
        );
        String referenciaJson = mockMvc.perform(multipart(
                                "/v1/planos/{planoId}/documentos",
                                planoId
                        )
                        .file(referencia)
                        .param("nome", "Norma de referência")
                        .param("versao", "1.0")
                        .param("classificacao", "REFERENCIA")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID referenciaId = uuid(objectMapper.readTree(referenciaJson), "id");

        JsonNode artefato = postJson(
                "/v1/planos/" + planoId + "/artefatos",
                tokenGestor,
                new ArtefatoRequestDTO(
                        documentoId,
                        auditorParticipacaoId,
                        Set.of(referenciaId),
                        "Documento de requisitos",
                        "1.0",
                        LocalDate.now().plusDays(1)
                ),
                201
        );
        UUID artefatoId = uuid(artefato, "id");
        UUID auditoriaId = uuid(artefato, "auditoriaId");

        String auditoriaJson = mockMvc.perform(get(
                                "/v1/planos/{planoId}/artefatos/{artefatoId}"
                                        + "/auditorias/{auditoriaId}",
                                planoId,
                                artefatoId,
                                auditoriaId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAuditor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.checklists.length()").value(1))
                .andExpect(jsonPath("$.checklists[0].status")
                        .value("PUBLICADO"))
                .andExpect(jsonPath("$.checklists[0].itens.length()")
                        .value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode checklist = objectMapper.readTree(auditoriaJson)
                .path("checklists")
                .get(0);
        UUID checklistId = uuid(checklist, "id");
        UUID itemId = UUID.fromString(
                checklist.path("itens").get(0).path("itemId").asText()
        );

        putJson(
                "/v1/planos/" + planoId + "/artefatos/" + artefatoId
                        + "/auditorias/" + auditoriaId + "/checklists/"
                        + checklistId + "/itens/" + itemId,
                tokenAuditor,
                new ItemChecklistRequestDTO(
                        1,
                        "Todos os requisitos possuem critérios de aceite?"
                ),
                200
        );
        mockMvc.perform(get(
                                "/v1/planos/{planoId}/artefatos/{artefatoId}"
                                        + "/auditorias/{auditoriaId}/checklists/"
                                        + "{checklistId}/versoes-execucao",
                                planoId,
                                artefatoId,
                                auditoriaId,
                                checklistId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAuditor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        JsonNode resposta = putJson(
                "/v1/planos/" + planoId + "/artefatos/" + artefatoId
                        + "/auditorias/" + auditoriaId + "/respostas/" + itemId,
                tokenAuditor,
                new RespostaAuditoriaRequestDTO(
                        ResultadoItem.NAO_CONFORME,
                        "O requisito não possui critério mensurável."
                ),
                200
        );
        UUID respostaId = uuid(resposta, "respostaId");

        JsonNode naoConformidade = postJson(
                "/v1/planos/" + planoId + "/nao-conformidades/rascunhos",
                tokenAuditor,
                new CriarNaoConformidadeRequestDTO(
                        respostaId,
                        null,
                        ClassificacaoNaoConformidade.COMPLEXA,
                        ""
                ),
                201
        );
        UUID naoConformidadeId = uuid(naoConformidade, "id");
        org.junit.jupiter.api.Assertions.assertEquals(
                "RASCUNHO",
                naoConformidade.path("status").asText()
        );
        org.junit.jupiter.api.Assertions.assertFalse(
                naoConformidade.path("identificadoEm").asText().isBlank()
        );

        putJson(
                "/v1/planos/" + planoId + "/nao-conformidades/"
                        + naoConformidadeId,
                tokenAuditor,
                new AtualizarNaoConformidadeRequestDTO(
                        responsavelParticipacaoId,
                        ClassificacaoNaoConformidade.COMPLEXA,
                        "Definir e revisar o critério de aceite."
                ),
                200
        );
        JsonNode versaoManual = postJson(
                "/v1/planos/" + planoId + "/artefatos/" + artefatoId
                        + "/auditorias/" + auditoriaId + "/checklists/"
                        + checklistId + "/versoes-execucao",
                tokenAuditor,
                new SalvarVersaoExecucaoRequestDTO(
                        "Ponto de controle antes do encaminhamento."
                ),
                200
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "NAO_CONFORME",
                versaoManual.path("itens").get(0).path("resultado").asText()
        );
        org.junit.jupiter.api.Assertions.assertFalse(
                versaoManual.path("itens").get(0)
                        .path("ncIdentificadaEm").asText().isBlank()
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "Definir e revisar o critério de aceite.",
                versaoManual.path("itens").get(0)
                        .path("acaoCorretiva").asText()
        );
        UUID versaoManualId = uuid(versaoManual, "id");
        UUID itemVersaoId = uuid(versaoManual.path("itens").get(0), "id");
        JsonNode itemVersaoEditado = putJson(
                "/v1/planos/" + planoId + "/artefatos/" + artefatoId
                        + "/auditorias/" + auditoriaId + "/checklists/"
                        + checklistId + "/versoes-execucao/" + versaoManualId
                        + "/itens/" + itemVersaoId,
                tokenAuditor,
                new AtualizarItemVersaoExecucaoRequestDTO(
                        "A versão salva possui critérios de aceite revisados?",
                        ResultadoItem.NAO_CONFORME,
                        responsavelParticipacaoId,
                        "Responsável pela resolução",
                        ClassificacaoNaoConformidade.COMPLEXA,
                        "Revisar os critérios registrados nesta versão."
                ),
                200
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "A versão salva possui critérios de aceite revisados?",
                itemVersaoEditado.path("descricao").asText()
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                responsavelParticipacaoId.toString(),
                itemVersaoEditado.path("responsavelParticipacaoId").asText()
        );
        org.junit.jupiter.api.Assertions.assertFalse(
                itemVersaoEditado.path("prazoResolucaoEm").asText().isBlank()
        );
        mockMvc.perform(get(
                                "/v1/planos/{planoId}/artefatos/{artefatoId}"
                                        + "/auditorias/{auditoriaId}/checklists/"
                                        + "{checklistId}/versoes-execucao",
                                planoId,
                                artefatoId,
                                auditoriaId,
                                checklistId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAuditor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itens[0].descricao").value(
                        "A versão salva possui critérios de aceite revisados?"
                ));
        mockMvc.perform(post(
                                "/v1/planos/{planoId}/nao-conformidades/{ncId}"
                                        + "/encaminhamentos",
                                planoId,
                                naoConformidadeId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAuditor))
                        .header("Idempotency-Key", "fluxo-critico-nc"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/planos/{planoId}", planoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenResponsavel)))
                .andExpect(status().isOk());

        mockMvc.perform(get(
                                "/v1/planos/{planoId}/nao-conformidades/{ncId}",
                                planoId,
                                naoConformidadeId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenResponsavel)))
                .andExpect(status().isOk());
        JsonNode resolucao = postJson(
                "/v1/planos/" + planoId + "/nao-conformidades/"
                        + naoConformidadeId + "/resolucoes",
                tokenResponsavel,
                new InformarResolucaoRequestDTO(
                        "Critério mensurável incluído e revisado.",
                        "Revisão 2 do requisito."
                ),
                201
        );
        UUID resolucaoId = uuid(resolucao, "id");

        mockMvc.perform(get("/v1/notificacoes")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(tokenAuditor)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath(
                        "$.conteudo[0].tipo"
                ).value("RESOLUCAO_INFORMADA"))
                .andExpect(jsonPath(
                        "$.conteudo[0].naoConformidadeId"
                ).value(naoConformidadeId.toString()));
        mockMvc.perform(get("/v1/notificacoes")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(tokenGestor)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath(
                        "$.conteudo[0].tipo"
                ).value("RESOLUCAO_INFORMADA"));

        JsonNode validacao = postJson(
                "/v1/planos/" + planoId + "/nao-conformidades/"
                        + naoConformidadeId + "/resolucoes/"
                        + resolucaoId + "/validacao",
                tokenAuditor,
                new ValidarResolucaoRequestDTO(
                        DecisaoValidacao.APROVAR,
                        "Correção validada."
                ),
                200
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "APROVADA",
                validacao.path("status").asText()
        );

        postSemCorpo(
                "/v1/planos/" + planoId + "/artefatos/" + artefatoId
                        + "/auditorias/" + auditoriaId + "/conclusao",
                tokenAuditor,
                200
        );

        mockMvc.perform(get(
                                "/v1/planos/{planoId}/nao-conformidades/{ncId}/historico",
                                planoId,
                                naoConformidadeId
                        )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(tokenAuditor)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(7))
                .andExpect(jsonPath(
                        "$.conteudo[0].tipo"
                ).value("NAO_CONFORMIDADE_IDENTIFICADA"))
                .andExpect(jsonPath(
                        "$.conteudo[1].tipo"
                ).value("ENCAMINHAMENTO_EQUIPE"))
                .andExpect(jsonPath(
                        "$.conteudo[2].tipo"
                ).value("NOTIFICACAO_GERADA"))
                .andExpect(jsonPath(
                        "$.conteudo[3].tipo"
                ).value("NOTIFICACAO_GERADA"))
                .andExpect(jsonPath(
                        "$.conteudo[3].titulo"
                ).value("Resolução informada"))
                .andExpect(jsonPath(
                        "$.conteudo[4].tipo"
                ).value("NOTIFICACAO_GERADA"))
                .andExpect(jsonPath(
                        "$.conteudo[4].titulo"
                ).value("Resolução informada"))
                .andExpect(jsonPath(
                        "$.conteudo[5].tipo"
                ).value("RESOLUCAO_INFORMADA"))
                .andExpect(jsonPath(
                        "$.conteudo[6].tipo"
                ).value("RESOLUCAO_APROVADA"));

        mockMvc.perform(get(
                                "/v1/planos/{planoId}/nao-conformidades/{ncId}",
                                planoId,
                                naoConformidadeId
                        )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(tokenAuditor)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    private void registrar(String nome, String email) throws Exception {
        executarJson(
                post("/v1/auth/register"),
                null,
                new UsuarioRequestDTO(nome, email, SENHA),
                201
        );
    }

    private String autenticar(String email) throws Exception {
        return executarJson(
                post("/v1/auth/login"),
                null,
                new UsuarioLoginRequestDTO(email, SENHA),
                200
        ).path("accessToken").asText();
    }

    private JsonNode postJson(
            String caminho,
            String token,
            Object corpo,
            int statusEsperado
    ) throws Exception {
        return executarJson(
                post(caminho),
                token,
                corpo,
                statusEsperado
        );
    }

    private JsonNode putJson(
            String caminho,
            String token,
            Object corpo,
            int statusEsperado
    ) throws Exception {
        return executarJson(
                put(caminho),
                token,
                corpo,
                statusEsperado
        );
    }

    private JsonNode getJson(
            String caminho,
            String token,
            int statusEsperado
    ) throws Exception {
        String json = mockMvc.perform(get(caminho)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().is(statusEsperado))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json);
    }

    private void postSemCorpo(
            String caminho,
            String token,
            int statusEsperado
    ) throws Exception {
        mockMvc.perform(post(caminho)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().is(statusEsperado));
    }

    private JsonNode executarJson(
            MockHttpServletRequestBuilder request,
            String token,
            Object corpo,
            int statusEsperado
    ) throws Exception {
        request.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(corpo));
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, bearer(token));
        }

        String json = mockMvc.perform(request)
                .andExpect(status().is(statusEsperado))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json);
    }

    private UUID uuid(JsonNode json, String campo) {
        return UUID.fromString(json.path(campo).asText());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

}
