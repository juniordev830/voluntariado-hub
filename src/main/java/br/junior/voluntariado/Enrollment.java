package br.junior.voluntariado;
import jakarta.persistence.*;
@Entity
@Table(name="enrollments",uniqueConstraints=@UniqueConstraint(columnNames={"shift_id","volunteer_code"}))
public class Enrollment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="shift_id",nullable=false) public VolunteerShift shift;
 @Column(name="volunteer_code",nullable=false,length=80) public String volunteerCode;
 protected Enrollment(){}
 public Enrollment(VolunteerShift shift,String code){this.shift=shift;this.volunteerCode=code;}
}
