package io.github.adnansaliq.workmyresume.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "resume_analyses")
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private ResumeVersion version;

    @Column(name = "job_description", columnDefinition = "text")
    private String jobDescription;

    @Column(name = "score")
    private Integer score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths", columnDefinition = "jsonb")
    private List<String> strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gaps", columnDefinition = "jsonb")
    private List<String> gaps;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suggestions", columnDefinition = "jsonb")
    private List<String> suggestions;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}