package com.airline.notification;

import com.airline.payment.event.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Sends booking confirmation emails.
 * Uses Spring Mail with HTML templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    /**
     * Sends a booking confirmation email with flight details and PNR.
     */
    public void sendBookingConfirmation(BookingConfirmedEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(event.getEmail());
            helper.setSubject("✈ Booking Confirmed — PNR: " + event.getPnr());
            helper.setFrom("noreply@airaxis.com");

            String htmlContent = buildConfirmationHtml(event);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Confirmation email sent: to={}, PNR={}", event.getEmail(), event.getPnr());

        } catch (MessagingException | MailException e) {
            log.error("Failed to send confirmation email to {} for PNR {}: {}",
                    event.getEmail(), event.getPnr(), e.getMessage(), e);
            // Don't throw — the booking is already confirmed.
            // Failed emails can be retried via a scheduled job later.
        }
    }

    private String buildConfirmationHtml(BookingConfirmedEvent event) {
        String departure = event.getDepartureTime() != null
                ? event.getDepartureTime().format(DATE_FMT)
                : "TBD";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Arial, sans-serif; background: #f4f7fa; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px;
                                     box-shadow: 0 2px 12px rgba(0,0,0,0.08); overflow: hidden; }
                        .header { background: linear-gradient(135deg, #1a237e 0%%, #3949ab 100%%);
                                  color: white; padding: 32px 24px; text-align: center; }
                        .header h1 { margin: 0; font-size: 24px; font-weight: 600; }
                        .header .pnr { font-size: 36px; font-weight: 700; letter-spacing: 6px;
                                       margin-top: 12px; font-family: 'Courier New', monospace; }
                        .body { padding: 24px; }
                        .detail-row { display: flex; justify-content: space-between; padding: 12px 0;
                                      border-bottom: 1px solid #eee; }
                        .detail-label { color: #666; font-size: 14px; }
                        .detail-value { font-weight: 600; color: #1a237e; font-size: 14px; }
                        .flight-route { text-align: center; padding: 20px; font-size: 20px; font-weight: 600; color: #333; }
                        .footer { background: #f8f9fa; padding: 16px 24px; text-align: center;
                                  font-size: 12px; color: #999; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>✈ Booking Confirmed</h1>
                            <div class="pnr">%s</div>
                        </div>
                        <div class="body">
                            <div class="flight-route">%s → %s</div>
                            <div class="detail-row">
                                <span class="detail-label">Flight</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Departure</span>
                                <span class="detail-value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="detail-label">Amount Paid</span>
                                <span class="detail-value">₹%s</span>
                            </div>
                        </div>
                        <div class="footer">
                            AirAxis — Your journey starts here.<br>
                            Please arrive at the airport at least 2 hours before departure.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                event.getPnr(),
                event.getOrigin(),
                event.getDestination(),
                event.getFlightNumber(),
                departure,
                event.getTotalAmount().toPlainString()
        );
    }
}
