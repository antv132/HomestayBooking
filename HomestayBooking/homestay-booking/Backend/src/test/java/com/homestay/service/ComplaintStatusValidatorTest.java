package com.homestay.service;

import com.homestay.entity.Complaint;
import com.homestay.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComplaintStatusValidatorTest {

    private ComplaintStatusValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ComplaintStatusValidator();
    }

    @Test
    void validTransition_openToInvestigating() {
        assertDoesNotThrow(() ->
                validator.validateTransition(Complaint.Status.OPEN, Complaint.Status.INVESTIGATING));
    }

    @Test
    void validTransition_investigatingToResolved() {
        assertDoesNotThrow(() ->
                validator.validateTransition(Complaint.Status.INVESTIGATING, Complaint.Status.RESOLVED));
    }

    @Test
    void validTransition_resolvedToClosed() {
        assertDoesNotThrow(() ->
                validator.validateTransition(Complaint.Status.RESOLVED, Complaint.Status.CLOSED));
    }

    @Test
    void invalidTransition_openToResolved_throws() {
        assertThrows(BusinessException.class, () ->
                validator.validateTransition(Complaint.Status.OPEN, Complaint.Status.RESOLVED));
    }

    @Test
    void invalidTransition_openToClosed_throws() {
        assertThrows(BusinessException.class, () ->
                validator.validateTransition(Complaint.Status.OPEN, Complaint.Status.CLOSED));
    }

    @Test
    void invalidTransition_closedToAnything_throws() {
        assertThrows(BusinessException.class, () ->
                validator.validateTransition(Complaint.Status.CLOSED, Complaint.Status.OPEN));
    }

    @Test
    void invalidTransition_sameStatus_throws() {
        assertThrows(BusinessException.class, () ->
                validator.validateTransition(Complaint.Status.OPEN, Complaint.Status.OPEN));
    }

    @Test
    void invalidTransition_nullCurrent_throws() {
        assertThrows(BusinessException.class, () ->
                validator.validateTransition(null, Complaint.Status.INVESTIGATING));
    }

    @Test
    void invalidTransition_nullNext_throws() {
        assertThrows(BusinessException.class, () ->
                validator.validateTransition(Complaint.Status.OPEN, null));
    }

    @Test
    void resolutionNotes_resolvedWithSufficientNotes_ok() {
        assertDoesNotThrow(() ->
                validator.validateResolutionNotes(Complaint.Status.RESOLVED, "This is sufficient resolution notes", null));
    }

    @Test
    void resolutionNotes_resolvedWithShortNotes_throws() {
        assertThrows(BusinessException.class, () ->
                validator.validateResolutionNotes(Complaint.Status.RESOLVED, "short", null));
    }

    @Test
    void resolutionNotes_resolvedWithNullNotesButExistingSufficient_ok() {
        assertDoesNotThrow(() ->
                validator.validateResolutionNotes(Complaint.Status.RESOLVED, null, "Previously stored notes that are long enough"));
    }

    @Test
    void resolutionNotes_resolvedWithNullNotesAndNoExisting_throws() {
        assertThrows(BusinessException.class, () ->
                validator.validateResolutionNotes(Complaint.Status.RESOLVED, null, null));
    }

    @Test
    void resolutionNotes_notResolved_noValidation() {
        assertDoesNotThrow(() ->
                validator.validateResolutionNotes(Complaint.Status.INVESTIGATING, null, null));
    }
}
