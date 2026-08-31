package br.com.grupo5.Quality.config;

import br.com.grupo5.Quality.database.RoleEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.repository.RoleRepository;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Set;

import static br.com.grupo5.Quality.database.enums.RoleTypeEnum.*;

@Configuration
@RequiredArgsConstructor
public class AdminInitializer {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.nome}")
    private String adminNome;
    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    CommandLineRunner criarAdminInicial() {
        return args -> {
            if (usuarioRepository.existsByEmailIgnoreCase(adminEmail)) {
                return;
            }
            RoleEntity roleAdmin = roleRepository.findByNome(ROLE_ADMIN.name())
                    .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                                .nome(ROLE_ADMIN.name())
                                .build()));
            RoleEntity roleUser = roleRepository.findByNome(ROLE_USER.name())
                    .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                            .nome(ROLE_USER.name())
                            .build()));
            usuarioRepository.save(UsuarioEntity.builder()
                    .nome(adminNome)
                    .email(adminEmail)
                    .roles(Set.of(roleAdmin, roleUser))
                    .senhaHash(passwordEncoder.encode(adminPassword))
                    .criadoEm(LocalDateTime.now())
                    .build());
        };
    }

}
