import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Primeiro dominio local para organizar vagas de acoes comunitarias.
 *
 * Sem dados pessoais reais, autenticacao ou persistencia nesta versao.
 * Todas as operacoes de vagas sao sincronizadas na instancia, impedindo
 * excesso de inscricoes por chamadas concorrentes no mesmo processo.
 */
public final class VolunteerHub {
    public record ShiftView(String id, String action, int capacity, int enrolled, int available) {}

    private static final class Shift {
        final String id;
        final String action;
        final int capacity;
        final Set<String> enrolled = new LinkedHashSet<>();

        Shift(String id, String action, int capacity) {
            this.id = id;
            this.action = action;
            this.capacity = capacity;
        }

        ShiftView snapshot() {
            return new ShiftView(id, action, capacity, enrolled.size(), capacity - enrolled.size());
        }
    }

    private final Map<String, Shift> shifts = new LinkedHashMap<>();

    private static String token(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 80 || !value.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(field + " deve ter 1 a 80 letras, digitos ou _ -");
        }
        return value;
    }

    private static String description(String value) {
        if (value == null || value.isBlank() || value.length() > 140) {
            throw new IllegalArgumentException("Nome da acao invalido");
        }
        return value.strip();
    }

    public synchronized ShiftView addShift(String id, String action, int capacity) {
        id = token(id, "turno");
        if (capacity < 1 || capacity > 10000) {
            throw new IllegalArgumentException("Capacidade deve ser de 1 a 10000");
        }
        if (shifts.containsKey(id)) {
            throw new IllegalArgumentException("Turno duplicado: " + id);
        }
        Shift shift = new Shift(id, description(action), capacity);
        shifts.put(id, shift);
        return shift.snapshot();
    }

    private Shift requireShift(String id) {
        Shift shift = shifts.get(token(id, "turno"));
        if (shift == null) throw new IllegalArgumentException("Turno nao encontrado: " + id);
        return shift;
    }

    public synchronized ShiftView enroll(String shiftId, String volunteerId) {
        Shift shift = requireShift(shiftId);
        volunteerId = token(volunteerId, "voluntario");
        if (shift.enrolled.contains(volunteerId)) {
            throw new IllegalStateException("Voluntario ja inscrito no turno");
        }
        if (shift.enrolled.size() == shift.capacity) {
            throw new IllegalStateException("Turno sem vagas");
        }
        shift.enrolled.add(volunteerId);
        return shift.snapshot();
    }

    public synchronized ShiftView cancel(String shiftId, String volunteerId) {
        Shift shift = requireShift(shiftId);
        volunteerId = token(volunteerId, "voluntario");
        if (!shift.enrolled.remove(volunteerId)) {
            throw new IllegalStateException("Inscricao ativa nao encontrada");
        }
        return shift.snapshot();
    }

    /**
     * Transfer an active enrollment atomically between shifts. All validation
     * happens before either set changes, so a full target leaves the source
     * enrollment untouched.
     */
    public synchronized ShiftView transfer(
            String sourceId, String destinationId, String volunteerId) {
        Shift source = requireShift(sourceId);
        Shift destination = requireShift(destinationId);
        volunteerId = token(volunteerId, "voluntario");

        if (source == destination) {
            throw new IllegalArgumentException("Turnos de origem e destino devem ser diferentes");
        }
        if (!source.enrolled.contains(volunteerId)) {
            throw new IllegalStateException("Voluntario nao inscrito na origem");
        }
        if (destination.enrolled.contains(volunteerId)) {
            throw new IllegalStateException("Voluntario ja inscrito no destino");
        }
        if (destination.enrolled.size() >= destination.capacity) {
            throw new IllegalStateException("Turno de destino sem vagas");
        }

        source.enrolled.remove(volunteerId);
        destination.enrolled.add(volunteerId);
        return destination.snapshot();
    }

    /**
     * Assign a volunteer to the first available shift in caller priority order.
     * Check every candidate and existing enrollment before changing any shift,
     * so invalid input and full shifts cannot cause partial enrollment.
     */
    public synchronized ShiftView enrollFirstAvailable(
            List<String> candidateShiftIds, String volunteerId) {
        if (candidateShiftIds == null || candidateShiftIds.isEmpty() || candidateShiftIds.size() > 100) {
            throw new IllegalArgumentException("Informe de 1 a 100 turnos candidatos");
        }
        volunteerId = token(volunteerId, "voluntario");

        Set<String> uniqueIds = new LinkedHashSet<>();
        List<Shift> candidates = new ArrayList<>();
        for (String id : candidateShiftIds) {
            Shift shift = requireShift(id);
            if (!uniqueIds.add(shift.id)) {
                throw new IllegalArgumentException("Turno candidato repetido: " + shift.id);
            }
            candidates.add(shift);
        }

        for (Shift shift : candidates) {
            if (shift.enrolled.contains(volunteerId)) {
                throw new IllegalStateException("Voluntario ja inscrito em turno candidato");
            }
        }

        for (Shift shift : candidates) {
            if (shift.enrolled.size() < shift.capacity) {
                shift.enrolled.add(volunteerId);
                return shift.snapshot();
            }
        }
        throw new IllegalStateException("Nenhum turno candidato com vagas livres");
    }

    public synchronized ShiftView getShift(String shiftId) {
        return requireShift(shiftId).snapshot();
    }

    public synchronized List<ShiftView> listShifts() {
        List<ShiftView> result = new ArrayList<>();
        for (Shift shift : shifts.values()) result.add(shift.snapshot());
        return List.copyOf(result);
    }

    public static void main(String[] args) {
        if (args.length != 0) {
            System.err.println("Uso: java -cp out VolunteerHub");
            System.exit(2);
        }
        VolunteerHub hub = new VolunteerHub();
        hub.addShift("manha", "Mutirao de arrecadacao", 2);
        hub.enroll("manha", "pessoa-ficticia-1");
        hub.enroll("manha", "pessoa-ficticia-2");
        for (ShiftView shift : hub.listShifts()) {
            System.out.printf("%s - %s: %d inscritos, %d vagas livres%n",
                shift.id(), shift.action(), shift.enrolled(), shift.available());
        }
    }
}
