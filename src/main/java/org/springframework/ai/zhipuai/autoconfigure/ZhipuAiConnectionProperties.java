package org.springframework.ai.zhipuai.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

@ConfigurationProperties(ZhipuAiConnectionProperties.CONFIG_PREFIX)
public class ZhipuAiConnectionProperties extends ZhipuAiParentProperties {

    public static final String CONFIG_PREFIX = "spring.ai.zhipuai";
    /**
     * 请求超时时间
     */
    private int requestTimeOut;
    /**
     * 请求超时时间单位
     */
    private TimeUnit timeOutTimeUnit;

    public int getRequestTimeOut() {
        return requestTimeOut;
    }

    public void setRequestTimeOut(int requestTimeOut) {
        this.requestTimeOut = requestTimeOut;
    }

    public TimeUnit getTimeOutTimeUnit() {
        return timeOutTimeUnit;
    }

    public void setTimeOutTimeUnit(TimeUnit timeOutTimeUnit) {
        this.timeOutTimeUnit = timeOutTimeUnit;
    }
}
