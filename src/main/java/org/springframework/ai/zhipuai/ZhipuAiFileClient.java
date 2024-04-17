package org.springframework.ai.zhipuai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.zhipuai.api.ZhipuAiFileApi;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import java.io.IOException;

public class ZhipuAiFileClient {

    private final static Logger logger = LoggerFactory.getLogger(ZhipuAiFileClient.class);

    private final ZhipuAiFileApi zhipuAiFileApi;

    public final RetryTemplate retryTemplate;

    public ZhipuAiFileClient(ZhipuAiFileApi zhipuAiFileApi, RetryTemplate retryTemplate) {
        Assert.notNull(zhipuAiFileApi, "ZhipuAiFileApi must not be null");
        Assert.notNull(retryTemplate, "retryTemplate must not be null");
        this.zhipuAiFileApi = zhipuAiFileApi;
        this.retryTemplate = retryTemplate;
    }

    public ResponseEntity<ZhipuAiFileApi.ZhipuAiFileResponse> listFile() {
        return retryTemplate.execute(context -> {
            logger.debug("Listing files");
            return zhipuAiFileApi.listFile();
        });
    }

    public ResponseEntity<ZhipuAiFileApi.ZhipuAiFileResponse.Data> uploadFile(ZhipuAiFileApi.ZhipuAiFileRequest request) throws IOException {
        return retryTemplate.execute(context -> {
            logger.debug("Uploading file");
            return zhipuAiFileApi.uploadFile(request);
        });
    }

}
