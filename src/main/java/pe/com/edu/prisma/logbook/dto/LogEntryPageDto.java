package pe.com.edu.prisma.logbook.dto;

import java.util.List;

/** One page of a student's logbook, newest session first; {@code hasMore} = there are older entries. */
public record LogEntryPageDto(List<LogEntryDto> entries, int page, int size, long totalElements, boolean hasMore) {
}
