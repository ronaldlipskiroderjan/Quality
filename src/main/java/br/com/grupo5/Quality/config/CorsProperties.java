package br.com.grupo5.Quality.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> origensPermitidas = List.of();

    public List<String> getOrigensPermitidas() {
        return origensPermitidas;
    }

    public void setOrigensPermitidas(List<String> origensPermitidas) {
        this.origensPermitidas = origensPermitidas == null
                ? List.of()
                : List.copyOf(origensPermitidas);
    }
}
