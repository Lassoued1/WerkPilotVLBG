package com.werkpilot.importing.application.port;

import com.werkpilot.importing.domain.ImportType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ImportJobPort {

    boolean existsNormalImportByTypeAndHash(ImportType importType, String fileHashSha256);

    ImportJobRecord create(ImportJobRecord job);

    Optional<ImportJobRecord> findById(UUID id);

    Page<ImportJobRecord> list(Pageable pageable);

    Page<ImportJobErrorRecord> listErrors(UUID importJobId, Pageable pageable);

    void replaceErrors(UUID importJobId, List<ImportJobErrorRecord> errors, int totalErrorCount);

    void markCommitted(UUID importJobId, int totalRows, int validRows);

    boolean supersede(UUID importJobId, String reason);
}
