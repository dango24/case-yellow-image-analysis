package com.icarusrises.caseyellowimageanalysis.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/image-analysis")
public class ImageAnalysisController {

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/health")
    public Payload health() {
        return new Payload("Show me how to live");
    }
}
