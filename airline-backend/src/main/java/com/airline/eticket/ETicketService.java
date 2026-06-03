package com.airline.eticket;

import com.airline.booking.entity.Booking;
import com.airline.booking.entity.BookingStatus;
import com.airline.booking.entity.Passenger;
import com.airline.booking.repository.BookingRepository;
import com.airline.exception.BookingException;
import com.airline.exception.ResourceNotFoundException;
import com.airline.flight.entity.Flight;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Generates E-ticket PDFs for confirmed bookings.
 * Uses iText 7 for PDF creation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ETicketService {

    private final BookingRepository bookingRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private static final Color HEADER_BG = new DeviceRgb(26, 35, 126);    // #1a237e
    private static final Color ACCENT = new DeviceRgb(57, 73, 171);       // #3949ab
    private static final Color LIGHT_GRAY = new DeviceRgb(245, 245, 245);

    /**
     * Generates a PDF e-ticket for a confirmed booking.
     *
     * @param pnr booking PNR code
     * @return PDF bytes
     */
    @Transactional(readOnly = true)
    public byte[] generateETicket(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "PNR", pnr));

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException("E-ticket is only available for CONFIRMED bookings. Current status: "
                    + booking.getBookingStatus());
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(30, 40, 30, 40);

            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            Flight flight = booking.getFlight();

            // ---- HEADER ----
            doc.add(new Paragraph("AirAxis")
                    .setFont(bold).setFontSize(28).setFontColor(HEADER_BG)
                    .setTextAlignment(TextAlignment.LEFT).setMarginBottom(2));

            doc.add(new Paragraph("Electronic Ticket / Boarding Pass")
                    .setFont(regular).setFontSize(10).setFontColor(ACCENT)
                    .setMarginBottom(15));

            // ---- PNR ----
            doc.add(new Paragraph("PNR: " + booking.getPnr())
                    .setFont(bold).setFontSize(22).setFontColor(HEADER_BG)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // ---- FLIGHT INFO TABLE ----
            Table flightTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2}))
                    .useAllAvailableWidth().setMarginBottom(20);

            addCell(flightTable, "Flight", bold, LIGHT_GRAY);
            addCell(flightTable, flight.getFlightNumber(), regular, null);
            addCell(flightTable, "Date", bold, LIGHT_GRAY);
            addCell(flightTable, flight.getDepartureTime().format(DATE_FMT), regular, null);

            addCell(flightTable, "From", bold, LIGHT_GRAY);
            addCell(flightTable, flight.getOriginAirport().getCity() + " (" +
                    flight.getOriginAirport().getIataCode() + ")", regular, null);
            addCell(flightTable, "Departure", bold, LIGHT_GRAY);
            addCell(flightTable, flight.getDepartureTime().format(TIME_FMT), regular, null);

            addCell(flightTable, "To", bold, LIGHT_GRAY);
            addCell(flightTable, flight.getDestinationAirport().getCity() + " (" +
                    flight.getDestinationAirport().getIataCode() + ")", regular, null);
            addCell(flightTable, "Arrival", bold, LIGHT_GRAY);
            addCell(flightTable, flight.getArrivalTime().format(TIME_FMT), regular, null);

            addCell(flightTable, "Status", bold, LIGHT_GRAY);
            addCell(flightTable, booking.getBookingStatus().name(), regular, null);
            addCell(flightTable, "Amount", bold, LIGHT_GRAY);
            addCell(flightTable, "₹" + booking.getTotalAmount().toPlainString(), regular, null);

            doc.add(flightTable);

            // ---- PASSENGER TABLE ----
            doc.add(new Paragraph("Passengers")
                    .setFont(bold).setFontSize(14).setFontColor(HEADER_BG)
                    .setMarginBottom(8));

            Table paxTable = new Table(UnitValue.createPercentArray(new float[]{0.5f, 2, 2, 1.5f, 1, 1}))
                    .useAllAvailableWidth().setMarginBottom(20);

            // Header row
            String[] headers = {"#", "First Name", "Last Name", "Passport", "Seat", "Class"};
            for (String h : headers) {
                paxTable.addHeaderCell(new Cell().add(new Paragraph(h).setFont(bold).setFontSize(9))
                        .setBackgroundColor(HEADER_BG)
                        .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(6));
            }

            int idx = 1;
            for (Passenger p : booking.getPassengers()) {
                Color rowBg = (idx % 2 == 0) ? LIGHT_GRAY : null;
                addCell(paxTable, String.valueOf(idx), regular, rowBg);
                addCell(paxTable, p.getFirstName(), regular, rowBg);
                addCell(paxTable, p.getLastName(), regular, rowBg);
                addCell(paxTable, p.getPassportNumber() != null ? p.getPassportNumber() : "-", regular, rowBg);
                addCell(paxTable, p.getSeat().getSeatNumber(), regular, rowBg);
                addCell(paxTable, p.getSeat().getSeatClass().name(), regular, rowBg);
                idx++;
            }

            doc.add(paxTable);

            // ---- FOOTER ----
            doc.add(new Paragraph("This is a computer-generated e-ticket. Please present this at the airport counter.")
                    .setFont(regular).setFontSize(8).setFontColor(ACCENT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30));

            doc.add(new Paragraph("AirAxis — Your journey starts here.")
                    .setFont(bold).setFontSize(9).setFontColor(HEADER_BG)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.close();

            log.info("E-ticket PDF generated for PNR: {}", pnr);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate e-ticket PDF for PNR {}: {}", pnr, e.getMessage(), e);
            throw new RuntimeException("Failed to generate e-ticket", e);
        }
    }

    private void addCell(Table table, String text, PdfFont font, Color bg) {
        Cell cell = new Cell().add(new Paragraph(text).setFont(font).setFontSize(10))
                .setPadding(6).setTextAlignment(TextAlignment.LEFT);
        if (bg != null) {
            cell.setBackgroundColor(bg);
        }
        table.addCell(cell);
    }
}
