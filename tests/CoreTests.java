import com.yamone.arcade2.core.GameId;
import com.yamone.arcade2.core.OrbitEngine;
import com.yamone.arcade2.core.ColorBreakEngine;
import com.yamone.arcade2.core.TwinTapEngine;
import com.yamone.arcade2.core.LineSurfEngine;
import com.yamone.arcade2.core.PocketPulseEngine;
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
        twinTapTests();
        lineSurfTests();
        pocketPulseTests();
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
        check(GameId.ORBIT_SNAP.ready && GameId.COLOR_BREAK.ready && GameId.TWIN_TAP.ready && GameId.LINE_SURF.ready
            && GameId.POCKET_PULSE.ready && !GameId.STACK_SLICE.ready, "only implemented games unlocked");
    }
    private static void twinHit(TwinTapEngine e) {
        double wait = e.targetOffset();
        if (wait > 0) e.advance(wait);
        int mask = e.noteMask();
        if ((mask & 1) != 0) e.tapLane(0);
        if ((mask & 2) != 0) e.tapLane(1);
    }
    private static void twinTapTests() {
        TwinTapEngine e = new TwinTapEngine(77);
        e.advance(5); e.tapLane(-1); e.tapLane(2);
        check(e.state() == TwinTapEngine.State.READY && e.elapsed() == 0, "twin clock waits for explicit start");
        e.start(); e.tapLane(e.noteMask() == 1 ? 0 : 1);
        check(e.score() == 0 && e.lives() == 5 && e.tappedMask() == 0, "twin very early tap is ignored");
        twinHit(e);
        check(e.score() == 150 && e.hits() == 1 && e.perfects() == 1 && e.combo() == 1, "twin exact single tap awards perfect");
        int score = e.score(), serial = e.noteSerial();
        e.tapLane(0); e.tapLane(1);
        check(e.score() == score && e.noteSerial() == serial, "twin taps outside window cannot duplicate score");

        TwinTapEngine wrong = new TwinTapEngine(7); wrong.start(); wrong.advance(wrong.targetOffset());
        int required = wrong.noteMask(); wrong.tapLane(required == 1 ? 1 : 0);
        check(wrong.lives() == 4 && wrong.score() == 0 && wrong.combo() == 0 && wrong.feedback() == TwinTapEngine.Feedback.MISS, "twin wrong lane costs one life");

        TwinTapEngine chord = new TwinTapEngine(77); chord.start();
        int watchdog = 30;
        while (chord.noteMask() != 3 && watchdog-- > 0) twinHit(chord);
        check(watchdog > 0 && chord.noteMask() == 3, "twin deterministic sequence includes double notes");
        int beforeScore = chord.score(), beforeHits = chord.hits(), beforeCombo = chord.combo();
        chord.advance(Math.max(0, chord.targetOffset() - .1)); chord.tapLane(0); chord.tapLane(0);
        check(chord.tappedMask() == 1 && chord.hits() == beforeHits, "twin chord waits for distinct second pointer");
        chord.advance(.15); chord.tapLane(1);
        int expectedChord = 200 + Math.min(100, beforeCombo * 10);
        check(chord.score() == beforeScore + expectedChord && chord.hits() == beforeHits + 1
            && chord.simultaneousHits() == 1 && chord.feedback() == TwinTapEngine.Feedback.HIT, "twin split multi-touch chord scores once within window");

        TwinTapEngine partial = new TwinTapEngine(5); partial.start();
        while (partial.noteMask() != 3) twinHit(partial);
        partial.advance(partial.targetOffset()); partial.tapLane(0);
        double at = partial.elapsed(), offset = partial.targetOffset(); int tapped = partial.tappedMask();
        partial.pause(); partial.advance(99); partial.tapLane(1);
        check(partial.elapsed() == at && partial.targetOffset() == offset && partial.tappedMask() == tapped, "twin pause freezes partial chord and input");
        partial.resume(); partial.tapLane(1);
        check(partial.hits() > 0 && partial.lives() == 5, "twin resume can complete pending chord");

        TwinTapEngine late = new TwinTapEngine(3); late.start();
        late.advance(late.targetOffset() + TwinTapEngine.HIT_WINDOW);
        check(late.lives() == 4 && late.feedback() == TwinTapEngine.Feedback.MISS && late.noteSerial() == 1, "twin late note misses exactly at deadline");
        TwinTapEngine ready = new TwinTapEngine(9); ready.pause(); ready.resume(); ready.advance(9);
        check(ready.state() == TwinTapEngine.State.READY && ready.elapsed() == 0, "twin pause before start preserves ready state");

        TwinTapEngine failed = new TwinTapEngine(11); failed.start(); failed.advance(99);
        check(failed.state() == TwinTapEngine.State.FINISHED && failed.lives() == 0 && failed.score() == 0, "twin five unattended notes finish round without passive score");
        at = failed.elapsed(); failed.start(); failed.tapLane(0); failed.advance(100);
        check(failed.elapsed() == at && failed.score() == 0 && failed.state() == TwinTapEngine.State.FINISHED, "twin finished state rejects input and time");

        TwinTapEngine clock = new TwinTapEngine(13); clock.start();
        while (clock.state() != TwinTapEngine.State.FINISHED) {
            if (clock.targetOffset() >= clock.remaining()) clock.advance(clock.remaining());
            else twinHit(clock);
        }
        check(clock.elapsed() == 60 && clock.lives() == 5 && clock.hits() > 50 && clock.simultaneousHits() > 0, "twin perfect play reaches exact 60 seconds with chords");
        check(clock.travelTime() < 1.6 && clock.travelTime() >= .95, "twin notes accelerate within readable bound");

        TwinTapEngine a = new TwinTapEngine(123), b = new TwinTapEngine(123), large = new TwinTapEngine(123);
        a.start(); b.start(); large.start();
        for (int i = 0; i < 600; i++) a.advance(1.0 / 60);
        for (int i = 0; i < 1200; i++) b.advance(1.0 / 120);
        large.advance(10);
        check(a.state() == b.state() && a.lives() == b.lives() && a.feedbackId() == b.feedbackId()
            && Math.abs(a.elapsed() - b.elapsed()) < 1e-8, "twin 60Hz and 120Hz deadline consistency");
        check(large.state() == a.state() && large.lives() == a.lives() && large.feedbackId() == a.feedbackId()
            && Math.abs(large.elapsed() - a.elapsed()) < 1e-8, "twin delayed frame cannot skip note deadlines");
        at = ready.elapsed(); ready.start(); ready.advance(Double.NaN); ready.advance(Double.POSITIVE_INFINITY); ready.advance(-1); ready.advance(0);
        check(ready.elapsed() == at, "twin invalid time deltas ignored");
    }
    private static void surfClearNext(LineSurfEngine e) {
        int serial = e.hazardSerial();
        e.press();
        int watchdog = 200000;
        while (e.state() == LineSurfEngine.State.RUNNING && e.hazardDistance() > 60 && watchdog-- > 0) e.advance(.002);
        if (watchdog <= 0) throw new AssertionError("Could not approach surf hazard");
        e.release();
        while (e.state() == LineSurfEngine.State.RUNNING && e.hazardSerial() == serial && watchdog-- > 0) e.advance(.002);
        if (watchdog <= 0) throw new AssertionError("Could not resolve surf hazard");
    }
    private static void lineSurfTests() {
        LineSurfEngine e = new LineSurfEngine(77);
        e.advance(8);
        check(e.state() == LineSurfEngine.State.READY && e.elapsed() == 0 && e.distance() == 0 && e.score() == 0, "surf waits for first hold without passive score");
        e.press();
        check(e.state() == LineSurfEngine.State.RUNNING && e.held(), "surf press starts line ride");
        e.release();
        check(e.airborne() && !e.held() && e.jumps() == 1 && e.velocity() > 0, "surf release launches jump");
        e.release();
        check(e.jumps() == 1, "surf duplicate release cannot double jump");

        LineSurfEngine cancel = new LineSurfEngine(2); cancel.press(); cancel.cancelInput(); cancel.release();
        check(!cancel.held() && !cancel.airborne() && cancel.jumps() == 0, "surf canceled touch never jumps");
        cancel.press(); cancel.advance(.25); double at = cancel.elapsed(), distance = cancel.distance();
        cancel.pause(); cancel.advance(30); cancel.release();
        check(cancel.state() == LineSurfEngine.State.PAUSED && cancel.elapsed() == at && cancel.distance() == distance
            && !cancel.held() && cancel.jumps() == 0, "surf pause freezes motion and cancels held input");
        cancel.resume(); cancel.advance(.1);
        check(cancel.elapsed() > at && !cancel.held(), "surf resume requires fresh hold before jump");

        LineSurfEngine safe = new LineSurfEngine(19); safe.press();
        boolean sawGap = false, sawObstacle = false;
        for (int i = 0; i < 8 && (!sawGap || !sawObstacle); i++) {
            sawGap |= safe.hazard() == LineSurfEngine.Hazard.GAP;
            sawObstacle |= safe.hazard() == LineSurfEngine.Hazard.OBSTACLE;
            int before = safe.cleared(); surfClearNext(safe);
            check(safe.cleared() == before + 1 && safe.lives() == 3, "surf timed jump clears one hazard");
        }
        check(sawGap && sawObstacle && safe.bestCombo() == safe.cleared(), "surf deterministic course contains gaps and obstacles");
        check(safe.score() == safe.distanceMeters() + safe.cleared() * 100, "surf score combines distance and cleared hazards");
        check(safe.hazardWidth() >= 34 && safe.hazardWidth() <= 90 && safe.hazardDistance() > 200, "surf next hazard keeps bounded size and reaction distance");

        LineSurfEngine crash = new LineSurfEngine(5); crash.press();
        int watchdog = 200000;
        while (crash.lives() == 3 && watchdog-- > 0) crash.advance(.002);
        check(watchdog > 0 && crash.lives() == 2 && crash.crashes() == 1 && crash.cleared() == 0
            && crash.feedback() == LineSurfEngine.Feedback.CRASH, "surf riding into hazard costs one life and no clear bonus");
        crash.advance(60);
        check(crash.state() == LineSurfEngine.State.FINISHED && crash.lives() == 0 && crash.crashes() == 3, "surf three crashes end round");
        at = crash.elapsed(); int score = crash.score(); crash.press(); crash.release(); crash.advance(100);
        check(crash.elapsed() == at && crash.score() == score && crash.state() == LineSurfEngine.State.FINISHED, "surf finished state rejects input and time");

        LineSurfEngine ready = new LineSurfEngine(9); ready.pause(); ready.resume(); ready.advance(9);
        check(ready.state() == LineSurfEngine.State.READY && ready.elapsed() == 0, "surf pause before start preserves ready state");
        LineSurfEngine clock = new LineSurfEngine(13); clock.press();
        while (clock.state() != LineSurfEngine.State.FINISHED) {
            if (clock.hazardDistance() > 60) clock.advance(Math.min(.01, clock.remaining()));
            else surfClearNext(clock);
        }
        check(clock.elapsed() == 60 && clock.lives() == 3 && clock.cleared() > 20 && clock.distanceMeters() > 900, "surf perfect play reaches exact 60 seconds");
        check(clock.speed() > 155 && clock.speed() <= 236, "surf speed rises within configured bound");

        LineSurfEngine a = new LineSurfEngine(123), b = new LineSurfEngine(123), large = new LineSurfEngine(123);
        a.press(); b.press(); large.press();
        for (int i = 0; i < 900; i++) a.advance(1.0 / 60);
        for (int i = 0; i < 1800; i++) b.advance(1.0 / 120);
        large.advance(15);
        check(a.state() == b.state() && a.lives() == b.lives() && a.crashes() == b.crashes()
            && Math.abs(a.elapsed() - b.elapsed()) < .01 && Math.abs(a.distance() - b.distance()) < 1, "surf 60Hz and 120Hz collision consistency");
        check(large.state() == a.state() && large.lives() == a.lives() && large.crashes() == a.crashes()
            && Math.abs(large.elapsed() - a.elapsed()) < .01 && Math.abs(large.distance() - a.distance()) < 1, "surf delayed frame cannot skip hazards");
        at = ready.elapsed(); ready.press(); ready.advance(Double.NaN); ready.advance(Double.POSITIVE_INFINITY); ready.advance(-1); ready.advance(0);
        check(ready.elapsed() == at, "surf invalid time deltas ignored");
    }
    private static void pulseToRadius(PocketPulseEngine e, double desired) {
        int watchdog = 400000;
        while (e.state() == PocketPulseEngine.State.RUNNING && !e.waitingNext()
            && e.radius() < desired && watchdog-- > 0) e.advance(.0005);
        if (watchdog <= 0) throw new AssertionError("Could not reach pulse radius");
    }
    private static void pulsePerfect(PocketPulseEngine e) {
        pulseToRadius(e, e.targetRadius());
        if (e.state() == PocketPulseEngine.State.RUNNING && !e.waitingNext()) e.tap();
    }
    private static void pocketPulseTests() {
        PocketPulseEngine e = new PocketPulseEngine(77), same = new PocketPulseEngine(77);
        e.advance(8);
        check(e.state() == PocketPulseEngine.State.READY && e.elapsed() == 0 && e.score() == 0, "pulse waits for first tap without passive score");
        check(e.targetRadius() == same.targetRadius() && e.targetRadius() >= 72 && e.targetRadius() < 130, "pulse seed fixes target inside visible range");
        e.tap();
        check(e.state() == PocketPulseEngine.State.RUNNING && e.score() == 0 && e.lives() == 4, "pulse first tap starts without judging");
        pulsePerfect(e);
        check(e.score() == 200 && e.hits() == 1 && e.perfects() == 1 && e.combo() == 1
            && e.feedback() == PocketPulseEngine.Feedback.PERFECT, "pulse matched rings award perfect");
        int score = e.score(), feedback = e.feedbackId(); e.tap(); e.tap();
        check(e.score() == score && e.feedbackId() == feedback && e.waitingNext(), "pulse recovery rejects duplicate taps");

        e.advance(PocketPulseEngine.RECOVERY);
        pulseToRadius(e, e.targetRadius() - 8); e.tap();
        check(e.score() == 360 && e.greats() == 1 && e.combo() == 2 && e.feedback() == PocketPulseEngine.Feedback.GREAT, "pulse medium error awards great plus combo");
        e.advance(PocketPulseEngine.RECOVERY);
        pulseToRadius(e, e.targetRadius() - 14); e.tap();
        check(e.score() == 480 && e.goods() == 1 && e.combo() == 3 && e.bestCombo() == 3, "pulse wider valid error awards good plus combo");
        e.advance(PocketPulseEngine.RECOVERY); e.tap();
        check(e.lives() == 3 && e.score() == 480 && e.combo() == 0 && e.misses() == 1, "pulse early miss costs life and resets combo");

        PocketPulseEngine late = new PocketPulseEngine(4); late.tap();
        while (late.feedbackId() == 0) late.advance(.01);
        check(late.feedback() == PocketPulseEngine.Feedback.MISS && late.lives() == 3 && late.misses() == 1, "pulse passing tolerance auto-misses once");
        int serial = late.pulseSerial(); late.advance(PocketPulseEngine.RECOVERY);
        check(late.pulseSerial() == serial + 1 && late.radius() >= PocketPulseEngine.MIN_RADIUS
            && !late.waitingNext(), "pulse recovery creates exactly one new seeded target");

        PocketPulseEngine paused = new PocketPulseEngine(8); paused.tap(); pulsePerfect(paused); paused.advance(.08);
        double at = paused.elapsed(), radius = paused.radius(), recovery = paused.recoveryRemaining(); int lives = paused.lives();
        paused.pause(); paused.advance(30); paused.tap();
        check(paused.state() == PocketPulseEngine.State.PAUSED && paused.elapsed() == at && paused.radius() == radius
            && paused.recoveryRemaining() == recovery && paused.lives() == lives, "pulse pause freezes wave/recovery and input");
        paused.resume(); paused.advance(recovery);
        check(paused.state() == PocketPulseEngine.State.RUNNING && paused.pulseSerial() == 1, "pulse resume continues pending recovery");
        PocketPulseEngine ready = new PocketPulseEngine(9); ready.pause(); ready.resume(); ready.advance(9);
        check(ready.state() == PocketPulseEngine.State.READY && ready.elapsed() == 0, "pulse pause before start preserves ready state");

        PocketPulseEngine failed = new PocketPulseEngine(11); failed.tap(); failed.advance(60);
        check(failed.state() == PocketPulseEngine.State.FINISHED && failed.lives() == 0 && failed.misses() == 4 && failed.score() == 0, "pulse four unattended waves finish without score");
        at = failed.elapsed(); failed.tap(); failed.advance(100); failed.pause(); failed.resume();
        check(failed.elapsed() == at && failed.score() == 0 && failed.state() == PocketPulseEngine.State.FINISHED, "pulse finished state rejects input and time");

        PocketPulseEngine clock = new PocketPulseEngine(13); clock.tap();
        while (clock.state() != PocketPulseEngine.State.FINISHED) {
            if (clock.waitingNext()) clock.advance(Math.min(clock.recoveryRemaining(), clock.remaining()));
            else pulsePerfect(clock);
        }
        check(clock.elapsed() == 60 && clock.lives() == 4 && clock.hits() > 40 && clock.perfects() == clock.hits(), "pulse perfect play reaches exact 60 seconds");
        check(clock.bestCombo() == clock.hits() && clock.score() > clock.hits() * 200, "pulse sustained accuracy builds capped combo bonus");
        check(clock.speed() > 72 && clock.speed() <= 126, "pulse speed rises within configured bound");

        PocketPulseEngine a = new PocketPulseEngine(123), b = new PocketPulseEngine(123), large = new PocketPulseEngine(123);
        a.tap(); b.tap(); large.tap();
        for (int i = 0; i < 600; i++) a.advance(1.0 / 60);
        for (int i = 0; i < 1200; i++) b.advance(1.0 / 120);
        large.advance(10);
        check(a.state() == b.state() && a.lives() == b.lives() && a.misses() == b.misses()
            && Math.abs(a.elapsed() - b.elapsed()) < .01, "pulse 60Hz and 120Hz deadline consistency");
        check(large.state() == a.state() && large.lives() == a.lives() && large.misses() == a.misses()
            && Math.abs(large.elapsed() - a.elapsed()) < .01, "pulse delayed frame cannot skip expired waves");
        at = ready.elapsed(); ready.tap(); ready.advance(Double.NaN); ready.advance(Double.POSITIVE_INFINITY); ready.advance(-1); ready.advance(0);
        check(ready.elapsed() == at, "pulse invalid time deltas ignored");
    }
}
