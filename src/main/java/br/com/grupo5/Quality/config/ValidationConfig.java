package br.com.grupo5.Quality.config;

import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForCalendar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfig {

    @Bean
    public PastOrPresentValidatorForCalendar pastOrPresentValidatorForCalendar() {
        return new PastOrPresentValidatorForCalendar();
    }
}
