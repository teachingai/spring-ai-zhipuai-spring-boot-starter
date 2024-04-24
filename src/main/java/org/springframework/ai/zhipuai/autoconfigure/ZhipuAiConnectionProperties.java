package org.springframework.ai.zhipuai.autoconfigure;

import org.springframework.ai.zhipuai.util.ApiUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(ZhipuAiConnectionProperties.CONFIG_PREFIX)
public class ZhipuAiConnectionProperties extends ZhipuAiParentProperties {

    public static final String CONFIG_PREFIX = "spring.ai.zhipuai";

}
