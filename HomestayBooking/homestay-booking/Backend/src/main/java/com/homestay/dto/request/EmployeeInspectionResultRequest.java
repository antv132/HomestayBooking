package com.homestay.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/** SCR-62 - Pass/Fail inspection body with structured checklist answers. */
@Data
public class EmployeeInspectionResultRequest {

    @JsonAlias("notes")
    @Size(max = 2000, message = "Note must not exceed 2000 characters")
    private String note;

    /** Preferred: answers keyed by definition id. */
    @Valid
    private List<Answer> answers;

    /** Legacy map by code (tv/ac/...) — still accepted and mapped to definitions. */
    private LegacyChecklist checklist;

    public String resolveNote() {
        return StringUtils.hasText(note) ? note.trim() : null;
    }

    @Data
    public static class Answer {
        @NotNull
        private UUID itemId;
        @NotNull
        private Boolean passed;
    }

    @Data
    public static class LegacyChecklist {
        private Boolean tv;
        private Boolean minibar;
        private Boolean ac;
        private Boolean bathroom;
        private Boolean beds;
    }
}
