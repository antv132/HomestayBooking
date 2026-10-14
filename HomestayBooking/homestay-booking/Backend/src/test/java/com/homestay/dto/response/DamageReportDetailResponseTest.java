package com.homestay.dto.response;

import com.homestay.entity.Attachment;
import com.homestay.entity.Booking;
import com.homestay.entity.DamageItem;
import com.homestay.entity.DamageReport;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DamageReportDetailResponseTest {

    @Test
    void fromEntity_withoutAttachments_mapsAllFields() {
        DamageReport report = createDamageReport();

        DamageReportDetailResponse response = DamageReportDetailResponse.fromEntity(report);

        assertThat(response.getId()).isEqualTo(report.getId());
        assertThat(response.getBookingId()).isEqualTo(report.getBooking().getId());
        assertThat(response.getRoomNumber()).isEqualTo("101");
        assertThat(response.getPropertyId()).isEqualTo(report.getInspection().getProperty().getId());
        assertThat(response.getPropertyName()).isEqualTo("Test Property");
        assertThat(response.getTotalEstimatedCost()).isEqualByComparingTo(new BigDecimal("2000000"));
        assertThat(response.getApprovedAmount()).isNull();
        assertThat(response.getStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(response.getInspectorName()).isEqualTo("Inspector User");
        assertThat(response.getApprovedByName()).isNull();
        assertThat(response.getRequiresAdminEscalation()).isTrue();
        assertThat(response.getNote()).isEqualTo("Severe damage");
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getAttachments()).isEmpty();
    }

    @Test
    void fromEntity_withAttachments_mapsAttachments() {
        DamageReport report = createDamageReport();
        
        Attachment att1 = new Attachment();
        att1.setFileUrl("/uploads/damage1.jpg");
        
        Attachment att2 = new Attachment();
        att2.setFileUrl("/uploads/damage2.jpg");

        List<Attachment> attachments = Arrays.asList(att1, att2);

        DamageReportDetailResponse response = DamageReportDetailResponse.fromEntity(report, attachments);

        assertThat(response.getAttachments()).hasSize(2);
        assertThat(response.getAttachments().get(0).getUrl()).isEqualTo("/uploads/damage1.jpg");
        assertThat(response.getAttachments().get(0).getType()).isEqualTo("IMAGE");
        assertThat(response.getAttachments().get(1).getUrl()).isEqualTo("/uploads/damage2.jpg");
    }

    @Test
    void fromEntity_mapsDamageItems() {
        DamageReport report = createDamageReport();

        DamageReportDetailResponse response = DamageReportDetailResponse.fromEntity(report);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getItemName()).isEqualTo("Broken TV");
        assertThat(response.getItems().get(0).getDescription()).isEqualTo("Screen cracked");
        assertThat(response.getItems().get(0).getEstimatedCost()).isEqualByComparingTo(new BigDecimal("1500000"));
        assertThat(response.getItems().get(1).getItemName()).isEqualTo("Damaged bed");
    }

    @Test
    void fromEntity_withApproval_mapsApprovedFields() {
        DamageReport report = createDamageReport();
        
        User approver = new User();
        approver.setFullName("Admin Approver");
        
        report.setApprovedBy(approver);
        report.setApprovedAmount(new BigDecimal("1800000"));
        report.setStatus(DamageReport.Status.APPROVED);

        DamageReportDetailResponse response = DamageReportDetailResponse.fromEntity(report);

        assertThat(response.getApprovedByName()).isEqualTo("Admin Approver");
        assertThat(response.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("1800000"));
        assertThat(response.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void fromEntity_withNullInspector_handlesGracefully() {
        DamageReport report = createDamageReport();
        report.getInspection().setInspectedBy(null);

        DamageReportDetailResponse response = DamageReportDetailResponse.fromEntity(report);

        assertThat(response.getInspectorName()).isNull();
    }

    @Test
    void fromEntity_withNullItemsList_handlesGracefully() {
        DamageReport report = createDamageReport();
        report.setItems(null);

        DamageReportDetailResponse response = DamageReportDetailResponse.fromEntity(report);

        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void fromEntity_withNullAttachmentsList_handlesGracefully() {
        DamageReport report = createDamageReport();

        DamageReportDetailResponse response = DamageReportDetailResponse.fromEntity(report, null);

        assertThat(response.getAttachments()).isEmpty();
    }

    @Test
    void damageItemResponseFromEntity_mapsAllFields() {
        DamageItem item = new DamageItem();
        item.setId(UUID.randomUUID());
        item.setItemName("Broken Window");
        item.setDescription("Glass shattered");
        item.setEstimatedCost(new BigDecimal("300000"));

        DamageReportDetailResponse.DamageItemResponse response =
                DamageReportDetailResponse.DamageItemResponse.fromEntity(item);

        assertThat(response.getId()).isEqualTo(item.getId());
        assertThat(response.getItemName()).isEqualTo("Broken Window");
        assertThat(response.getDescription()).isEqualTo("Glass shattered");
        assertThat(response.getEstimatedCost()).isEqualByComparingTo(new BigDecimal("300000"));
    }

    private DamageReport createDamageReport() {
        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setName("Test Property");

        Room room = new Room();
        room.setRoomNumber("101");

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());

        User inspector = new User();
        inspector.setFullName("Inspector User");

        RoomInspection inspection = new RoomInspection();
        inspection.setId(UUID.randomUUID());
        inspection.setProperty(property);
        inspection.setRoom(room);
        inspection.setInspectedBy(inspector);

        DamageItem item1 = new DamageItem();
        item1.setId(UUID.randomUUID());
        item1.setItemName("Broken TV");
        item1.setDescription("Screen cracked");
        item1.setEstimatedCost(new BigDecimal("1500000"));

        DamageItem item2 = new DamageItem();
        item2.setId(UUID.randomUUID());
        item2.setItemName("Damaged bed");
        item2.setDescription("Frame broken");
        item2.setEstimatedCost(new BigDecimal("500000"));

        DamageReport report = new DamageReport();
        report.setId(UUID.randomUUID());
        report.setBooking(booking);
        report.setInspection(inspection);
        report.setTotalEstimatedCost(new BigDecimal("2000000"));
        report.setStatus(DamageReport.Status.PENDING_APPROVAL);
        report.setRequiresAdminEscalation(true);
        report.setNote("Severe damage");
        report.setCreatedAt(LocalDateTime.now());
        report.setItems(Arrays.asList(item1, item2));

        return report;
    }
}
