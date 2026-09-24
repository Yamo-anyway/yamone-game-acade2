import com.yamone.arcade2.core.GameId;
import com.yamone.arcade2.core.OrbitEngine;
import com.yamone.arcade2.core.ColorBreakEngine;
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
        colorBreakTests();
        System.out.println("All " + passed + " core checks passed.");
    }
    private static int matchingLane(ColorBreakEngine e) { return e.laneColor(0) == e.targetColor() ? 0 : 1; }
    private static void colorHit(ColorBreakEngine e) {
        e.tapLane(matchingLane(e));
        e.advance(e.wallDuration() * (1 - e.wallProgress()));
    }
    private static void colorBreakTests() {
        ColorBreakEngine e = new ColorBreakEngine(77);
        e.advance(10); e.tapLane(-1); e.tapLane(2);
        check(e.state() == ColorBreakEngine.State.READY && e.elapsed() == 0, "color waits for valid first lane tap");
        check(e.laneColor(0) != e.laneColor(1) && (e.laneColor(0) == e.targetColor() || e.laneColor(1) == e.targetColor()), "color wall has exactly one matching lane");
        e.tapLane(1 - matchingLane(e)); e.advance(e.wallDuration() - .001);
        check(e.score() == 0 && e.lives() == 3, "color only judges at crossing");
        e.tapLane(matchingLane(e)); e.advance(.001);
        check(e.score() == 100 && e.combo() == 1 && e.passed() == 1, "color last moment lane change scores once");
        e.tapLane(0); e.tapLane(1); e.advance(.1);
        check(e.score() == 100 && e.feedbackId() == 1, "color repeated taps and recovery cannot duplicate score");
        e.advance(ColorBreakEngine.RECOVERY - .1); colorHit(e);
        check(e.score() == 210 && e.combo() == 2 && e.bestCombo() == 2, "color consecutive hit earns combo bonus");
        e.advance(ColorBreakEngine.RECOVERY); e.tapLane(1 - matchingLane(e)); e.advance(e.wallDuration());
        check(e.combo() == 0 && e.bestCombo() == 2 && e.score() == 210 && e.lives() == 2, "color mismatch resets combo but keeps best and score");
        double at = e.elapsed(), recovery = e.recoveryRemaining(); int lane = e.lane();
        e.pause(); e.pause(); e.advance(99); e.tapLane(1 - lane);
        check(e.elapsed() == at && e.recoveryRemaining() == recovery && e.lane() == lane, "color pause freezes recovery timer and input");
        e.resume(); e.advance(ColorBreakEngine.RECOVERY); colorHit(e);
        check(e.combo() == 1 && e.score() == 310, "color resume and post-miss hit restart combo");
        ColorBreakEngine ready = new ColorBreakEngine(1); ready.pause(); ready.resume(); ready.advance(9);
        check(ready.state() == ColorBreakEngine.State.READY && ready.elapsed() == 0, "color pause before start preserves ready state");
        ColorBreakEngine failed = new ColorBreakEngine(2);
        for (int i = 0; i < 3; i++) {
            failed.tapLane(1 - matchingLane(failed)); failed.advance(failed.wallDuration());
            if (i < 2) failed.advance(ColorBreakEngine.RECOVERY);
        }
        check(failed.state() == ColorBreakEngine.State.FINISHED && failed.lives() == 0 && failed.score() == 0, "color three mismatches finish round");
        at = failed.elapsed(); failed.tapLane(matchingLane(failed)); failed.advance(1000); failed.pause(); failed.resume();
        check(failed.elapsed() == at && failed.score() == 0 && failed.state() == ColorBreakEngine.State.FINISHED, "color finished state rejects input and time");
        ColorBreakEngine clock = new ColorBreakEngine(4);
        int expectedScore = 0;
        while (clock.state() != ColorBreakEngine.State.FINISHED) {
            int before = clock.passed(); colorHit(clock);
            if (clock.passed() > before) expectedScore += 100 + Math.min(10, clock.combo() - 1) * 10;
            clock.advance(ColorBreakEngine.RECOVERY);
        }
        check(clock.elapsed() == 60 && clock.lives() == 3 && clock.combo() > 11 && clock.score() == expectedScore, "color exact 60 seconds and capped combo bonus");
        check(clock.wallDuration() < 2.4 && clock.wallDuration() >= 1.1, "color walls accelerate within reaction-time bound");
        ColorBreakEngine a = new ColorBreakEngine(123), b = new ColorBreakEngine(123);
        a.tapLane(0); b.tapLane(0);
        for (int i = 0; i < 720; i++) a.advance(1.0 / 60);
        for (int i = 0; i < 1440; i++) b.advance(1.0 / 120);
        check(a.score() == b.score() && a.lives() == b.lives() && a.feedbackId() == b.feedbackId()
            && Math.abs(a.elapsed() - b.elapsed()) < 1e-8 && Math.abs(a.wallProgress() - b.wallProgress()) < 1e-8, "color 60Hz and 120Hz event consistency");
        ColorBreakEngine large = new ColorBreakEngine(123); large.tapLane(0); large.advance(12);
        check(large.score() == a.score() && large.lives() == a.lives() && large.feedbackId() == a.feedbackId(), "color delayed frame cannot skip wall collision");
        at = ready.elapsed(); ready.tapLane(0); ready.advance(Double.NaN); ready.advance(Double.POSITIVE_INFINITY); ready.advance(-1); ready.advance(0);
        check(ready.elapsed() == at, "color invalid time deltas ignored");
        check(GameId.ORBIT_SNAP.ready && GameId.COLOR_BREAK.ready && !GameId.TWIN_TAP.ready, "only implemented games unlocked");
    }
}
