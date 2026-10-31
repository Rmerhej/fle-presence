package fr.asso.fle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfig {

    /** Dates affichées en français partout (« mardi 4 novembre »). */
    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(Locale.FRANCE);
    }
}
