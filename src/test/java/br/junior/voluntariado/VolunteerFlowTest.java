package br.junior.voluntariado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
class VolunteerFlowTest {
 @Autowired MockMvc mvc;
 @Test void twoSlotsRejectThirdAndDuplicate() throws Exception {
  mvc.perform(post("/api/activities").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Mutirão\"}")).andExpect(status().isCreated());
  mvc.perform(post("/api/shifts").contentType(MediaType.APPLICATION_JSON).content("{\"activityId\":1,\"capacity\":2,\"startsAt\":\"2026-11-08T09:00:00\",\"endsAt\":\"2026-11-08T11:00:00\"}")).andExpect(status().isCreated());
  mvc.perform(post("/api/shifts/1/enrollments").contentType(MediaType.APPLICATION_JSON).content("{\"volunteerCode\":\"teste-1\"}")).andExpect(status().isCreated());
  mvc.perform(post("/api/shifts/1/enrollments").contentType(MediaType.APPLICATION_JSON).content("{\"volunteerCode\":\"teste-1\"}")).andExpect(status().isConflict());
  mvc.perform(post("/api/shifts/1/enrollments").contentType(MediaType.APPLICATION_JSON).content("{\"volunteerCode\":\"teste-2\"}")).andExpect(status().isCreated());
  mvc.perform(post("/api/shifts/1/enrollments").contentType(MediaType.APPLICATION_JSON).content("{\"volunteerCode\":\"teste-3\"}")).andExpect(status().isConflict());
  mvc.perform(get("/api/shifts/1")).andExpect(status().isOk()).andExpect(jsonPath("$.remaining").value(0));
 }
}
