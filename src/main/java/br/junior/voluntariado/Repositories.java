package br.junior.voluntariado;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
interface ActivityRepository extends JpaRepository<Activity,Long> {}
interface ShiftRepository extends JpaRepository<VolunteerShift,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select s from VolunteerShift s where s.id = :id")
 Optional<VolunteerShift> lockById(@Param("id") Long id);
}
interface EnrollmentRepository extends JpaRepository<Enrollment,Long> {
 long countByShiftId(Long shiftId);
 boolean existsByShiftIdAndVolunteerCode(Long shiftId,String volunteerCode);
}
