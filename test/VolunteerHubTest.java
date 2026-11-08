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

    public static void main(String[] args) throws InterruptedException {
        capacityAndUniqueness();
        invalidInputs();
        concurrentEnrollment();
        transferIsAtomicAndChecksCapacity();
        concurrentTransfersRespectCapacity();
        System.out.println("PASS: capacity, uniqueness, cancellation, transfers, concurrency");
    }
}
