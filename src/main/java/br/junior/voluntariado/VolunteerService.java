package br.junior.voluntariado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class VolunteerService {
 private final ShiftRepository shifts;
 private final EnrollmentRepository enrollments;
 VolunteerService(ShiftRepository shifts,EnrollmentRepository enrollments){this.shifts=shifts;this.enrollments=enrollments;}
 @Transactional
 public EnrollmentSummary enroll(Long shiftId,String code){
  VolunteerShift shift=shifts.lockById(shiftId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Turno inexistente"));
  if(code==null || code.trim().isEmpty() || code.trim().length()>80)throw new IllegalArgumentException("Identificador fictício inválido");
  String value=code.trim();
  if(enrollments.existsByShiftIdAndVolunteerCode(shiftId,value))throw new IllegalStateException("Inscrição duplicada");
  if(enrollments.countByShiftId(shiftId)>=shift.capacity)throw new IllegalStateException("Turno sem vagas");
  Enrollment e=enrollments.saveAndFlush(new Enrollment(shift,value));
  return new EnrollmentSummary(e.id,shiftId,shift.capacity-enrollments.countByShiftId(shiftId));
 }
 public ShiftSummary summary(Long shiftId){
  VolunteerShift s=shifts.findById(shiftId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Turno inexistente"));
  long taken=enrollments.countByShiftId(shiftId);
  return new ShiftSummary(s.id,s.activity.id,s.capacity,taken,s.capacity-taken);
 }
 public record EnrollmentSummary(Long id,Long shiftId,long remaining){}
 public record ShiftSummary(Long id,Long activityId,int capacity,long occupied,long remaining){}
}
