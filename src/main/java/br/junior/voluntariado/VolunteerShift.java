package br.junior.voluntariado;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="volunteer_shifts")
public class VolunteerShift {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="activity_id",nullable=false) public Activity activity;
 @Column(nullable=false) public int capacity;
 @Column(nullable=false) public LocalDateTime startsAt;
 @Column(nullable=false) public LocalDateTime endsAt;
 protected VolunteerShift(){}
 public VolunteerShift(Activity activity,int capacity,LocalDateTime startsAt,LocalDateTime endsAt){
  this.activity=activity;this.capacity=capacity;this.startsAt=startsAt;this.endsAt=endsAt;
 }
}
