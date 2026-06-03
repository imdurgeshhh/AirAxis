package com.airline.seat.service;

import com.airline.config.AppProperties;
import com.airline.exception.ResourceNotFoundException;
import com.airline.exception.SeatAlreadyLockedException;
import com.airline.seat.dto.SeatDTO;
import com.airline.seat.entity.Seat;
import com.airline.seat.repository.SeatRepository;
import com.airline.user.entity.User;
import com.airline.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    private static final String SEAT_LOCK_PREFIX = "seat:lock:";

    // =============================================
    // GET SEAT MAP (for a flight)
    // =============================================
    @Transactional(readOnly = true)
    public List<SeatDTO> getSeatMap(Long flightId, Long currentUserId) {
        List<Seat> seats = seatRepository.findByFlightIdOrderBySeatNumber(flightId);
        return seats.stream()
                .map(seat -> toDTO(seat, currentUserId))
                .collect(Collectors.toList());
    }

    // =============================================
    // LOCK SEAT (Step 4 — Redis TTL lock)
    // =============================================
    @Transactional
    public SeatDTO lockSeat(Long flightId, Long seatId, Long userId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", "id", seatId));

        if (!seat.getFlight().getId().equals(flightId)) {
            throw new ResourceNotFoundException("Seat", "flightId", flightId);
        }

        // Check Redis lock first
        String lockKey = buildLockKey(flightId, seatId);
        String existingLock = redisTemplate.opsForValue().get(lockKey);

        if (existingLock != null && !existingLock.equals(userId.toString())) {
            throw new SeatAlreadyLockedException(
                    "Seat " + seat.getSeatNumber() + " is already locked by another user");
        }

        if (!seat.isAvailable()) {
            throw new SeatAlreadyLockedException(
                    "Seat " + seat.getSeatNumber() + " is not available");
        }

        // Set Redis lock with TTL
        int ttlMinutes = appProperties.getSeatLock().getTtlMinutes();
        redisTemplate.opsForValue().set(lockKey, userId.toString(), Duration.ofMinutes(ttlMinutes));

        // Update DB fallback
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        seat.setAvailable(false);
        seat.setLockedUntil(LocalDateTime.now().plusMinutes(ttlMinutes));
        seat.setLockedByUser(user);
        seatRepository.save(seat);

        log.info("Seat {} locked by user {} on flight {} for {} minutes",
                seat.getSeatNumber(), userId, flightId, ttlMinutes);

        return toDTO(seat, userId);
    }

    // =============================================
    // RELEASE SEAT LOCK
    // =============================================
    @Transactional
    public void releaseSeat(Long flightId, Long seatId, Long userId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", "id", seatId));

        // Only the user who locked it (or admin) can release
        String lockKey = buildLockKey(flightId, seatId);
        String lockedBy = redisTemplate.opsForValue().get(lockKey);

        if (lockedBy != null && lockedBy.equals(userId.toString())) {
            redisTemplate.delete(lockKey);
        }

        // Update DB
        seat.setAvailable(true);
        seat.setLockedUntil(null);
        seat.setLockedByUser(null);
        seatRepository.save(seat);

        log.info("Seat {} released by user {} on flight {}", seat.getSeatNumber(), userId, flightId);
    }

    // =============================================
    // SCHEDULED: Clean up expired DB locks
    // Runs every 2 minutes as a safety net
    // =============================================
    @Scheduled(fixedRate = 120_000)
    @Transactional
    public void cleanupExpiredLocks() {
        int released = seatRepository.releaseExpiredLocks(LocalDateTime.now());
        if (released > 0) {
            log.info("Cleanup: released {} expired seat locks", released);
        }
    }

    // =============================================
    // INTERNAL: Mark seat as booked (called by BookingService)
    // =============================================
    @Transactional
    public void markSeatAsBooked(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", "id", seatId));
        seat.setAvailable(false);
        seat.setLockedUntil(null);
        seat.setLockedByUser(null);
        seatRepository.save(seat);

        // Also remove Redis lock
        String lockKey = SEAT_LOCK_PREFIX + seat.getFlight().getId() + ":" + seatId;
        redisTemplate.delete(lockKey);
    }

    // ---- Helpers ----

    private String buildLockKey(Long flightId, Long seatId) {
        return SEAT_LOCK_PREFIX + flightId + ":" + seatId;
    }

    private SeatDTO toDTO(Seat seat, Long currentUserId) {
        boolean lockedByCurrent = false;
        if (seat.getLockedByUser() != null && currentUserId != null) {
            lockedByCurrent = seat.getLockedByUser().getId().equals(currentUserId);
        }

        return SeatDTO.builder()
                .id(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .seatClass(seat.getSeatClass().name())
                .available(seat.isAvailable())
                .lockedByCurrentUser(lockedByCurrent)
                .build();
    }
}
