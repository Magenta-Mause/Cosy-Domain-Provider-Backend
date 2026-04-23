package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.action.SubdomainCreationDto;
import com.magentamause.cosydomainprovider.model.action.SubdomainUpdateDto;
import com.magentamause.cosydomainprovider.model.core.LabelAvailabilityDto;
import com.magentamause.cosydomainprovider.model.core.SubdomainDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/v1/subdomain")
public interface SubdomainApi {

    @GetMapping
    ResponseEntity<List<SubdomainDto>> listMySubdomains();

    @GetMapping("/check")
    ResponseEntity<LabelAvailabilityDto> checkLabelAvailability(@RequestParam String label);

    @GetMapping("/{uuid}")
    ResponseEntity<SubdomainDto> getSubdomain(@PathVariable String uuid);

    @PostMapping
    ResponseEntity<SubdomainDto> createSubdomain(
            @Valid @RequestBody SubdomainCreationDto creationDto);

    @PutMapping("/{uuid}")
    ResponseEntity<SubdomainDto> updateSubdomain(
            @PathVariable String uuid, @Valid @RequestBody SubdomainUpdateDto updateDto);

    @DeleteMapping("/{uuid}")
    ResponseEntity<Void> deleteSubdomain(@PathVariable String uuid);
}
