package alp.semester2.tugas.controller;

import alp.semester2.tugas.model.Reminder;
import alp.semester2.tugas.repository.ReminderRepository;
import alp.semester2.tugas.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

/**
 * ReminderController
 * -----------------------------------------------
 * GET    /api/reminders        → semua pengingat user
 * POST   /api/reminders        → buat pengingat baru
 * PUT    /api/reminders/{id}   → update pengingat
 * DELETE /api/reminders/{id}   → hapus pengingat
 */
@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderRepository reminderRepo;
    private final UserRepository     userRepo;

    public ReminderController(ReminderRepository reminderRepo, UserRepository userRepo) {
        this.reminderRepo = reminderRepo;
        this.userRepo     = userRepo;
    }

    @GetMapping
    public ResponseEntity<List<Reminder>> getAll(@AuthenticationPrincipal UserDetails ud) {
        Long userId = getUserId(ud);
        return ResponseEntity.ok(reminderRepo.findByUserIdOrderByScheduleTimeAsc(userId));
    }

    @PostMapping
    public ResponseEntity<Reminder> create(
            @Valid @RequestBody ReminderRequest req,
            @AuthenticationPrincipal UserDetails ud) {

        Long userId = getUserId(ud);
        Reminder r = new Reminder();
        r.setUserId(userId);
        r.setType(Reminder.Type.valueOf(req.getType()));
        r.setLabel(req.getLabel());
        r.setScheduleTime(LocalTime.parse(req.getScheduleTime())); // "08:00"
        r.setRepeatDays(req.getRepeatDays());
        r.setActive(true);
        return ResponseEntity.ok(reminderRepo.save(r));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<Reminder> toggle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {

        Long userId = getUserId(ud);
        Reminder r = reminderRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Pengingat tidak ditemukan"));

        if (!r.getUserId().equals(userId)) return ResponseEntity.status(403).build();

        r.setActive(!r.isActive());
        return ResponseEntity.ok(reminderRepo.save(r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {

        Long userId = getUserId(ud);
        Reminder r = reminderRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Pengingat tidak ditemukan"));

        if (!r.getUserId().equals(userId)) return ResponseEntity.status(403).build();

        reminderRepo.delete(r);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(UserDetails ud) {
        return userRepo.findByEmail(ud.getUsername())
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"))
            .getId();
    }

    @Data
    public static class ReminderRequest {
        @NotBlank private String type;         // "periksa" / "obat" / "pola_hidup"
        @NotBlank private String label;        // "Minum obat hipertensi"
        @NotBlank private String scheduleTime; // "08:00"
        @NotBlank private String repeatDays;   // "daily" atau "Mon,Wed,Fri"
    }
}
