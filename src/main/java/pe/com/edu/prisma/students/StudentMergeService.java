package pe.com.edu.prisma.students;

import pe.com.edu.prisma.students.dto.MergePreviewDto;
import pe.com.edu.prisma.students.dto.StudentDto;

/**
 * Merging two records of the same student (agreements of 2026-11-18),
 * ADMIN only: everything of {@code removeId} moves to {@code keepId} and
 * {@code removeId} is deleted for good.
 */
public interface StudentMergeService {

    MergePreviewDto preview(Long keepId, Long removeId);

    StudentDto merge(Long keepId, Long removeId);
}
