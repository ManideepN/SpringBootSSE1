package com.example;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootApplication
@RestController
public class SpringBootSseApplication {

	private final Map<String,SseEmitter> sses=new ConcurrentHashMap<>(); 
	
	@Bean
	IntegrationFlow inboundFlow(@Value("${input-dir:file:///C:/Users/MA342559/Desktop/sse}") File in) {
		
		return IntegrationFlows.from(Files.inboundAdapter(in).autoCreateDirectory(true),
				poller-> poller.poller(spec->spec.fixedRate(1000L)))
				.transform(File.class,File::getAbsolutePath)
				.handle(String.class,(GenericHandler<String>) (path, map) -> {
					sses.forEach((name,sse)->{
						try {
							sse.send(path);
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
					return null;
				})
				.get();
	}
	
	@GetMapping(value="/files/{name}")
	SseEmitter emitter(@PathVariable String name) {
		SseEmitter sseEmitter=new SseEmitter(/*60*1_000L*/);
		sses.put(name, sseEmitter);
		return sseEmitter;
	}
	
	public static void main(String[] args) {
		SpringApplication.run(SpringBootSseApplication.class, args);
	}
}
