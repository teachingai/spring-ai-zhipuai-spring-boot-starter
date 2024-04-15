package com.p6spy.spring.boot;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.p6spy.engine.common.P6LogQuery;
import com.p6spy.engine.spy.DefaultJdbcEventListenerFactory;
import com.p6spy.engine.spy.JdbcEventListenerFactory;
import com.p6spy.engine.spy.P6DataSource;
import com.p6spy.engine.spy.P6ModuleManager;
import com.p6spy.engine.spy.option.P6OptionChangedListener;
import com.p6spy.spring.boot.ext.P6spyDataSource;

/**
 */
@Configuration
@ConditionalOnClass(com.p6spy.engine.spy.P6DataSource.class)
@EnableConfigurationProperties({ P6SpyProperties.class, DataSourceProperties.class})
public class P6SpyAutoConfiguration {
	
	@Bean
	@ConditionalOnMissingBean
	protected P6OptionChangedListener p6OptionChangedListener() {
		return new P6LogQuery();
	}
	
	@Bean
	@ConditionalOnMissingBean
	protected JdbcEventListenerFactory jdbcEventListenerFactory() {
		return new DefaultJdbcEventListenerFactory();
	}
	
	@Primary
	public P6DataSource druidDataSource(@P6spyDataSource ObjectProvider<DataSource> p6spyDataSource,
			ObjectProvider<P6OptionChangedListener> p6OptionChangedListener,
			JdbcEventListenerFactory jdbcEventListenerFactory) {
		
		p6OptionChangedListener.stream().forEach(listener -> {
			P6ModuleManager.getInstance().registerOptionChangedListener(listener);
		});
		
		P6DataSource p6DataSource = new P6DataSource(p6spyDataSource.getObject());
		p6DataSource.setJdbcEventListenerFactory(jdbcEventListenerFactory);
		
		return p6DataSource;
	}

}
