package br.com.grupo5.Quality.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveExporContratoComAutenticacaoJwt() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Quality API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.bearerAuth.type"
                ).value("http"))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.bearerAuth.scheme"
                ).value("bearer"))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.bearerAuth.bearerFormat"
                ).value("JWT"))
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray())
                .andExpect(jsonPath(
                        "$.paths['/v1/auth/login'].post.security"
                ).isEmpty())
                .andExpect(jsonPath(
                        "$.paths['/v1/auth/register'].post.security"
                ).isEmpty())
                .andExpect(jsonPath(
                        "$.paths['/v1/usuarios/me'].put"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/v1/usuarios/me/imagem'].put"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/v1/usuarios/me/imagem'].delete"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/v1/usuarios/me/senha'].patch"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/v1/planos/{planoId}/participantes/{participanteId}/imagem'].get"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/v1/planos/{planoId}/configuracao/feriados'].get"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/v1/planos/{planoId}/configuracao/feriados'].post"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/v1/planos/{planoId}/configuracao/feriados/{feriadoId}'].delete"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/v1/planos/{planoId}/documentos/{documentoId}'].put"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/v1/planos/{planoId}/nao-conformidades/{naoConformidadeId}/alertas-equipe'].post"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/v1/planos/{planoId}/artefatos/{artefatoId}/auditorias/{auditoriaId}/checklists/{checklistId}/versoes-execucao/{versaoId}/itens/{itemId}'].put"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/v1/planos/{planoId}/artefatos/{artefatoId}/auditorias/{auditoriaId}/checklists/{checklistId}/versoes-execucao/{versaoId}/itens/{itemId}'].delete"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/v1/planos/{planoId}/artefatos/{artefatoId}/auditorias/{auditoriaId}/checklists/{checklistId}/itens/{itemId}'].delete"
                ).exists());
    }
}
