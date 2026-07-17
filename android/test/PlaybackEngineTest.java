package ti.gifview;

import java.util.ArrayList;
import java.util.List;

/** JVM-only test harness (no JUnit): javac + java with direct assertions. */
public class PlaybackEngineTest {

	static final long MS = 1_000_000L;

	static class Recorder implements PlaybackEngine.Listener {
		final List<String> events = new ArrayList<>();

		@Override
		public void onShowFrame(int index) {
			events.add("frame:" + index);
		}

		@Override
		public void onLoopComplete(int loops, boolean finished) {
			events.add("complete:" + loops + ":" + finished);
		}
	}

	static void check(boolean condition, String description) {
		if (!condition) {
			throw new AssertionError("FAILED: " + description);
		}
		System.out.println("OK: " + description);
	}

	public static void main(String[] args) {
		testPendingStartBeforeFramesLoaded();
		testSingleFrameDoesNotAnimate();
		testAdvanceAndInfiniteWrap();
		testLoopLimitStopsOnLastFrame();
		testBoomerangSequenceAndLoopCount();
		testBoomerangWithLoopLimit();
		testPauseResumeSemantics();
		testStopResets();
		testAtMostOneAdvancePerTick();
		System.out.println("ALL TESTS PASSED");
	}

	static void testPendingStartBeforeFramesLoaded() {
		Recorder recorder = new Recorder();
		PlaybackEngine engine = new PlaybackEngine(recorder);
		check(!engine.play(), "play() without frames does not start the driver");
		check(engine.consumePendingStart(), "pendingStart was recorded");
		check(!engine.consumePendingStart(), "consumePendingStart is consumed only once");
	}

	static void testSingleFrameDoesNotAnimate() {
		Recorder recorder = new Recorder();
		PlaybackEngine engine = new PlaybackEngine(recorder);
		engine.setFrames(new int[] { 100 });
		check(!engine.play(), "a single frame does not animate");
		check(!engine.isPlaying(), "the engine remains stopped");
	}

	static void testAdvanceAndInfiniteWrap() {
		Recorder recorder = new Recorder();
		PlaybackEngine engine = new PlaybackEngine(recorder);
		engine.setFrames(new int[] { 100, 100, 100 });
		check(engine.play(), "play() with frames starts the driver");
		engine.tick(100 * MS);
		engine.tick(100 * MS);
		engine.tick(100 * MS);
		check(recorder.events.equals(List.of("frame:1", "frame:2", "complete:1:false", "frame:0")),
			"infinite wraparound sequence, got: " + recorder.events);
		check(engine.isPlaying(), "loop=0 keeps playing after wraparound");
	}

	static void testLoopLimitStopsOnLastFrame() {
		Recorder recorder = new Recorder();
		PlaybackEngine engine = new PlaybackEngine(recorder);
		engine.setLoopCount(1);
		engine.setFrames(new int[] { 100, 100, 100 });
		engine.play();
		engine.tick(100 * MS);
		engine.tick(100 * MS);
		engine.tick(100 * MS);
		check(recorder.events.equals(List.of("frame:1", "frame:2", "complete:1:true")),
			"loop=1 finishes without frame:0, got: " + recorder.events);
		check(!engine.isPlaying() && !engine.isPaused(), "the engine is stopped, not paused");
		check(engine.currentFrame() == 2, "it remains on the final frame");
	}

	static void testBoomerangSequenceAndLoopCount() {
		Recorder recorder = new Recorder();
		PlaybackEngine engine = new PlaybackEngine(recorder);
		engine.setBoomerang(true);
		engine.setFrames(new int[] { 100, 100, 100 });
		engine.play();
		for (int i = 0; i < 6; i++) {
			engine.tick(100 * MS);
		}
		check(recorder.events.equals(List.of(
			"frame:1", "frame:2", "frame:1", "frame:0", "complete:1:false", "frame:1", "frame:2")),
			"boomerang sequence, got: " + recorder.events);
	}

	static void testBoomerangWithLoopLimit() {
		Recorder recorder = new Recorder();
		PlaybackEngine engine = new PlaybackEngine(recorder);
		engine.setBoomerang(true);
		engine.setLoopCount(1);
		engine.setFrames(new int[] { 100, 100, 100 });
		engine.play();
		for (int i = 0; i < 5; i++) {
			engine.tick(100 * MS);
		}
		check(recorder.events.equals(List.of(
			"frame:1", "frame:2", "frame:1", "frame:0", "complete:1:true")),
			"boomerang loop=1 sequence, got: " + recorder.events);
		check(!engine.isPlaying(), "the engine stops after finished");
		check(engine.currentFrame() == 0, "the engine remains on frame 0");
	}

	static void testPauseResumeSemantics() {
		Recorder recorder = new Recorder();
		PlaybackEngine engine = new PlaybackEngine(recorder);
		engine.setFrames(new int[] { 100, 100 });
		check(!engine.pause(), "pause without playback is a no-op");
		engine.play();
		check(engine.pause(), "pause during playback");
		check(engine.isPaused() && !engine.isPlaying(), "paused state");
		check(engine.play(), "play after pause starts again");
		check(engine.isPlaying() && !engine.isPaused(), "playing state after play-from-pause");
		engine.stop();
		engine.play();
		check(!engine.resume(), "resume without pause is a no-op");
		engine.pause();
		check(engine.resume(), "resume from paused state");
		check(engine.isPlaying() && !engine.isPaused(), "playing state after resume");
	}

	static void testStopResets() {
		Recorder recorder = new Recorder();
		PlaybackEngine engine = new PlaybackEngine(recorder);
		engine.setFrames(new int[] { 100, 100, 100 });
		engine.play();
		engine.tick(100 * MS);
		engine.stop();
		check(engine.currentFrame() == 0, "stop resets to frame 0");
		check(!engine.isPlaying() && !engine.isPaused(), "stop leaves the engine stopped");

		PlaybackEngine pendingEngine = new PlaybackEngine(new Recorder());
		pendingEngine.play();
		pendingEngine.stop();
		check(!pendingEngine.consumePendingStart(), "stop cancels pendingStart");
	}

	static void testAtMostOneAdvancePerTick() {
		Recorder recorder = new Recorder();
		PlaybackEngine engine = new PlaybackEngine(recorder);
		engine.setFrames(new int[] { 100, 100, 100 });
		engine.play();
		engine.tick(350 * MS);
		check(recorder.events.equals(List.of("frame:1")),
			"only one frame advance per tick, got: " + recorder.events);
		engine.tick(50 * MS);
		check(recorder.events.equals(List.of("frame:1", "frame:2")),
			"accumulated time is preserved between ticks");
	}
}
