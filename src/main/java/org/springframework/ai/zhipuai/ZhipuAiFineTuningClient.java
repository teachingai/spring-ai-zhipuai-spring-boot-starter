package org.springframework.ai.zhipuai;

import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.service.v4.file.FileApiResponse;
import com.zhipu.oapi.service.v4.fine_turning.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import java.util.Objects;

public class ZhipuAiFineTuningClient {

    private final static Logger logger = LoggerFactory.getLogger(ZhipuAiFineTuningClient.class);

    private final ClientV4 zhipuClient;

    public final RetryTemplate retryTemplate;

    public ZhipuAiFineTuningClient(ClientV4 zhipuClient, RetryTemplate retryTemplate) {
        Assert.notNull(zhipuClient, "ClientV4 must not be null");
        Assert.notNull(retryTemplate, "retryTemplate must not be null");
        this.zhipuClient = zhipuClient;
        this.retryTemplate = retryTemplate;
    }

    /**
     * 微调V4-上传数据集
     * @param purpose
     * @param path
     * @return
     */
    public ResponseEntity<FileApiResponse> uploadFile( String purpose, String path) {
        return retryTemplate.execute(context -> {
            logger.debug("Uploading file");
            return ResponseEntity.ofNullable(zhipuClient.invokeUploadFileApi(purpose, path));
        });
    }

    /**
     * 微调V4-创建微调任务
     * @param request
     * @return
     */
    public ResponseEntity<FineTuningJob> createFineTuningJob(FineTuningJobRequest request) {
        return retryTemplate.execute(context -> {
            logger.debug("Listing files");
            CreateFineTuningJobApiResponse response = zhipuClient.createFineTuningJob(request);
            if (Objects.nonNull(response)) {
                return ResponseEntity.internalServerError().build();
            }
            if (response.isSuccess()) {
                return ResponseEntity.ofNullable(response.getData());
            }
            return new ResponseEntity<>(response.getData(), HttpStatus.INTERNAL_SERVER_ERROR);
        });
    }

    /**
     * 微调V4-查询微调任务
     * @param request
     * @return
     */
    public ResponseEntity<FineTuningJob> retrieveFineTuningJobs(QueryFineTuningJobRequest request) {
        return retryTemplate.execute(context -> {
            logger.debug("Listing files");
            QueryFineTuningJobApiResponse response = zhipuClient.retrieveFineTuningJobs(request);
            if (Objects.nonNull(response)) {
                return ResponseEntity.internalServerError().build();
            }
            if (response.isSuccess()) {
                return ResponseEntity.ofNullable(response.getData());
            }
            return new ResponseEntity<>(response.getData(), HttpStatus.INTERNAL_SERVER_ERROR);
        });
    }

    /**
     * 微调V4-查询微调任务状态
     * @param request
     * @return
     */
    public ResponseEntity<FineTuningEvent> queryFineTuningJobsEvents(QueryFineTuningJobRequest request) {
        return retryTemplate.execute(context -> {
            logger.debug("Listing files");
            QueryFineTuningEventApiResponse response = zhipuClient.queryFineTuningJobsEvents(request);
            if (Objects.nonNull(response)) {
                return ResponseEntity.internalServerError().build();
            }
            if (response.isSuccess()) {
                return ResponseEntity.ofNullable(response.getData());
            }
            return new ResponseEntity<>(response.getData(), HttpStatus.INTERNAL_SERVER_ERROR);
        });
    }

    /**
     * 微调V4-查询个人微调任务
     * @param request
     * @return
     */
    public ResponseEntity<PersonalFineTuningJob> queryPersonalFineTuningJobs(QueryPersonalFineTuningJobRequest request) {
        return retryTemplate.execute(context -> {
            logger.debug("QueryPersonalFineTuningJob: Retry {} Times ", context.getRetryCount());
            QueryPersonalFineTuningJobApiResponse response = zhipuClient.queryPersonalFineTuningJobs(request);
            if (Objects.nonNull(response)) {
                return ResponseEntity.internalServerError().build();
            }
            if (response.isSuccess()) {
                return ResponseEntity.ofNullable(response.getData());
            }
            return new ResponseEntity<>(response.getData(), HttpStatus.INTERNAL_SERVER_ERROR);
        });
    }

}
