package io.github.adnansaliq.workmyresume.repository;

import io.github.adnansaliq.workmyresume.domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserId(Long userId);
}