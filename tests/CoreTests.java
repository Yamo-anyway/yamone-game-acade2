import com.yamone.arcade2.core.GameId;
import com.yamone.arcade2.core.OrbitEngine;
import com.yamone.arcade2.core.ColorBreakEngine;
import com.yamone.arcade2.core.TwinTapEngine;
import com.yamone.arcade2.core.LineSurfEngine;
import com.yamone.arcade2.core.PocketPulseEngine;
import com.yamone.arcade2.core.StackSliceEngine;
import com.yamone.arcade2.data.RankingGateway;

public final class CoreTests {
    private static int passed;
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        passed++; System.out.println("PASS " + name);
    }
    private static void hit(OrbitEngine e) {
        e.advance(((e.target() - e.angle() + 360) % 360) / e.speed());
        e.tap();
    }
    public static void main(String[] args) {
        orbitTests();
        RankingGateway.Board emptyBoard = new RankingGateway.Board(GameId.ORBIT_SNAP, -1, null, null, null);
        check(emptyBoard.totalPlayers == 0 && emptyBoard.top.isEmpty() && emptyBoard.nearby.isEmpty(), "empty ranking contract shows no fake entries");
        check("normal".equals(RankingGateway.MODE_ID) && "points".equals(RankingGateway.SCORE_UNIT), "ranking contract matches shared worker mode and unit");
        check(GameId.values().length == 6, "catalog contains all six approved concepts");
        colorBreakTests();
        twinTapTests();
        lineSurfTests();
        pocketPulseTests();
        stackSliceTests();
        System.out.println("All " + passed + " core checks passed.");
    }
    private static void orbitTests() {
        OrbitEngine e = new OrbitEngine(77);
        double angle = e.angle(); e.advance(.1);
        check(e.state() == OrbitEngine.State.RUNNING && e.angle() != angle && e.lives() == 5, "orbit rotates automatically with five lives");
        hit(e);
        check(e.score() == 150 && e.jumps() == 1 && e.perfects() == 1, "orbit center tap awards perfect");
        e.tap(); e.tap();
        check(e.score() == 150 && e.lives() == 5, "orbit rapid repeat taps cannot double judge");
        angle = e.angle(); e.advance(.08);
        check(e.angle() != angle && e.feedbackRemaining() > 0, "orbit keeps rotating during hit feedback");
        double at = e.elapsed(), cooldown = e.feedbackRemaining(); angle = e.angle();
        e.pause(); e.pause(); e.advance(45); e.tap();
        check(e.elapsed() == at && e.angle() == angle && e.feedbackRemaining() == cooldown && e.lives() == 5, "orbit pause freezes rotation feedback and input");
        e.resume(); hit(e);
        check(e.jumps() == 2 && e.bestCombo() == 2 && e.score() == 300, "orbit resumes automatic rotation and scoring");
        e.advance(OrbitEngine.FEEDBACK_SECONDS); e.tap();
        check(e.lives() == 4 && e.combo() == 0 && e.bestCombo() == 2 && e.score() == 300, "orbit early tap loses one life and resets combo");
        OrbitEngine edge = new OrbitEngine(8);
        edge.advance(((edge.target()-edge.angle()+360)%360 - edge.tolerance()) / edge.speed()); edge.tap();
        check(edge.jumps() == 1 && edge.score() == 100 && edge.perfects() == 0, "orbit leading edge counts as normal hit");
        OrbitEngine exit = new OrbitEngine(8);
        exit.advance(((exit.target()-exit.angle()+360)%360 + exit.tolerance()) / exit.speed()); exit.tap();
        check(exit.lives() == 4 && exit.feedbackId() == 1 && exit.score() == 0, "orbit passing trailing edge automatically misses exactly once");
        OrbitEngine fail = new OrbitEngine(8);
        for (int i=0; i<4; i++) { fail.tap(); fail.advance(OrbitEngine.FEEDBACK_SECONDS); }
        check(fail.lives() == 1 && fail.state() == OrbitEngine.State.RUNNING, "orbit fourth miss retains final chance");
        fail.tap();
        check(fail.lives() == 0 && fail.state() == OrbitEngine.State.FINISHED, "orbit fifth miss ends game");
        at = fail.elapsed(); fail.advance(100); fail.tap(); fail.resume();
        check(fail.elapsed() == at && fail.lives() == 0 && fail.score() == 0, "orbit terminal state is immutable");
        OrbitEngine idle = new OrbitEngine(1); idle.advance(Double.MAX_VALUE);
        check(idle.state() == OrbitEngine.State.FINISHED && idle.lives() == 0 && idle.score() == 0 && idle.feedbackId() == 5, "orbit huge idle delta is bounded and awards no score");
        OrbitEngine clock = new OrbitEngine(9); double speed = clock.speed(), width = clock.tolerance();
        for (int i=0; i<1500; i++) hit(clock);
        check(clock.elapsed() > 600 && clock.state() == OrbitEngine.State.RUNNING && clock.lives() == 5 && clock.score() > 100000, "orbit has no 60-second or ten-minute time limit");
        check(clock.speed() > speed && clock.tolerance() < width && clock.level() > 100, "orbit accelerates while target window narrows");
        check(clock.speed() < 360 && clock.tolerance() > 10, "orbit long play keeps a playable target and speed ceiling");
        try {
            java.lang.reflect.Field field = OrbitEngine.class.getDeclaredField("score"); field.setAccessible(true);
            field.setInt(clock, OrbitEngine.MAX_SCORE - 1); hit(clock);
            check(clock.score() == OrbitEngine.MAX_SCORE, "orbit score safely saturates at server ceiling");
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        check(OrbitEngine.angularDistance(359, 1) == 2 && OrbitEngine.angularDistance(-1, 361) == 2, "orbit angle wrap handles target crossing");
        OrbitEngine a = new OrbitEngine(123), b = new OrbitEngine(123), c = new OrbitEngine(123);
        for (int i = 0; i < 180; i++) a.advance(1.0 / 60);
        for (int i = 0; i < 360; i++) b.advance(1.0 / 120);
        c.advance(3);
        check(OrbitEngine.angularDistance(a.angle(), b.angle()) < .00001 && OrbitEngine.angularDistance(a.angle(), c.angle()) < .00001 && a.lives() == c.lives(), "orbit 60fps 120fps and large frame have identical motion and misses");
        at = a.elapsed(); a.advance(Double.NaN); a.advance(Double.POSITIVE_INFINITY); a.advance(-1); a.advance(0);
        check(a.elapsed() == at, "orbit invalid deltas ignored");
        OrbitEngine seededA = new OrbitEngine(44), seededB = new OrbitEngine(44);
        for (int i=0; i<100; i++) { hit(seededA); hit(seededB); }
        check(seededA.target() == seededB.target() && seededA.score() == seededB.score(), "orbit seeded target sequence is reproducible");
        for (int seed=0; seed<100; seed++) {
            OrbitEngine sample = new OrbitEngine(seed);
            for (int n=0; n<30; n++) hit(sample);
            if (sample.jumps() != 30 || sample.lives() != 5) throw new AssertionError("Orbit wrap/target sequence " + seed);
        }
        check(true, "orbit many target wraps stay hittable without lost lives");
    }
    private static int matchingLane(ColorBreakEngine e) {
        for (int i = 0; i < 4; i++) if (e.laneColor(i) == e.targetColor()) return i;
        throw new AssertionError("No matching color");
    }
    private static void colorHit(ColorBreakEngine e) {
        e.tapLane(matchingLane(e));
        e.advance(e.wallDuration() * (1 - e.wallProgress()));
    }
    private static void colorBreakTests() {
        ColorBreakEngine e = new ColorBreakEngine(77);
        e.advance(10); e.tapLane(-1); e.tapLane(4);
        check(e.state() == ColorBreakEngine.State.READY && e.elapsed() == 0, "color waits for a valid pad tap");
        int mask = 0; for (int i = 0; i < 4; i++) mask |= 1 << e.laneColor(i);
        check(mask == 15 && e.lives() == 5, "color starts with four distinct colors and five lives");
        e.tapLane((matchingLane(e) + 1) % 4); e.advance(e.wallDuration() - .001);
        check(e.score() == 0 && e.lives() == 5, "color only judges at crossing");
        e.tapLane(matchingLane(e)); e.advance(.001);
        check(e.score() == 100 && e.combo() == 1 && e.passed() == 1, "color last moment lane change scores once");
        int selected = e.lane();
        e.tapLane((selected + 1) % 4); e.advance(ColorBreakEngine.RECOVERY / 2);
        check(e.score() == 100 && e.feedbackId() == 1 && e.lane() == selected, "color feedback interval rejects new input and duplicate score");
        e.advance(ColorBreakEngine.RECOVERY / 2); colorHit(e);
        check(e.score() == 210 && e.combo() == 2 && e.bestCombo() == 2, "color consecutive hit earns combo bonus");
        e.advance(ColorBreakEngine.RECOVERY); e.tapLane((matchingLane(e) + 1) % 4); e.advance(e.wallDuration());
        check(e.combo() == 0 && e.bestCombo() == 2 && e.score() == 210 && e.lives() == 4, "color one mismatch costs exactly one life and resets combo");
        double at = e.elapsed(), recovery = e.recoveryRemaining(); int lane = e.lane();
        e.pause(); e.pause(); e.advance(99); e.tapLane((lane + 1) % 4);
        check(e.elapsed() == at && e.recoveryRemaining() == recovery && e.lane() == lane, "color pause freezes recovery and input");
        e.resume(); e.advance(ColorBreakEngine.RECOVERY); colorHit(e);
        check(e.combo() == 1 && e.score() == 310, "color resume and post-miss hit restart combo");
        ColorBreakEngine ready = new ColorBreakEngine(1); ready.pause(); ready.resume(); ready.advance(9);
        check(ready.state() == ColorBreakEngine.State.READY && ready.elapsed() == 0, "color pause before first input preserves ready state");
        ColorBreakEngine failed = new ColorBreakEngine(2);
        for (int i = 0; i < 5; i++) {
            failed.tapLane((matchingLane(failed) + 1) % 4); failed.advance(failed.wallDuration());
            if (i < 4) {
                check(failed.state() == ColorBreakEngine.State.RUNNING && failed.lives() == 4-i, "color survives miss " + (i+1));
                failed.advance(ColorBreakEngine.RECOVERY);
            }
        }
        check(failed.state() == ColorBreakEngine.State.FINISHED && failed.lives() == 0 && failed.score() == 0, "color exactly five misses end a run");
        at = failed.elapsed(); failed.tapLane(matchingLane(failed)); failed.advance(1000); failed.pause(); failed.resume();
        check(failed.elapsed() == at && failed.score() == 0 && failed.state() == ColorBreakEngine.State.FINISHED, "color finished state rejects input and time");
        ColorBreakEngine clock = new ColorBreakEngine(4), sameSeed = new ColorBreakEngine(4);
        int expectedScore = 0, laneCoverage = 0; boolean shuffled = true, unique = true, faster = true, deterministic = true;
        double previousDuration = clock.wallDuration();
        for (int round = 0; round < 1200; round++) {
            int[] before = new int[4]; int oldTarget = clock.targetColor();
            mask = 0;
            for (int i = 0; i < 4; i++) {
                before[i] = clock.laneColor(i); mask |= 1 << before[i];
                deterministic &= before[i] == sameSeed.laneColor(i);
            }
            deterministic &= clock.targetColor() == sameSeed.targetColor();
            unique &= mask == 15; laneCoverage |= 1 << matchingLane(clock);
            colorHit(clock); colorHit(sameSeed);
            expectedScore += 100 + Math.min(10, clock.combo() - 1) * 10;
            clock.advance(ColorBreakEngine.RECOVERY); sameSeed.advance(ColorBreakEngine.RECOVERY);
            boolean changed = false; for (int i = 0; i < 4; i++) changed |= before[i] != clock.laneColor(i);
            shuffled &= changed && oldTarget != clock.targetColor();
            faster &= clock.wallDuration() < previousDuration && clock.wallDuration() > .35;
            previousDuration = clock.wallDuration();
        }
        check(clock.state() == ColorBreakEngine.State.RUNNING && clock.elapsed() > 600 && clock.lives() == 5, "color perfect play remains alive past ten minutes without a timer");
        check(clock.score() == expectedScore && clock.score() > 100000, "color endless score crosses the old server limit without truncation");
        check(faster && clock.level() > 100, "color difficulty keeps increasing beyond the initial minute");
        check(shuffled && unique && laneCoverage == 15, "color each row reshuffles all four colors with one target across every lane");
        check(deterministic, "color same seed yields reproducible color and target sequences");
        ColorBreakEngine unattended = new ColorBreakEngine(9); colorHit(unattended); unattended.advance(1000);
        check(unattended.passed() == 1 && unattended.lives() == 0, "color holding a lane never farms passive hits");
        ColorBreakEngine paused = new ColorBreakEngine(8); paused.tapLane(3); paused.advance(.45); paused.pause();
        at = paused.elapsed(); double progress = paused.wallProgress(); paused.advance(100); paused.tapLane(0);
        check(paused.elapsed() == at && paused.wallProgress() == progress && paused.lane() == 3, "color pause freezes an in-flight wall and chosen pad");
        paused.resume(); paused.tapLane(matchingLane(paused)); paused.advance(paused.wallDuration() * (1-paused.wallProgress()));
        check(paused.score() == 100, "color paused wall can finish correctly after resume");
        ColorBreakEngine a = new ColorBreakEngine(123), b = new ColorBreakEngine(123);
        a.tapLane(0); b.tapLane(0);
        for (int i = 0; i < 720; i++) a.advance(1.0 / 60);
        for (int i = 0; i < 1440; i++) b.advance(1.0 / 120);
        check(a.score() == b.score() && a.lives() == b.lives() && a.feedbackId() == b.feedbackId()
            && Math.abs(a.elapsed() - b.elapsed()) < 1e-8 && Math.abs(a.wallProgress() - b.wallProgress()) < 1e-8, "color 60Hz and 120Hz outcomes match");
        ColorBreakEngine large = new ColorBreakEngine(123); large.tapLane(0); large.advance(12);
        check(large.score() == a.score() && large.lives() == a.lives() && large.feedbackId() == a.feedbackId(), "color delayed frame cannot skip a collision");
        ColorBreakEngine huge = new ColorBreakEngine(2); huge.tapLane(matchingLane(huge)); huge.advance(Double.MAX_VALUE);
        check(huge.lives() == 0 && huge.passed() == 1 && Double.isFinite(huge.elapsed()), "color huge finite delay terminates without overflow or looping");
        try {
            ColorBreakEngine limit = new ColorBreakEngine(11);
            java.lang.reflect.Field score = ColorBreakEngine.class.getDeclaredField("score"); score.setAccessible(true);
            score.setInt(limit, ColorBreakEngine.MAX_SCORE - 20); colorHit(limit);
            check(limit.score() == ColorBreakEngine.MAX_SCORE && limit.state() == ColorBreakEngine.State.RUNNING, "color integer score bound never ends the endless run");
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        at = ready.elapsed(); ready.tapLane(0); ready.advance(Double.NaN); ready.advance(Double.POSITIVE_INFINITY); ready.advance(-1); ready.advance(0);
        check(ready.elapsed() == at, "color invalid time deltas ignored");
        check(GameId.ORBIT_SNAP.ready && GameId.COLOR_BREAK.ready && GameId.TWIN_TAP.ready && GameId.LINE_SURF.ready
            && GameId.POCKET_PULSE.ready && GameId.STACK_SLICE.ready, "all six implemented games unlocked");
    }
    private static void twinHit(TwinTapEngine e) {
        if (e.targetOffset()>0) e.advance(e.targetOffset());
        int mask=e.noteMask();
        for (int lane=0;lane<4;lane++) if ((mask&(1<<lane))!=0) e.tapLane(lane);
    }
    private static void twinTapTests() {
        TwinTapEngine e=new TwinTapEngine(77);
        e.advance(5); e.tapLane(-1); e.tapLane(4);
        check(e.state()==TwinTapEngine.State.READY && e.elapsed()==0 && e.lives()==5, "tap tap waits for explicit start with five lives");
        check("탭탭".equals(GameId.TWIN_TAP.title) && "twin_tap".equals(GameId.TWIN_TAP.key), "tap tap rename preserves stable ranking identity");
        e.start(); e.tapLane(Integer.numberOfTrailingZeros(e.noteMask()));
        check(e.score()==0 && e.lives()==5 && e.tappedMask()==0, "tap tap very early input is ignored");
        twinHit(e);
        check(e.score()==150 && e.hits()==1 && e.perfects()==1 && e.combo()==1, "tap tap exact single awards perfect");
        int score=e.score(), serial=e.noteSerial();
        for (int lane=0;lane<4;lane++) e.tapLane(lane);
        check(e.score()==score && e.noteSerial()==serial, "tap tap repeat input cannot score next note");
        check(e.noteProgress()<1e-8, "tap tap each new row begins at top without a visual jump");

        TwinTapEngine edge=new TwinTapEngine(6); edge.start();
        edge.advance(edge.targetOffset()-TwinTapEngine.HIT_WINDOW); edge.tapLane(Integer.numberOfTrailingZeros(edge.noteMask()));
        check(edge.score()==100 && edge.feedback()==TwinTapEngine.Feedback.HIT, "tap tap early window boundary counts as hit");
        TwinTapEngine lateHit=new TwinTapEngine(6); lateHit.start();
        lateHit.advance(lateHit.targetOffset()+.1); lateHit.tapLane(Integer.numberOfTrailingZeros(lateHit.noteMask()));
        check(lateHit.score()==100 && lateHit.lives()==5, "tap tap late valid touch still scores");
        TwinTapEngine wrong=new TwinTapEngine(7); wrong.start(); wrong.advance(wrong.targetOffset());
        wrong.tapLane((Integer.numberOfTrailingZeros(wrong.noteMask())+1)%4);
        check(wrong.lives()==4 && wrong.score()==0 && wrong.combo()==0, "tap tap wrong lane loses exactly one life");
        for (int lane=0;lane<4;lane++) wrong.tapLane(lane);
        check(wrong.lives()==4, "tap tap extra pointers after miss cannot multiply life loss");

        TwinTapEngine chord=new TwinTapEngine(77); chord.start();
        int watchdog=100;
        while (Integer.bitCount(chord.noteMask())!=2 && watchdog-->0) twinHit(chord);
        check(watchdog>0, "tap tap sequence includes two-note chords");
        int first=Integer.numberOfTrailingZeros(chord.noteMask());
        int second=Integer.numberOfTrailingZeros(chord.noteMask()&~(1<<first));
        int beforeScore=chord.score(), beforeHits=chord.hits(), beforeCombo=chord.combo();
        chord.advance(chord.targetOffset()-.1); chord.tapLane(first); chord.tapLane(first);
        check(chord.tappedMask()==(1<<first) && chord.hits()==beforeHits, "tap tap chord needs distinct second lane");
        double at=chord.elapsed(), offset=chord.targetOffset(); int tapped=chord.tappedMask();
        chord.pause(); chord.advance(99); chord.tapLane(second);
        check(chord.elapsed()==at && chord.targetOffset()==offset && chord.tappedMask()==tapped, "tap tap pause freezes partial chord and input");
        chord.resume(); chord.advance(.15); chord.tapLane(second);
        check(chord.score()==beforeScore+200+Math.min(100,beforeCombo*10) && chord.hits()==beforeHits+1
            && chord.simultaneousHits()==1 && chord.feedback()==TwinTapEngine.Feedback.HIT, "tap tap resumed split chord scores once with worst timing grade");
        int bestCombo=chord.bestCombo(); score=chord.score();
        chord.advance(chord.targetOffset()+TwinTapEngine.HIT_WINDOW);
        check(chord.lives()==4 && chord.combo()==0 && chord.bestCombo()==bestCombo && chord.score()==score, "tap tap miss resets combo but preserves score and best");
        twinHit(chord);
        check(chord.combo()==1, "tap tap restarts combo after miss");

        TwinTapEngine partial=new TwinTapEngine(1); partial.start();
        while (Integer.bitCount(partial.noteMask())!=2) twinHit(partial);
        partial.advance(partial.targetOffset()); score=partial.score();
        partial.tapLane(Integer.numberOfTrailingZeros(partial.noteMask()));
        partial.advance(TwinTapEngine.HIT_WINDOW);
        check(partial.lives()==4 && partial.score()==score && partial.tappedMask()==0, "tap tap incomplete chord loses one life and gives no partial points");
        TwinTapEngine ready=new TwinTapEngine(9); ready.pause(); ready.resume(); ready.advance(9);
        check(ready.state()==TwinTapEngine.State.READY && ready.elapsed()==0, "tap tap pause before start preserves ready");
        TwinTapEngine failed=new TwinTapEngine(11); failed.start();
        for (int i=0;i<4;i++) failed.advance(failed.targetOffset()+TwinTapEngine.HIT_WINDOW);
        check(failed.state()==TwinTapEngine.State.RUNNING && failed.lives()==1, "tap tap fourth miss retains last heart");
        failed.advance(failed.targetOffset()+TwinTapEngine.HIT_WINDOW);
        check(failed.state()==TwinTapEngine.State.FINISHED && failed.lives()==0 && failed.score()==0, "tap tap fifth unattended row ends run without passive score");
        at=failed.elapsed(); failed.start(); failed.resume(); failed.tapLane(0); failed.advance(100);
        check(failed.elapsed()==at && failed.score()==0 && failed.lives()==0, "tap tap finished state rejects input");

        TwinTapEngine clock=new TwinTapEngine(13); clock.start(); int lanes=0, pairs=0;
        for (int i=0;i<1500;i++) {
            int mask=clock.noteMask(), count=Integer.bitCount(mask);
            if (mask<=0 || mask>15 || count<1 || count>2) throw new AssertionError("Invalid four-lane mask "+mask);
            lanes|=mask;
            if (count==2) pairs|=1<<mask;
            twinHit(clock);
        }
        check(lanes==15 && Integer.bitCount(pairs)==6, "tap tap uses all four lanes and all six two-finger pairings");
        check(clock.elapsed()>600 && clock.lives()==5 && clock.hits()==1500 && clock.score()>100000
            && clock.state()==TwinTapEngine.State.RUNNING, "tap tap continues past ten minutes and old score ceiling");
        check(clock.travelTime()<.6 && clock.travelTime()>.48 && clock.level()>100, "tap tap keeps accelerating toward readable minimum travel");
        try {
            java.lang.reflect.Field field=TwinTapEngine.class.getDeclaredField("score"); field.setAccessible(true);
            field.setInt(clock,TwinTapEngine.MAX_SCORE-1); twinHit(clock);
            check(clock.score()==TwinTapEngine.MAX_SCORE && clock.state()==TwinTapEngine.State.RUNNING, "tap tap safely saturates score without ending play");
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        TwinTapEngine a=new TwinTapEngine(123), b=new TwinTapEngine(123), large=new TwinTapEngine(123);
        a.start(); b.start(); large.start();
        for (int i=0;i<600;i++) a.advance(1.0/60);
        for (int i=0;i<1200;i++) b.advance(1.0/120);
        large.advance(10);
        check(a.lives()==b.lives() && a.feedbackId()==b.feedbackId() && Math.abs(a.elapsed()-b.elapsed())<1e-8, "tap tap 60Hz and 120Hz outcomes match");
        check(large.lives()==a.lives() && Math.abs(large.elapsed()-a.elapsed())<1e-8, "tap tap large delta cannot skip deadlines");
        TwinTapEngine huge=new TwinTapEngine(8); huge.start(); huge.advance(Double.MAX_VALUE);
        check(huge.state()==TwinTapEngine.State.FINISHED && huge.feedbackId()==5 && Double.isFinite(huge.elapsed()), "tap tap huge idle delta terminates without overflow");
        TwinTapEngine copyA=new TwinTapEngine(45), copyB=new TwinTapEngine(45); copyA.start(); copyB.start();
        for (int i=0;i<100;i++) { twinHit(copyA); twinHit(copyB); }
        check(copyA.noteMask()==copyB.noteMask() && copyA.score()==copyB.score(), "tap tap seeded sequence is reproducible");
        at=ready.elapsed(); ready.start(); ready.advance(Double.NaN); ready.advance(Double.POSITIVE_INFINITY); ready.advance(-1); ready.advance(0);
        ready.tapLane(-1); ready.tapLane(4);
        check(ready.elapsed()==at && ready.lives()==5, "tap tap invalid time and lanes ignored");
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
    private static void stackBalancedCut(StackSliceEngine e) {
        int watchdog = 400000;
        while (e.state() == StackSliceEngine.State.RUNNING && !e.waitingNext()
            && Math.abs(Math.abs(e.incomingCenter() - e.topCenter()) - StackSliceEngine.CUT_WIDTH / 2) > .03
            && watchdog-- > 0) e.advance(.0005);
        if (watchdog <= 0) throw new AssertionError("Could not align stack cut");
        double offset = e.incomingCenter() - e.topCenter();
        if (e.state() == StackSliceEngine.State.RUNNING && !e.waitingNext()) e.swipe(offset < 0 ? -1 : 1);
    }
    private static void stackSliceTests() {
        StackSliceEngine e = new StackSliceEngine(77), same = new StackSliceEngine(77);
        e.advance(8); e.swipe(0); e.swipe(2);
        check(e.state() == StackSliceEngine.State.READY && e.elapsed() == 0 && e.score() == 0, "stack waits for explicit start and valid direction");
        check(e.incomingCenter() == same.incomingCenter() && e.incomingWidth() == 130
            && e.topWidth() == StackSliceEngine.BASE_WIDTH, "stack seed fixes first moving block on bounded base");
        e.start();
        check(e.state() == StackSliceEngine.State.RUNNING && e.score() == 0 && e.layerCount() == 1, "stack start does not place a block");
        stackBalancedCut(e);
        check(e.score() == 200 && e.placed() == 1 && e.balanced() == 1 && e.layerCount() == 2
            && Math.abs(e.tilt()) < .001 && e.feedback() == StackSliceEngine.Feedback.BALANCED, "stack aligned side cut earns full balance score");
        int score = e.score(), feedback = e.feedbackId(); e.swipe(-1); e.swipe(1);
        check(e.score() == score && e.feedbackId() == feedback && e.waitingNext(), "stack recovery rejects duplicate swipes");
        int serial = e.blockSerial(); e.advance(StackSliceEngine.RECOVERY);
        check(e.blockSerial() == serial + 1 && !e.waitingNext() && e.turnElapsed() == 0, "stack recovery creates exactly one next block");

        e.advance(.0005);
        double offset = e.incomingCenter() - e.topCenter();
        int wrongDirection = offset < 0 ? 1 : -1;
        double beforeWidth = e.topWidth(); e.swipe(wrongDirection);
        check(e.placed() == 2 && e.topWidth() < beforeWidth && Math.abs(e.tilt()) > 0
            && e.score() < 400, "stack wrong-side cut narrows support and reduces balance bonus");

        StackSliceEngine collapse = new StackSliceEngine(5); collapse.start();
        int watchdog = 100;
        while (collapse.state() == StackSliceEngine.State.RUNNING && watchdog-- > 0) {
            if (collapse.waitingNext()) collapse.advance(StackSliceEngine.RECOVERY);
            else {
                collapse.advance(.35);
                double drift = collapse.incomingCenter() - collapse.topCenter();
                collapse.swipe(drift < 0 ? 1 : -1);
            }
        }
        check(watchdog > 0 && collapse.state() == StackSliceEngine.State.FINISHED
            && collapse.feedback() == StackSliceEngine.Feedback.FALL, "stack repeated bad cuts eventually lose center-of-mass support");
        int placed = collapse.placed(); double at = collapse.elapsed(); collapse.swipe(1); collapse.advance(100); collapse.pause(); collapse.resume();
        check(collapse.placed() == placed && collapse.elapsed() == at && collapse.state() == StackSliceEngine.State.FINISHED, "stack fallen state rejects input and time");

        StackSliceEngine timeout = new StackSliceEngine(9); timeout.start(); timeout.advance(10);
        check(timeout.state() == StackSliceEngine.State.FINISHED && timeout.feedback() == StackSliceEngine.Feedback.TIMEOUT
            && timeout.placed() == 0 && timeout.elapsed() >= 3.5 && timeout.elapsed() <= 3.61, "stack idle block deadline prevents camping");

        StackSliceEngine paused = new StackSliceEngine(8); paused.start(); paused.advance(.4);
        at = paused.elapsed(); double center = paused.incomingCenter(), turn = paused.turnElapsed();
        paused.pause(); paused.advance(30); paused.swipe(1);
        check(paused.state() == StackSliceEngine.State.PAUSED && paused.elapsed() == at
            && paused.incomingCenter() == center && paused.turnElapsed() == turn && paused.placed() == 0, "stack pause freezes block, deadline and input");
        paused.resume(); paused.advance(.1);
        check(paused.state() == StackSliceEngine.State.RUNNING && paused.elapsed() > at, "stack resume continues same moving block");
        StackSliceEngine ready = new StackSliceEngine(3); ready.pause(); ready.resume(); ready.advance(9);
        check(ready.state() == StackSliceEngine.State.READY && ready.elapsed() == 0, "stack pause before start preserves ready state");

        StackSliceEngine clock = new StackSliceEngine(13); clock.start();
        while (clock.state() != StackSliceEngine.State.FINISHED) {
            if (clock.waitingNext()) clock.advance(Math.min(clock.recoveryRemaining(), clock.remaining()));
            else stackBalancedCut(clock);
        }
        check(clock.elapsed() == 60 && clock.placed() > 120 && clock.balanced() == clock.placed()
            && clock.layerCount() == clock.placed() + 1, "stack balanced play reaches exact 60 seconds without layer overflow");
        check(clock.bestBalanceStreak() == clock.placed() && clock.maxTilt() < .01
            && clock.score() == clock.placed() * 200, "stack sustained centered cuts keep full balance bonus");
        check(clock.angularSpeed() > 1.9 && clock.angularSpeed() <= 2.62 && clock.turnLimit() >= 2.7, "stack motion accelerates within deadline bounds");

        StackSliceEngine a = new StackSliceEngine(123), b = new StackSliceEngine(123), large = new StackSliceEngine(123);
        a.start(); b.start(); large.start();
        for (int i = 0; i < 240; i++) a.advance(1.0 / 60);
        for (int i = 0; i < 480; i++) b.advance(1.0 / 120);
        large.advance(4);
        check(a.state() == b.state() && a.feedback() == b.feedback() && Math.abs(a.elapsed() - b.elapsed()) < .01
            && Math.abs(a.incomingCenter() - b.incomingCenter()) < .05, "stack 60Hz and 120Hz timeout consistency");
        check(large.state() == a.state() && large.feedback() == a.feedback() && Math.abs(large.elapsed() - a.elapsed()) < .01, "stack delayed frame cannot skip block timeout");
        at = ready.elapsed(); ready.start(); ready.advance(Double.NaN); ready.advance(Double.POSITIVE_INFINITY); ready.advance(-1); ready.advance(0);
        check(ready.elapsed() == at, "stack invalid time deltas ignored");
    }
}
