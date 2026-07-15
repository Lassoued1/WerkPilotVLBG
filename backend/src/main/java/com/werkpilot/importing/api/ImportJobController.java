package com.werkpilot.importing.api;

import com.werkpilot.importing.application.DowntimeCsvImportService;
import com.werkpilot.importing.application.EnergyCsvImportService;
import com.werkpilot.importing.application.ImportJobService;
import com.werkpilot.importing.application.ProductionCsvImportService;
import com.werkpilot.importing.application.ScrapCsvImportService;
import com.werkpilot.importing.application.port.ImportJobRecord;
import com.werkpilot.shared.api.JobResponse;
import com.werkpilot.shared.api.JobStatus;
import com.werkpilot.shared.api.PageResponse;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/import-jobs")
public class ImportJobController {

    private final ImportJobService importJobService;
    private final ProductionCsvImportService productionCsvImportService;
    private final EnergyCsvImportService energyCsvImportService;
    private final DowntimeCsvImportService downtimeCsvImportService;
    private final ScrapCsvImportService scrapCsvImportService;

    public ImportJobController(
            ImportJobService importJobService,
            ProductionCsvImportService productionCsvImportService,
            EnergyCsvImportService energyCsvImportService,
            DowntimeCsvImportService downtimeCsvImportService,
            ScrapCsvImportService scrapCsvImportService) {
        this.importJobService = importJobService;
        this.productionCsvImportService = productionCsvImportService;
        this.energyCsvImportService = energyCsvImportService;
        this.downtimeCsvImportService = downtimeCsvImportService;
        this.scrapCsvImportService = scrapCsvImportService;
    }

    @PostMapping(path = "/production-records", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    JobResponse startProductionImport(Authentication authentication, @RequestPart("file") MultipartFile file) {
        ImportJobRecord job = importJobService.startProductionImportJob(file, principal(authentication));
        productionCsvImportService.process(job, file);
        return new JobResponse(job.id(), JobStatus.PROCESSING, job.createdAt(), job.completedAt());
    }

    @PostMapping(path = "/energy-measurements", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    JobResponse startEnergyImport(Authentication authentication, @RequestPart("file") MultipartFile file) {
        ImportJobRecord job = importJobService.startEnergyImportJob(file, principal(authentication));
        energyCsvImportService.process(job, file);
        return new JobResponse(job.id(), JobStatus.PROCESSING, job.createdAt(), job.completedAt());
    }

    @PostMapping(path = "/downtime-records", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    JobResponse startDowntimeImport(Authentication authentication, @RequestPart("file") MultipartFile file) {
        ImportJobRecord job = importJobService.startDowntimeImportJob(file, principal(authentication));
        downtimeCsvImportService.process(job, file);
        return new JobResponse(job.id(), JobStatus.PROCESSING, job.createdAt(), job.completedAt());
    }

    @PostMapping(path = "/scrap-records", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    JobResponse startScrapImport(Authentication authentication, @RequestPart("file") MultipartFile file) {
        ImportJobRecord job = importJobService.startScrapImportJob(file, principal(authentication));
        scrapCsvImportService.process(job, file);
        return new JobResponse(job.id(), JobStatus.PROCESSING, job.createdAt(), job.completedAt());
    }

    @GetMapping
    PageResponse<ImportJobService.ImportJobListItemResponse> listJobs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return page(importJobService.listJobs(page, size));
    }

    @GetMapping("/{id}/errors")
    PageResponse<ImportJobService.ImportJobErrorResponse> listErrors(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return page(importJobService.listErrors(id, page, size));
    }

    private static <T> PageResponse<T> page(ImportJobService.ImportJobPage<T> page) {
        return new PageResponse<>(page.items(), page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    private static AuthenticatedPrincipal principal(Authentication authentication) {
        return (AuthenticatedPrincipal) authentication.getPrincipal();
    }
}
