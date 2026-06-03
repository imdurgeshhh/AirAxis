package com.airline.eticket;

import com.airline.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class ETicketController {

    private final ETicketService eTicketService;

    /**
     * GET /api/bookings/pnr/{pnr}/eticket
     * Downloads the e-ticket PDF for a confirmed booking.
     */
    @GetMapping("/pnr/{pnr}/eticket")
    public ResponseEntity<byte[]> downloadETicket(@PathVariable String pnr) {

        byte[] pdfBytes = eTicketService.generateETicket(pnr);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "AirAxis-ETicket-" + pnr.toUpperCase() + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
