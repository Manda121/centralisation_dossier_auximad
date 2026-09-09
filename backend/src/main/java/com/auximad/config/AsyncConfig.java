package com.auximad.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Active l'exécution asynchrone (@Async) utilisée par l'audit d'historique.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
