package ti.gifview;

/**
 * Pure-Java replica of the iOS playback state machine (ios/TiGifviewGifView.m).
 * Zero Android imports so it can be unit-tested on a desktop JVM.
 *
 * Mirrored behaviors: pendingAutoStartAfterLoad, boomerang ping-pong (loop
 * counted only when bouncing at the start), loop limit ("finished" stops
 * without advancing), stop() full reset, single-frame GIFs never animate,
 * and at most one frame advance per tick.
 */
public final class PlaybackEngine {

	public interface Listener {
		void onShowFrame(int index);

		void onLoopComplete(int completedLoops, boolean finished);
	}

	private static final int FORWARD = 1;
	private static final int BACKWARD = -1;

	private final Listener listener;

	private volatile int[] delaysMs = new int[0];
	private volatile int frameIndex = 0;
	private long accumulatedNanos = 0L;
	private int direction = FORWARD;
	private int completedLoops = 0;
	private int loopCount = 0;
	private boolean boomerang = false;
	private volatile boolean playing = false;
	private volatile boolean paused = false;
	private boolean pendingStart = false;

	public PlaybackEngine(Listener listener) {
		this.listener = listener;
	}

	public void setLoopCount(int loopCount) {
		this.loopCount = loopCount;
	}

	public void setBoomerang(boolean boomerang) {
		this.boomerang = boomerang;
	}

	public void setFrames(int[] delaysMs) {
		this.delaysMs = delaysMs != null ? delaysMs : new int[0];
		frameIndex = 0;
		direction = FORWARD;
		completedLoops = 0;
		accumulatedNanos = 0L;
		playing = false;
		paused = false;
	}

	public boolean play() {
		if (delaysMs.length == 0) {
			pendingStart = true;
			return false;
		}
		if (delaysMs.length <= 1) {
			return false;
		}
		paused = false;
		playing = true;
		return true;
	}

	public boolean pause() {
		if (!playing) {
			return false;
		}
		paused = true;
		playing = false;
		return true;
	}

	public boolean resume() {
		if (delaysMs.length == 0) {
			pendingStart = true;
			return false;
		}
		if (!paused) {
			return false;
		}
		paused = false;
		playing = true;
		return true;
	}

	public void stop() {
		playing = false;
		paused = false;
		pendingStart = false;
		frameIndex = 0;
		direction = FORWARD;
		accumulatedNanos = 0L;
		completedLoops = 0;
	}

	public boolean consumePendingStart() {
		boolean wasPending = pendingStart;
		pendingStart = false;
		return wasPending;
	}

	public int currentFrame() {
		return frameIndex;
	}

	public int frameCount() {
		return delaysMs.length;
	}

	public boolean isPlaying() {
		return playing;
	}

	public boolean isPaused() {
		return paused;
	}

	public void tick(long deltaNanos) {
		if (!playing || delaysMs.length == 0) {
			return;
		}
		accumulatedNanos += deltaNanos;
		long delayNanos = delaysMs[frameIndex] * 1_000_000L;
		if (accumulatedNanos < delayNanos) {
			return;
		}
		accumulatedNanos -= delayNanos;
		advanceFrame();
	}

	private void advanceFrame() {
		int nextIndex = frameIndex + direction;
		boolean reachedEnd = direction == FORWARD && nextIndex >= delaysMs.length;
		boolean reachedStart = direction == BACKWARD && nextIndex < 0;

		if (reachedEnd || reachedStart) {
			if (boomerang) {
				direction = direction == FORWARD ? BACKWARD : FORWARD;
				nextIndex = frameIndex + direction;
				nextIndex = Math.max(0, Math.min(nextIndex, delaysMs.length - 1));
				if (reachedStart) {
					handleLoopCompleted();
					if (!playing) {
						return;
					}
				}
			} else {
				nextIndex = 0;
				handleLoopCompleted();
				if (!playing) {
					return;
				}
			}
		}

		frameIndex = nextIndex;
		listener.onShowFrame(frameIndex);
	}

	private void handleLoopCompleted() {
		completedLoops += 1;
		boolean finished = loopCount > 0 && completedLoops >= loopCount;
		listener.onLoopComplete(completedLoops, finished);
		if (finished) {
			playing = false;
			paused = false;
		}
	}
}
