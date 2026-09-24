import com.yamone.arcade2.core.GameId;
import com.yamone.arcade2.core.OrbitEngine;
import com.yamone.arcade2.data.RankingGateway;

public final class CoreTests {
    private static int passed;
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        passed++; System.out.println("PASS " + name);
    }
    private static void hit(OrbitEngine e) {
        e.press();
        int watchdog = 5000;
        while (e.state() != OrbitEngine.State.FINISHED && OrbitEngine.angularDistance(e.angle(), e.target()) > 2 && watchdog-- > 0) e.advance(.002);
        if (watchdog <= 0) throw new AssertionError("Could not reach target");
        e.release();
    }
    public static void main(String[] args) {
        OrbitEngine e = new OrbitEngine(77);
        e.advance(10);
        check(e.elapsed() == 0 && e.state() == OrbitEngine.State.READY, "clock waits for first touch");
        hit(e);
        check(e.score() == 150 && e.jumps() == 1 && e.perfects() == 1, "accurate release awards perfect");
        e.release(); e.release();
        check(e.score() == 150 && e.lives() == 3, "duplicate releases cannot award score");
        e.advance(.4); e.press(); e.cancelInput(); e.release();
        check(e.jumps() == 1 && e.lives() == 3, "touch cancellation never fires jump");
        double at = e.elapsed(); e.pause(); e.advance(45); e.press(); e.release();
        check(e.elapsed() == at && e.state() == OrbitEngine.State.PAUSED && !e.held(), "background pause freezes timer and input");
        e.resume(); e.advance(.1);
        check(e.elapsed() > at && !e.held(), "resume requires fresh touch");
        OrbitEngine idle = new OrbitEngine(1); idle.press(); idle.cancelInput(); idle.advance(60);
        check(idle.state() == OrbitEngine.State.FINISHED && idle.lives() == 0 && idle.score() == 0, "idle camping ends with zero score");
        int score = e.score(); e.advance(1000); e.press(); e.release(); e.advance(10);
        check(e.state() == OrbitEngine.State.FINISHED && e.score() == score, "finished games reject input");
        OrbitEngine clock = new OrbitEngine(9);
        while (clock.state() != OrbitEngine.State.FINISHED) {
            hit(clock); clock.advance(.33);
        }
        check(clock.elapsed() == 60 && clock.lives() == 3 && clock.score() > 0, "successful play stops at exactly 60 seconds");
        check(OrbitEngine.angularDistance(359, 1) == 2 && OrbitEngine.angularDistance(-1, 361) == 2, "angle wrap handles gate crossing");
        OrbitEngine a = new OrbitEngine(123), b = new OrbitEngine(123);
        a.press(); b.press(); for (int i = 0; i < 60; i++) a.advance(1.0 / 60); for (int i = 0; i < 120; i++) b.advance(1.0 / 120);
        check(Math.abs(a.angle() - b.angle()) < .02 && Math.abs(a.elapsed() - b.elapsed()) < .0001, "frame rate independent motion");
        at = a.elapsed(); a.advance(Double.NaN); a.advance(Double.POSITIVE_INFINITY); a.advance(-1);
        check(a.elapsed() == at, "invalid deltas ignored");
        RankingGateway disabled = new RankingGateway.Disabled();
        check(disabled.status() == RankingGateway.Status.NOT_CONNECTED && disabled.top(GameId.ORBIT_SNAP).isEmpty(), "unconnected ranking shows no fake entries");
        check(disabled.submit(new RankingGateway.ScoreSubmission("run", "local", "player", GameId.ORBIT_SNAP, 1, 100, 77)) == RankingGateway.Status.NOT_CONNECTED, "server submission disabled");
        check(GameId.values().length == 6, "catalog contains all six approved concepts");
        System.out.println("All " + passed + " core checks passed.");
    }
}
