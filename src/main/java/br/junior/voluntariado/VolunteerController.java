package br.junior.voluntariado;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api")
public class VolunteerController {
 private final ActivityRepository activities;private final ShiftRepository shifts;private final VolunteerService service;
 public VolunteerController(ActivityRepository activities,ShiftRepository shifts,VolunteerService service){
  this.activities=activities;this.shifts=shifts;this.service=service;
 }
 public record NewActivity(String title){}
 public record NewShift(Long activityId,Integer capacity,LocalDateTime startsAt,LocalDateTime endsAt){}
 public record NewEnrollment(String volunteerCode){}
 @PostMapping("/activities")
 public ResponseEntity<?> activity(@RequestBody NewActivity body){
  if(body.title()==null||body.title().trim().isEmpty()||body.title().length()>160)throw new IllegalArgumentException("Ação inválida");
  Activity a=activities.save(new Activity(body.title().trim()));
  return ResponseEntity.status(201).body(Map.of("id",a.id,"title",a.title));
 }
 @PostMapping("/shifts")
 public ResponseEntity<?> shift(@RequestBody NewShift body){
  if(body.capacity()==null||body.capacity()<=0||body.startsAt()==null||body.endsAt()==null||!body.endsAt().isAfter(body.startsAt()))throw new IllegalArgumentException("Capacidade ou horários inválidos");
  Activity a=activities.findById(body.activityId()==null?-1L:body.activityId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ação inexistente"));
  VolunteerShift s=shifts.save(new VolunteerShift(a,body.capacity(),body.startsAt(),body.endsAt()));
  return ResponseEntity.status(201).body(Map.of("id",s.id,"activityId",a.id,"capacity",s.capacity));
 }
 @GetMapping("/shifts/{id}")
 public VolunteerService.ShiftSummary show(@PathVariable Long id){return service.summary(id);}
 @PostMapping("/shifts/{id}/enrollments")
 public ResponseEntity<?> enroll(@PathVariable Long id,@RequestBody NewEnrollment request){
  return ResponseEntity.status(201).body(service.enroll(id,request.volunteerCode()));
 }
 @ExceptionHandler(IllegalArgumentException.class)
 public ResponseEntity<?> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
 @ExceptionHandler(IllegalStateException.class)
 public ResponseEntity<?> conflict(IllegalStateException e){return ResponseEntity.status(409).body(Map.of("error",e.getMessage()));}
}
