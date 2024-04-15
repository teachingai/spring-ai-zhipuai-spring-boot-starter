# spring-ai-zhipuai-spring-boot-starter

Spring Boot Starter For Spring AI Implementation Base On ZhiPu AI（ChatGLM）

### 说明


 > 基于 智普AI 的 ChatGLM 模型 和 Spring AI 的 Spring Boot Starter 实现

### Maven

``` xml
<dependency>
	<groupId>com.github.hiwepy</groupId>
	<artifactId>spring-ai-zhipuai-spring-boot-starter</artifactId>
	<version>${project.version}</version>
</dependency>
```

### Sample

```java

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
	}

}

```

