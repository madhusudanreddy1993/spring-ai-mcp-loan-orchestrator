package com.example.loanmcp.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DroolsConfig {

    private static final String RULES_PATH = "rules/loan-rules.drl";

    @Bean
    public KieContainer kieContainer() {
        KieServices ks = KieServices.Factory.get();

        // Step 1: Create a virtual file system and load your DRL
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write(ResourceFactory.newClassPathResource(RULES_PATH));

        // Step 2: Build — validates all rules at startup, fails fast on DRL errors
        KieBuilder kieBuilder = ks.newKieBuilder(kfs);
        kieBuilder.buildAll();

        // Step 3: Check for build errors — surface them clearly at startup
        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException(
                    "Drools DRL compilation errors:\n" +
                            kieBuilder.getResults().toString()
            );
        }

        // Step 4: Build the container from the compiled module
        KieModule kieModule = kieBuilder.getKieModule();
        return ks.newKieContainer(kieModule.getReleaseId());
    }
}