package com.ruoyi.fangyuanapi.conf;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class TokenConf {

    @Value("${com.fangyuan.token.aes.accessTokenKey}")
    private String accessTokenKey;

    @Value("${com.fangyuan.token.aes.refreshTokenKey}")
    private String refreshTokenKey;


}
