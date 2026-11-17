package pe.com.edu.prisma.logbook;

import pe.com.edu.prisma.logbook.dto.CreateLogEntryRequest;
import pe.com.edu.prisma.logbook.dto.LogEntryDto;
import pe.com.edu.prisma.logbook.dto.LogEntryPageDto;

/** A student's logbook (REQ-LOGBOOK-01/02): append-only entries and their history. */
public interface LogbookService {

    LogEntryPageDto findByStudent(Long studentId, int page, int size);

    LogEntryDto create(Long studentId, CreateLogEntryRequest request);
}
