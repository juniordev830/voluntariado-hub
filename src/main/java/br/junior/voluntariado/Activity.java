package br.junior.voluntariado;
import jakarta.persistence.*;
@Entity
@Table(name="activities")
public class Activity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=160) public String title;
 protected Activity(){}
 public Activity(String title){this.title=title;}
}
