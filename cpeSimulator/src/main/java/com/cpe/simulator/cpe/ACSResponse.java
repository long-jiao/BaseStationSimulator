package com.cpe.simulator.cpe;

import lombok.Data;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;

@Data
public class ACSResponse {
	
	private String response;
	private List<String> cookies = new ArrayList<>();
	private HttpHeaders headers;

}
