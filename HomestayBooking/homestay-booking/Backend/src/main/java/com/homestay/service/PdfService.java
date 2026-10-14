package com.homestay.service;

import com.homestay.entity.Contract;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] generateContractPdf(Contract contract) {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.DARK_GRAY);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);

            Paragraph title = new Paragraph("ACCOMMODATION CONTRACT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            document.add(new Paragraph("Contract ID: " + contract.getId(), normalFont));
            document.add(new Paragraph("Booking ID: " + contract.getBooking().getId(), normalFont));
            document.add(new Paragraph("Date: " + resolveContractDate(contract).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), normalFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            addTableCell(table, "Customer Name", headerFont);
            addTableCell(table, contract.getCustomer().getFullName() != null ? contract.getCustomer().getFullName() : "Unknown", normalFont);

            addTableCell(table, "Customer Email", headerFont);
            addTableCell(table, contract.getCustomer().getEmail(), normalFont);

            addTableCell(table, "Property", headerFont);
            addTableCell(table, contract.getRoom().getProperty().getName(), normalFont);

            addTableCell(table, "Room", headerFont);
            addTableCell(table, contract.getRoom().getRoomNumber(), normalFont);

            addTableCell(table, "Check-in Date", headerFont);
            addTableCell(table, contract.getCheckInDate().toString(), normalFont);

            addTableCell(table, "Check-out Date", headerFont);
            addTableCell(table, contract.getCheckOutDate().toString(), normalFont);

            addTableCell(table, "Deposit Amount", headerFont);
            addTableCell(table, "VND " + contract.getDepositAmount().toString(), normalFont);

            addTableCell(table, "Total Amount", headerFont);
            addTableCell(table, "VND " + contract.getTotalAmount().toString(), normalFont);

            document.add(table);

            document.add(new Paragraph("Terms and Conditions:", headerFont));
            document.add(new Paragraph("1. Check-in time is from 14:00. Check-out time is before 12:00.", normalFont));
            document.add(new Paragraph("2. The deposit is non-refundable in case of cancellation after payment.", normalFont));
            document.add(new Paragraph("3. Guests are responsible for any damages to the property during their stay.", normalFont));
            document.add(new Paragraph(" "));

            Paragraph footer = new Paragraph("Thank you for choosing our service!", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 12, BaseColor.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

        } catch (DocumentException e) {
            throw new RuntimeException("Lỗi khi tạo file PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    /** createdAt may still be null pre-flush after persist — fall back to generatedAt / now. */
    private static LocalDateTime resolveContractDate(Contract contract) {
        if (contract.getCreatedAt() != null) {
            return contract.getCreatedAt();
        }
        if (contract.getGeneratedAt() != null) {
            return contract.getGeneratedAt();
        }
        return LocalDateTime.now();
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        table.addCell(cell);
    }
}
