package br.com.grupo5.Quality.handler;

import br.com.grupo5.Quality.config.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProblemDetailContractTest {

    private static final String CORRELATION_ID = "contrato-http-123";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devePadronizarRespostaNaoAutenticada() throws Exception {
        mockMvc.perform(get("/v1/planos")
                        .header(CorrelationIdFilter.HEADER, CORRELATION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(CorrelationIdFilter.HEADER, CORRELATION_ID))
                .andExpect(jsonPath("$.type").value("urn:quality:problema:nao-autenticado"))
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "USER")
    void devePadronizarRespostaDeAcessoNegado() throws Exception {
        mockMvc.perform(get("/v1/usuarios")
                        .header(CorrelationIdFilter.HEADER, CORRELATION_ID))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(CorrelationIdFilter.HEADER, CORRELATION_ID))
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
    }

    @Test
    void deveInformarCamposInvalidos() throws Exception {
        String jsonInvalido = """
                {
                  "nome": "",
                  "email": "email-invalido",
                  "senha": "123"
                }
                """;

        mockMvc.perform(post("/v1/auth/register")
                        .header(CorrelationIdFilter.HEADER, CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    void naoDevePropagarCorrelationIdInvalido() throws Exception {
        String valorInvalido = "<script>alert(1)</script>";

        mockMvc.perform(get("/v1/planos")
                        .header(CorrelationIdFilter.HEADER, valorInvalido))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        CorrelationIdFilter.HEADER,
                        not(is(valorInvalido))
                ))
                .andExpect(jsonPath("$.correlationId", not(is(valorInvalido))));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deveTratarEndpointRestInexistenteSemExporMensagemDeRecursoEstatico()
            throws Exception {
        mockMvc.perform(get("/v1/endpoint-inexistente")
                        .header(CorrelationIdFilter.HEADER, CORRELATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title").value("Endpoint não encontrado"))
                .andExpect(jsonPath("$.codigo").value(
                        "ENDPOINT_NAO_ENCONTRADO"
                ))
                .andExpect(jsonPath("$.detail").value(
                        "A operação solicitada não está disponível nesta versão da API."
                ));
    }
}
