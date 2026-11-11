import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class VolunteerHubTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void fails(Class<? extends Exception> type, Runnable action) {
        try {
            action.run();
            throw new AssertionError("Era esperada uma excecao: " + type.getSimpleName());
        } catch (Exception error) {
            if (!type.isInstance(error)) throw new AssertionError("Excecao incorreta", error);
        }
    }

    private static void capacityAndUniqueness() {
        VolunteerHub hub = new VolunteerHub();
        hub.addShift("sabado", "Acao comunitaria", 2);
        hub.enroll("sabado", "a");
        fails(IllegalStateException.class, () -> hub.enroll("sabado", "a"));
        hub.enroll("sabado", "b");
        fails(IllegalStateException.class, () -> hub.enroll("sabado", "c"));
        check(hub.getShift("sabado").available() == 0, "Turno nao deveria ter vagas");
        hub.cancel("sabado", "a");
        hub.enroll("sabado", "c");
        check(hub.getShift("sabado").enrolled() == 2, "Cancelar deve liberar vaga");
    }

    private static void invalidInputs() {
        VolunteerHub hub = new VolunteerHub();
        fails(IllegalArgumentException.class, () -> hub.addShift("sabado", "Acao", 0));
        fails(IllegalArgumentException.class, () -> hub.addShift("turno 1", "Acao", 2));
        hub.addShift("t1", "Acao", 1);
        fails(IllegalArgumentException.class, () -> hub.addShift("t1", "Acao", 1));
        fails(IllegalArgumentException.class, () -> hub.enroll("t1", "nome completo"));
        fails(IllegalArgumentException.class, () -> hub.cancel("inexistente", "a"));
        fails(IllegalStateException.class, () -> hub.cancel("t1", "a"));
        check(hub.getShift("t1").available() == 1, "Erro nao pode consumir vaga");
    }

    private static void concurrentEnrollment() throws InterruptedException {
        final VolunteerHub hub = new VolunteerHub();
        hub.addShift("dia", "Mutirao", 2);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Thread> threads = new ArrayList<>();
        final AtomicInteger accepted = new AtomicInteger();
        for (int i = 0; i < 30; i++) {
            final String person = "p" + i;
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    hub.enroll("dia", person);
                    accepted.incrementAndGet();
                } catch (IllegalStateException ignored) {
                    // Turno ficou cheio, comportamento esperado.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(thread);
            thread.start();
        }
        start.countDown();
        for (Thread thread : threads) thread.join();
        check(accepted.get() == 2, "Apenas duas inscricoes podem ser aceitas");
        check(hub.getShift("dia").available() == 0, "Vagas nao podem ficar negativas");
        check(hub.listShifts().size() == 1, "Snapshot incorreto");
    }

    private static void transferIsAtomicAndChecksCapacity() {
        VolunteerHub hub = new VolunteerHub();
        hub.addShift("origem", "Acao de manha", 3);
        hub.addShift("destino", "Acao de tarde", 1);
        hub.enroll("origem", "ana");
        hub.enroll("origem", "bia");
        hub.enroll("destino", "caio");

        fails(IllegalStateException.class, () -> hub.transfer("origem", "destino", "ana"));
        check(hub.getShift("origem").enrolled() == 2, "Falha nao pode remover da origem");
        check(hub.getShift("destino").enrolled() == 1, "Falha nao pode alterar destino");

        hub.cancel("destino", "caio");
        VolunteerHub.ShiftView result = hub.transfer("origem", "destino", "ana");
        check(result.enrolled() == 1 && result.available() == 0, "Transferencia deve ocupar destino");
        check(hub.getShift("origem").enrolled() == 1, "Transferencia deve liberar origem");

        fails(IllegalArgumentException.class, () -> hub.transfer("origem", "origem", "bia"));
        fails(IllegalStateException.class, () -> hub.transfer("origem", "destino", "ausente"));
        check(hub.getShift("origem").enrolled() == 1, "Transferencia invalida nao altera vagas");

        hub.cancel("destino", "ana");
        hub.enroll("destino", "bia");
        fails(IllegalStateException.class, () -> hub.transfer("origem", "destino", "bia"));
        check(hub.getShift("origem").enrolled() == 1, "Duplicidade no destino preserva origem");
    }

    private static void concurrentTransfersRespectCapacity() throws InterruptedException {
        VolunteerHub hub = new VolunteerHub();
        hub.addShift("origem", "Acao A", 30);
        hub.addShift("destino", "Acao B", 1);

        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        AtomicInteger moved = new AtomicInteger();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();
        for (int index = 0; index < 20; index++) {
            String volunteer = "p" + index;
            hub.enroll("origem", volunteer);
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                    hub.transfer("origem", "destino", volunteer);
                    moved.incrementAndGet();
                } catch (IllegalStateException expectedFullTarget) {
                    // Only one of the twenty transfers can obtain the free slot.
                } catch (Throwable error) {
                    unexpected.compareAndSet(null, error);
                }
            });
            worker.start();
            threads.add(worker);
        }
        start.countDown();
        for (Thread worker : threads) worker.join();

        if (unexpected.get() != null) {
            throw new AssertionError("Erro inesperado em transferencia concorrente", unexpected.get());
        }
        check(moved.get() == 1, "Somente uma transferencia pode ocupar a ultima vaga");
        check(hub.getShift("destino").enrolled() == 1, "Destino nao pode exceder capacidade");
        check(hub.getShift("origem").enrolled() == 19, "Demais inscricoes devem permanecer na origem");
    }

    private static void firstAvailableAssignmentIsAtomic() {
        VolunteerHub hub = new VolunteerHub();
        hub.addShift("manha", "Turno inicial", 1);
        hub.addShift("tarde", "Turno alternativo", 2);
        hub.enroll("manha", "ocupado");

        VolunteerHub.ShiftView assigned =
            hub.enrollFirstAvailable(List.of("manha", "tarde"), "nova-pessoa");
        check(assigned.id().equals("tarde") && assigned.enrolled() == 1,
            "Deve respeitar a ordem e pular turnos lotados");

        fails(IllegalArgumentException.class,
            () -> hub.enrollFirstAvailable(List.of("manha", "inexistente"), "outro"));
        fails(IllegalArgumentException.class,
            () -> hub.enrollFirstAvailable(List.of("tarde", "tarde"), "outro"));
        fails(IllegalArgumentException.class,
            () -> hub.enrollFirstAvailable(List.of(), "outro"));
        fails(IllegalStateException.class,
            () -> hub.enrollFirstAvailable(List.of("manha", "tarde"), "nova-pessoa"));
        check(hub.getShift("tarde").enrolled() == 1,
            "Falha de validacao nao pode alterar inscricoes");

        hub.enrollFirstAvailable(List.of("manha", "tarde"), "segunda-pessoa");
        fails(IllegalStateException.class,
            () -> hub.enrollFirstAvailable(List.of("manha", "tarde"), "sem-vaga"));
        check(hub.getShift("tarde").available() == 0,
            "Nao pode ultrapassar capacidade");
    }

    private static void concurrentFirstAvailableAssignment() throws InterruptedException {
        VolunteerHub hub = new VolunteerHub();
        hub.addShift("um", "Acao A", 1);
        hub.addShift("dois", "Acao B", 2);
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        AtomicInteger accepted = new AtomicInteger();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();
        for (int i = 0; i < 20; i++) {
            String id = "p" + i;
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                    hub.enrollFirstAvailable(List.of("um", "dois"), id);
                    accepted.incrementAndGet();
                } catch (IllegalStateException noSlots) {
                    // Esperado apos as tres vagas serem ocupadas.
                } catch (Throwable error) {
                    unexpected.compareAndSet(null, error);
                }
            });
            worker.start();
            workers.add(worker);
        }
        start.countDown();
        for (Thread worker : workers) worker.join();
        if (unexpected.get() != null) {
            throw new AssertionError("Falha concorrente inesperada", unexpected.get());
        }
        check(accepted.get() == 3, "Devem ser aceitas exatamente tres inscricoes");
        check(hub.getShift("um").enrolled() == 1 && hub.getShift("dois").enrolled() == 2,
            "Cada turno deve preservar seu limite");
    }

    private static void volunteerShiftLookup() {
        VolunteerHub hub = new VolunteerHub();
        hub.addShift("manha", "Distribuicao de alimentos", 2);
        hub.addShift("tarde", "Triagem", 2);
        hub.addShift("noite", "Organizacao", 2);
        hub.enroll("manha", "vol-1");
        hub.enroll("tarde", "vol-1");

        List<VolunteerHub.ShiftView> snapshot = hub.shiftsForVolunteer("vol-1");
        check(snapshot.stream().map(VolunteerHub.ShiftView::id).toList()
                .equals(List.of("manha", "tarde")), "Deve listar turnos na ordem de cadastro");
        check(hub.shiftsForVolunteer("sem-inscricao").isEmpty(),
                "Voluntario sem inscricoes deve retornar lista vazia");
        fails(IllegalArgumentException.class, () -> hub.shiftsForVolunteer("nome invalido"));
        fails(UnsupportedOperationException.class, () -> snapshot.clear());

        hub.transfer("manha", "noite", "vol-1");
        check(snapshot.size() == 2 && snapshot.get(0).id().equals("manha"),
                "Snapshot antigo deve permanecer inalterado");
        check(hub.shiftsForVolunteer("vol-1").stream().map(VolunteerHub.ShiftView::id)
                .toList().equals(List.of("tarde", "noite")), "Transferencia deve atualizar consulta");
        hub.cancel("tarde", "vol-1");
        check(hub.shiftsForVolunteer("vol-1").stream().map(VolunteerHub.ShiftView::id)
                .toList().equals(List.of("noite")), "Cancelamento deve atualizar consulta");
    }

    public static void main(String[] args) throws InterruptedException {
        capacityAndUniqueness();
        invalidInputs();
        concurrentEnrollment();
        transferIsAtomicAndChecksCapacity();
        concurrentTransfersRespectCapacity();
        firstAvailableAssignmentIsAtomic();
        concurrentFirstAvailableAssignment();
        volunteerShiftLookup();
        System.out.println("PASS: capacity, uniqueness, cancellation, transfers, concurrency");
    }
}
