package com.homestay.dto.response;



import com.homestay.entity.HousekeepingTask;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;



import java.time.LocalDateTime;

import java.util.UUID;



@Data

@NoArgsConstructor

@AllArgsConstructor

public class HousekeepingTaskSummaryResponse {



    private UUID id;

    private UUID propertyId;

    private String propertyName;

    private UUID roomId;

    private String roomNumber;

    private UUID bookingId;

    private String status;

    private UUID assigneeId;

    private String assigneeName;

    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String note;



    public static HousekeepingTaskSummaryResponse fromEntity(HousekeepingTask task) {

        return new HousekeepingTaskSummaryResponse(

                task.getId(),

                task.getProperty().getId(),

                task.getProperty().getName(),

                task.getRoom().getId(),

                task.getRoom().getRoomNumber(),

                task.getBooking() != null ? task.getBooking().getId() : null,

                task.getStatus().name(),

                task.getAssignedEmployee() != null ? task.getAssignedEmployee().getId() : null,

                task.getAssignedEmployee() != null ? task.getAssignedEmployee().getFullName() : null,

                task.getCreatedAt(),

                task.getStartedAt(),

                task.getCompletedAt(),

                task.getNote()

        );

    }

}

