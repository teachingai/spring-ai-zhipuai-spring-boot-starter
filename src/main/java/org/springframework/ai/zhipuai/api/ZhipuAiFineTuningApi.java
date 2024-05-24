package org.springframework.ai.zhipuai.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.zhipuai.util.ApiUtils;
import org.springframework.http.*;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public class ZhipuAiFineTuningApi {

    private final RestClient restClient;

    /**
     * Create a new ZhipuAI Fine-Tuning api with base URL set to https://open.bigmodel.cn/api/paas
     * @param apiKey ZhipuAI apiKey.
     */
    public ZhipuAiFineTuningApi(String apiKey) {
        this(ApiUtils.DEFAULT_BASE_URL, apiKey, RestClient.builder());
    }

    /**
     * Create a new ZhipuAI Fine-Tuning API with the provided base URL.
     * @param baseUrl the base URL for the ZhipuAI API.
     * @param apiKey ZhipuAI apiKey.
     * @param restClientBuilder the rest client builder to use.
     */
    public ZhipuAiFineTuningApi(String baseUrl, String apiKey, RestClient.Builder restClientBuilder) {
        this(baseUrl, apiKey, restClientBuilder, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
    }

    /**
     * Create a new ZhipuAI Fine-Tuning API with the provided base URL.
     * @param baseUrl the base URL for the ZhipuAI API.
     * @param apiKey ZhipuAI apiKey.
     * @param restClientBuilder the rest client builder to use.
     * @param responseErrorHandler the response error handler to use.
     */
    public ZhipuAiFineTuningApi(String baseUrl, String apiKey, RestClient.Builder restClientBuilder,
                                ResponseErrorHandler responseErrorHandler) {

        this.restClient = restClientBuilder.baseUrl(baseUrl)
                .defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey))
                .defaultStatusHandler(responseErrorHandler)
                .build();
    }

    // @formatter:off
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ZhipuAiFileRequest (
            @JsonProperty("file") MultipartFile file,

            @JsonProperty("purpose") String purpose) {

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ZhipuAiFileResponse(
            @JsonProperty("object") String object,
            @JsonProperty("data") List<Data> data,
            @JsonProperty("error") Error error) {

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Data(
                @JsonProperty("id") String id,
                @JsonProperty("filename") String filename,
                @JsonProperty("purpose") String purpose,
                @JsonProperty("code") Integer code,
                @JsonProperty("msg") String msg) {
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Error(
            @JsonProperty("code") String code,
            @JsonProperty("message") String message) {
    }
    // @formatter:onn

    /**
     * 列举出用户已上传的所有文件
     * @return
     */
    public ResponseEntity<ZhipuAiFileResponse> listFile() {
        return this.restClient.get()
                .uri("/api/paas/v4/files")
                .retrieve()
                .toEntity(ZhipuAiFileResponse.class);
    }

    /**
     * 上传文件
     * 注意，单个用户最多只能上传 1000 个文件，单文件不超过 100MB，同时所有已上传的文件总和不超过 10G 容量。如果您要抽取更多文件，需要先删除一部分不再需要的文件。
     * @param request
     * @return
     */
    public ResponseEntity<ZhipuAiFileResponse.Data> uploadFile(ZhipuAiFileRequest request) throws IOException {

        Assert.notNull(request.file(), "File cannot be null.");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        ContentDisposition contentDisposition = ContentDisposition
                .builder("form-data")
                .name("file")
                .filename(request.file().getOriginalFilename())
                .build();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

        HttpEntity<byte[]> fileEntity = new HttpEntity<>(request.file().getBytes(), fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileEntity);
        body.add("purpose", request.purpose());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        return this.restClient.post()
                .uri("/api/paas/v4/files")
                .body(requestEntity)
                .retrieve()
                .toEntity(ZhipuAiFileResponse.Data.class);
    }

}
