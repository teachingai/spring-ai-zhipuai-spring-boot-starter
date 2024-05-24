package org.springframework.ai.zhipuai.metadata;

import com.zhipu.oapi.service.v4.image.ImageResult;
import org.springframework.ai.image.ImageResponseMetadata;
import org.springframework.ai.zhipuai.api.ZhipuAiImageApi;
import org.springframework.util.Assert;

import java.util.Objects;

public class ZhipuAiImageResponseMetadata implements ImageResponseMetadata {

    private final Long created;

    public static ZhipuAiImageResponseMetadata from(ImageResult imageResult) {
        Assert.notNull(imageResult, "ImageResult must not be null");
        return new ZhipuAiImageResponseMetadata(imageResult.getCreated());
    }

    protected ZhipuAiImageResponseMetadata(Long created) {
        this.created = created;
    }

    @Override
    public Long created() {
        return this.created;
    }

    @Override
    public String toString() {
        return "ZhipuAiImageResponseMetadata{" + "created=" + created + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ZhipuAiImageResponseMetadata that))
            return false;
        return Objects.equals(created, that.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(created);
    }

}
