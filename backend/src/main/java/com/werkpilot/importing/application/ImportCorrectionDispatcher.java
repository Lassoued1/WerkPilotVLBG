package com.werkpilot.importing.application;

import com.werkpilot.importing.application.port.ImportJobRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportCorrectionDispatcher {

    private final ProductionCsvImportService productionCsvImportService;
    private final EnergyCsvImportService energyCsvImportService;
    private final DowntimeCsvImportService downtimeCsvImportService;
    private final ScrapCsvImportService scrapCsvImportService;

    public ImportCorrectionDispatcher(
            ProductionCsvImportService productionCsvImportService,
            EnergyCsvImportService energyCsvImportService,
            DowntimeCsvImportService downtimeCsvImportService,
            ScrapCsvImportService scrapCsvImportService) {
        this.productionCsvImportService = productionCsvImportService;
        this.energyCsvImportService = energyCsvImportService;
        this.downtimeCsvImportService = downtimeCsvImportService;
        this.scrapCsvImportService = scrapCsvImportService;
    }

    public void process(ImportJobRecord job, MultipartFile file) {
        switch (job.importType()) {
            case PRODUCTION_RECORDS -> productionCsvImportService.process(job, file);
            case ENERGY_MEASUREMENTS -> energyCsvImportService.process(job, file);
            case DOWNTIME_RECORDS -> downtimeCsvImportService.process(job, file);
            case SCRAP_RECORDS -> scrapCsvImportService.process(job, file);
        }
    }
}
