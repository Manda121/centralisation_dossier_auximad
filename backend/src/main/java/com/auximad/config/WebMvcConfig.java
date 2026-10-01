package com.auximad.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuditInterceptor auditInterceptor;

    @Value("${app.upload.dir:uploads/documents}")
    private String uploadDir;

    public WebMvcConfig(AuditInterceptor auditInterceptor) {
        this.auditInterceptor = auditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**");
    }

    /**
     * Sert les fichiers uploadés directement en tant que ressources statiques.
     * URL pattern : /uploads/documents/dossier-{id}/{fichier}
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Récupère le chemin parent du répertoire d'upload (ex: "uploads/documents" -> "uploads/")
        Path uploadsRoot = Paths.get(uploadDir).toAbsolutePath().getParent();
        String uploadsLocation = uploadsRoot.toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsLocation);
    }
}
