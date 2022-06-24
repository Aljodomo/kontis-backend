package com.aljodomo.kontis.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Aljoscha Domonell
 */
@Data
@Configuration
@ConfigurationProperties("telegram")
public class TelegramProps {
    private String apiKey;
}
