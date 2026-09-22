package br.com.grupo5.Quality.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaginacaoContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "usuario@quality.test", roles = "USER")
    void devePaginarPlanosDoUsuario() throws Exception {
        mockMvc.perform(get("/v1/planos")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(5))
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0))
                .andExpect(jsonPath("$.primeira").value(true))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    @Test
    @WithMockUser(username = "admin@quality.test", roles = "ADMIN")
    void deveLimitarTamanhoDaPaginaDeUsuarios() throws Exception {
        mockMvc.perform(get("/v1/usuarios")
                        .param("page", "0")
                        .param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(100))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }
}
