package org.springframework.ai.zhipuai;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.StreamingChatClient;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.function.AbstractFunctionCallSupport;
import org.springframework.ai.model.function.FunctionCallbackContext;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.zhipuai.api.ZhipuAiChatOptions;
import org.springframework.ai.zhipuai.util.ApiUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZhipuAiChatClient
        extends AbstractFunctionCallSupport<ChatMessage, ChatCompletionRequest, ResponseEntity<ModelApiResponse>>
        implements ChatClient, StreamingChatClient {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static String requestIdTemplate = "zhipu-ai-chat-%s";
    /**
     * Default options to be used for all chat requests.
     */
    private ZhipuAiChatOptions defaultOptions;
    /**
     * 智普 SDK library.
     */
    private final ClientV4 zhipuClient;
    private final RetryTemplate retryTemplate;
    private static final ObjectMapper OBJECT_MAPPER = defaultObjectMapper();

    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return mapper;
    }

    public ZhipuAiChatClient(ClientV4 zhipuClient) {
        this(zhipuClient, ZhipuAiChatOptions.builder()
                        .withModel(Constants.ModelChatGLM3TURBO)
                        .withMaxToken(ApiUtils.DEFAULT_MAX_TOKENS)
                        .withDoSample(Boolean.TRUE)
                        .withTemperature(ApiUtils.DEFAULT_TEMPERATURE)
                        .withTopP(ApiUtils.DEFAULT_TOP_P)
                        .build());
    }

    public ZhipuAiChatClient(ClientV4 zhipuClient, ZhipuAiChatOptions options) {
        this(zhipuClient, options, null, RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    public ZhipuAiChatClient(ClientV4 zhipuClient, ZhipuAiChatOptions options,
                               FunctionCallbackContext functionCallbackContext, RetryTemplate retryTemplate) {
        super(functionCallbackContext);
        Assert.notNull(zhipuClient, "ClientV4 must not be null");
        Assert.notNull(options, "Options must not be null");
        Assert.notNull(retryTemplate, "RetryTemplate must not be null");
        this.zhipuClient = zhipuClient;
        this.defaultOptions = options;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        // 1、创建请求
        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
        var request = this.createRequest(prompt, requestId, false, Constants.invokeMethodAsync);
        // 2、执行请求
        return retryTemplate.execute(ctx -> {
            // 3、调用智能聊天接口
            ResponseEntity<ModelApiResponse> completionEntity = this.callWithFunctionSupport(request);

            var chatCompletion = completionEntity.getBody();
            if (chatCompletion == null) {
                log.warn("No chat completion returned for prompt: {}", prompt);
                return new ChatResponse(List.of());
            }

            List<Generation> generations = chatCompletion.getData().getChoices()
                    .stream()
                    .map(choice -> new Generation(Objects.toString(choice.getMessage().getContent()), toMap(chatCompletion.getData().getId(), choice))
                            .withGenerationMetadata(ChatGenerationMetadata.from(choice.getFinishReason(), null)))
                    .toList();

            return new ChatResponse(generations);
        });
    }

    private Map<String, Object> toMap(String id, Choice choice) {
        Map<String, Object> map = new HashMap<>();

        var message = choice.getMessage();
        if (message.getRole() != null) {
            map.put("role", message.getRole());
        }
        if (choice.getFinishReason() != null) {
            map.put("finishReason", choice.getFinishReason());
        }
        map.put("id", id);
        return map;
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        // 1、创建请求
        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
        var request = this.createRequest(prompt, requestId, true, Constants.invokeMethod);
        // 2、执行请求
        return retryTemplate.execute(ctx -> Flux.create(sink -> {
            // 3、调用智能聊天接口
            var sseModelApiResp = this.zhipuClient.invokeModelApi(request);
            if (sseModelApiResp.isSuccess()) {


                AtomicBoolean isFirst = new AtomicBoolean(true);
                ChatMessageAccumulator chatMessageAccumulator = this.mapStreamToAccumulator(sseModelApiResp.getFlowable())
                        .doOnNext(accumulator -> {
                            {
                                if (isFirst.getAndSet(false)) {
                                    System.out.print("Response: ");
                                }
                                if (accumulator.getDelta() != null && accumulator.getDelta().getTool_calls() != null) {
                                    String jsonString = OBJECT_MAPPER.writeValueAsString(accumulator.getDelta().getTool_calls());
                                    System.out.println("tool_calls: " + jsonString);
                                }
                                if (accumulator.getDelta() != null && accumulator.getDelta().getContent() != null) {
                                    System.out.print(accumulator.getDelta().getContent());
                                }
                                sink.next();
                            }
                        })
                        .doOnComplete(() -> sink.complete())
                        .lastElement()
                        .blockingGet();

                Choice choice = new Choice(chatMessageAccumulator.getChoice().getFinishReason(), 0L, chatMessageAccumulator.getDelta());
                List<Choice> choices = new ArrayList<>();
                choices.add(choice);
                ModelData data = new ModelData();
                data.setChoices(choices);
                data.setUsage(chatMessageAccumulator.getUsage());
                data.setId(chatMessageAccumulator.getId());
                data.setCreated(chatMessageAccumulator.getCreated());
                data.setRequestId(request.getRequestId());
                sseModelApiResp.setFlowable(null);
                sseModelApiResp.setData(data);



            }
            System.out.println("model output:" + JSON.toJSONString(sseModelApiResp));

            // For chunked responses, only the first chunk contains the choice role.
            // The rest of the chunks with same ID share the same role.
            ConcurrentHashMap<String, String> roleMap = new ConcurrentHashMap<>();

            return completionChunks.getFlowable().map(chunk -> toChatCompletion(chunk)).map(chatCompletion -> {

                chatCompletion = handleFunctionCallOrReturn(request, ResponseEntity.of(Optional.of(chatCompletion)))
                        .getBody();

                @SuppressWarnings("null")
                String id = chatCompletion.id();

                List<Generation> generations = chatCompletion.choices().stream().map(choice -> {
                    if (choice.message().role() != null) {
                        roleMap.putIfAbsent(id, choice.message().role().name());
                    }
                    String finish = (choice.finishReason() != null ? choice.finishReason().name() : "");
                    var generation = new Generation(choice.message().content(),
                            Map.of("id", id, "role", roleMap.get(id), "finishReason", finish));
                    if (choice.finishReason() != null) {
                        generation = generation
                                .withGenerationMetadata(ChatGenerationMetadata.from(choice.finishReason().name(), null));
                    }
                    return generation;
                }).toList();
                return new ChatResponse(generations);
            });
        }));
    }

    private Flowable<ChatMessageAccumulator> mapStreamToAccumulator(Flowable<ModelData> flowable) {
        return flowable.map(chunk -> new ChatMessageAccumulator(chunk.getChoices().get(0).getDelta(), null, chunk.getChoices().get(0), chunk.getUsage(), chunk.getCreated(), chunk.getId()));
    }

    private ChatMessageAccumulator toChatCompletion(ModelData chunk) {
        List<ModelApiResponse.Choice> choices = chunk.getChoices()
                .stream()
                .map(cc -> new ModelApiResponse.Choice(cc.index(), cc.delta(), cc.finishReason()))
                .toList();

        return new ModelApiResponse(chunk.id(), "chat.completion", chunk.created(), chunk.model(), choices, chunk.requestId(),null);
    }

    /**
     * Accessible for testing.
     */
    ChatCompletionRequest createRequest(Prompt prompt, String requestId, boolean stream, String invokeMethod) {

        Set<String> functionsForThisRequest = new HashSet<>();

        var chatCompletionMessages = prompt.getInstructions()
                .stream()
                .map(m -> new ChatMessage(m.getMessageType().name(), m.getContent()))
                .toList();

        var chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM3TURBO)
                .stream(stream)
                .invokeMethod(invokeMethod)
                .messages(chatCompletionMessages)
                .requestId(requestId)
                .toolChoice("auto")
                .build();

        // Add the default enabled functions to the request's tools parameter.
        if (this.defaultOptions != null) {
            Set<String> defaultEnabledFunctions = super.handleFunctionCallbackConfigurations(this.defaultOptions, !IS_RUNTIME_CALL);
            functionsForThisRequest.addAll(defaultEnabledFunctions);
            chatCompletionRequest = ModelOptionsUtils.merge(chatCompletionRequest, this.defaultOptions, ChatCompletionRequest.class);
        }
        // Add the prompt enabled functions to the request's tools parameter.
        if (prompt.getOptions() != null) {
            if (prompt.getOptions() instanceof ChatOptions runtimeOptions) {

                var updatedRuntimeOptions = ModelOptionsUtils.copyToTarget(runtimeOptions, ChatOptions.class, ZhipuAiChatOptions.class);
                Set<String> promptEnabledFunctions = this.handleFunctionCallbackConfigurations(updatedRuntimeOptions, IS_RUNTIME_CALL);
                functionsForThisRequest.addAll(promptEnabledFunctions);
                chatCompletionRequest = ModelOptionsUtils.merge(updatedRuntimeOptions, chatCompletionRequest, ChatCompletionRequest.class);
            } else {
                throw new IllegalArgumentException("Prompt options are not of type ChatOptions: " + prompt.getOptions().getClass().getSimpleName());
            }
        }
        // Add the enabled functions definitions to the request's tools parameter.
        if (!CollectionUtils.isEmpty(functionsForThisRequest)) {
            chatCompletionRequest = ModelOptionsUtils.merge(
                    ZhipuAiChatOptions.builder().withTools(this.getFunctionTools(functionsForThisRequest)).build(),
                    chatCompletionRequest, ChatCompletionRequest.class);
        }

        return chatCompletionRequest;
    }

    private List<ChatTool> getFunctionTools(Set<String> functionNames) {
        return this.resolveFunctionCallbacks(functionNames).stream().map(functionCallback -> {

            ChatTool chatTool = new ChatTool();
            chatTool.setType(ChatToolType.FUNCTION.value());
            ChatFunctionParameters chatFunctionParameters = OBJECT_MAPPER.convertValue(functionCallback.getInputTypeSchema(), ChatFunctionParameters.class);
            ChatFunction chatFunction = ChatFunction.builder()
                    .name(functionCallback.getName())
                    .description(functionCallback.getDescription())
                    .parameters(chatFunctionParameters)
                    .build();
            chatTool.setFunction(chatFunction);
/*
            chatTool1.setType(ChatToolType.WEB_SEARCH.value());
            WebSearch webSearch = new WebSearch();
            webSearch.setSearch_query("清华的升学率");
            webSearch.setEnable(false);
            chatTool1.setWeb_search(webSearch);*/

            return chatTool;
        }).toList();
    }

    //
    // Function Calling Support
    //
    @Override
    protected ChatCompletionRequest doCreateToolResponseRequest(ChatCompletionRequest previousRequest,
                                                                           ChatMessage responseMessage,
                                                                           List<ChatMessage> conversationHistory) {

        // Every tool-call item requires a separate function call and a response (TOOL)
        // message.
        for (ToolCalls toolCall : responseMessage.getTool_calls()) {

            var functionName = toolCall.getFunction().getName();
            String functionArguments = toolCall.getFunction().getArguments().asText();

            if (!this.functionCallbackRegister.containsKey(functionName)) {
                throw new IllegalStateException("No function callback found for function name: " + functionName);
            }

            String functionResponse = this.functionCallbackRegister.get(functionName).call(functionArguments);

            // Add the function response to the conversation.
            conversationHistory.add(new ChatMessage(ChatMessageRole.FUNCTION.value(), functionResponse, functionName, toolCall.getId()));

        }

        // Recursively call chatCompletionWithTools until the model doesn't call a
        // functions anymore.
        ChatCompletionRequest newRequest = new ChatCompletionRequest(previousRequest.getRequestId(), conversationHistory, false);
        newRequest = ModelOptionsUtils.merge(newRequest, previousRequest, ChatCompletionRequest.class);

        return newRequest;
    }

    @Override
    protected List<ChatMessage> doGetUserMessages(ChatCompletionRequest request) {
        return request.getMessages();
    }

    @SuppressWarnings("null")
    @Override
    protected ChatMessage doGetToolResponseMessage(ResponseEntity<ModelApiResponse> chatCompletion) {
        return chatCompletion.getBody().getData().getChoices().iterator().next().getMessage();
    }

    @Override
    protected ResponseEntity<ModelApiResponse> doChatCompletion(ChatCompletionRequest request) {
        ModelApiResponse invokeModelApiResp = zhipuClient.invokeModelApi(request);
        return ResponseEntity.ofNullable(invokeModelApiResp);
    }

    @Override
    protected boolean isToolFunctionCall(ResponseEntity<ModelApiResponse> chatCompletion) {

        var body = chatCompletion.getBody();
        if (body == null) {
            return false;
        }

        var choices = body.getData().getChoices();
        if (CollectionUtils.isEmpty(choices)) {
            return false;
        }

        return !CollectionUtils.isEmpty(choices.get(0).getMessage().getTool_calls());
    }
}
