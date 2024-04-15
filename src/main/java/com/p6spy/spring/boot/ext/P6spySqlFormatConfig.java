package com.p6spy.spring.boot.ext;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 自定义 p6spy sql输出格式
 *
 * @author tianyin
 */
public class P6spySqlFormatConfig implements MessageFormattingStrategy {

    public static final String SPACE = " ";
    public static final String EMPTY = "";
    public static final String LF = "\n";
    public static final String CR = "\r";
    public static final String FULL_TIME_SPLIT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 过滤掉定时任务的 SQL
     */
    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        return StringUtils.hasText(sql) ? formatFullTime(LocalDateTime.now(), FULL_TIME_SPLIT_PATTERN)
                + " | 耗时 " + elapsed + " ms | SQL 语句：" + LF + sql.replaceAll("[\\s]+", SPACE) + ";" : "";
    }

    public static String formatFullTime(LocalDateTime localDateTime, String pattern) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        return localDateTime.format(dateTimeFormatter);
    }

}
