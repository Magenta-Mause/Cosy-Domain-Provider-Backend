package com.magentamause.cosydomainprovider.controller;

import com.magentamause.cosydomainprovider.model.dns.DnsEntry;
import com.magentamause.cosydomainprovider.services.aws.Route53Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/route")
public class RouteController {

    private final Route53Service route53Service;

    @GetMapping
    public ResponseEntity<List<DnsEntry>> getRoutes() {
        return ResponseEntity.ok(route53Service.listAllRecords());
    }
}
