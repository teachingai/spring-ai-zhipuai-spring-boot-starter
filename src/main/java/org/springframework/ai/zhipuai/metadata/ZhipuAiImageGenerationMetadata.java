package org.springframework.ai.zhipuai.metadata;

import org.springframework.ai.image.ImageGenerationMetadata;

import java.util.Objects;

public class ZhipuAiImageGenerationMetadata implements ImageGenerationMetadata {

    private String revisedPrompt;

    public ZhipuAiImageGenerationMetadata(String revisedPrompt) {
        this.revisedPrompt = revisedPrompt;
    }

    public String getRevisedPrompt() {
        return revisedPrompt;
    }

    @Override
    public String toString() {
        return "ZhipuAiImageGenerationMetadata{" + "revisedPrompt='" + revisedPrompt + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ZhipuAiImageGenerationMetadata that)) {
            return false;
        }
        return Objects.equals(revisedPrompt, that.revisedPrompt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revisedPrompt);
    }

}
