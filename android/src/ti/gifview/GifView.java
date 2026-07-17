package ti.gifview;

import android.content.Context;
import android.view.Choreographer;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

/** Android counterpart of ios/TiGifviewGifView.m. */
public class GifView extends TiUIView
	implements PlaybackEngine.Listener, Choreographer.FrameCallback {

	private static final String PROPERTY_URL = "url";
	private static final String PROPERTY_AUTO_START = "autoStart";
	private static final String PROPERTY_LOOP = "loop";
	private static final String PROPERTY_BOOMERANG = "boomerang";
	private static final String PROPERTY_SCALING_MODE = "scalingMode";

	private final ImageView imageView;
	private final PlaybackEngine engine = new PlaybackEngine(this);

	private List<GifLoader.Frame> frames;
	private boolean autoStart = true;
	private boolean driverRunning = false;
	private long lastFrameTimeNanos = -1L;
	private int loadGeneration = 0;
	private boolean released = false;

	public GifView(TiViewProxy proxy) {
		super(proxy);
		imageView = new LifecycleImageView(proxy.getActivity());
		imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		setNativeView(imageView);
	}

	private final class LifecycleImageView extends ImageView {
		LifecycleImageView(Context context) {
			super(context);
		}

		@Override
		protected void onAttachedToWindow() {
			super.onAttachedToWindow();
			if (!released && engine.isPlaying()) {
				startDriver();
			}
		}

		@Override
		protected void onDetachedFromWindow() {
			stopDriver();
			super.onDetachedFromWindow();
		}
	}

	@Override
	public void processProperties(KrollDict properties) {
		super.processProperties(properties);
		if (properties.containsKey(PROPERTY_AUTO_START)) {
			autoStart = TiConvert.toBoolean(properties.get(PROPERTY_AUTO_START), true);
		}
		if (properties.containsKey(PROPERTY_LOOP)) {
			engine.setLoopCount(TiConvert.toInt(properties.get(PROPERTY_LOOP), 0));
		}
		if (properties.containsKey(PROPERTY_BOOMERANG)) {
			engine.setBoomerang(TiConvert.toBoolean(properties.get(PROPERTY_BOOMERANG), false));
		}
		if (properties.containsKey(PROPERTY_SCALING_MODE)) {
			applyScalingMode(TiConvert.toInt(
				properties.get(PROPERTY_SCALING_MODE),
				TiGifviewModule.IMAGE_SCALING_ASPECT_FIT));
		}
		if (properties.containsKey(PROPERTY_URL)) {
			loadUrl(TiConvert.toString(properties.get(PROPERTY_URL)));
		}
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy changedProxy) {
		if (PROPERTY_URL.equals(key)) {
			loadUrl(TiConvert.toString(newValue));
		} else if (PROPERTY_AUTO_START.equals(key)) {
			autoStart = TiConvert.toBoolean(newValue, true);
		} else if (PROPERTY_LOOP.equals(key)) {
			engine.setLoopCount(TiConvert.toInt(newValue, 0));
		} else if (PROPERTY_BOOMERANG.equals(key)) {
			engine.setBoomerang(TiConvert.toBoolean(newValue, false));
		} else if (PROPERTY_SCALING_MODE.equals(key)) {
			applyScalingMode(TiConvert.toInt(
				newValue,
				TiGifviewModule.IMAGE_SCALING_ASPECT_FIT));
		} else {
			super.propertyChanged(key, oldValue, newValue, changedProxy);
		}
	}

	private void applyScalingMode(int mode) {
		switch (mode) {
			case TiGifviewModule.IMAGE_SCALING_ASPECT_FILL:
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				break;
			case TiGifviewModule.IMAGE_SCALING_FILL:
				imageView.setScaleType(ImageView.ScaleType.FIT_XY);
				break;
			case TiGifviewModule.IMAGE_SCALING_ASPECT_FIT:
			default:
				imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
				break;
		}
	}

	private void loadUrl(String url) {
		if (url == null || url.length() == 0) {
			return;
		}
		int generation = ++loadGeneration;
		GifLoader.load(proxy, url, new LoadCallback(this, generation));
	}

	private void fireErrorEvent(String message) {
		KrollDict event = new KrollDict();
		event.put("success", false);
		event.put("message", message);
		proxy.fireEvent("error", event);
	}

	private static final class LoadCallback implements GifLoader.Callback {
		private final WeakReference<GifView> viewReference;
		private final int generation;

		LoadCallback(GifView view, int generation) {
			viewReference = new WeakReference<>(view);
			this.generation = generation;
		}

		private GifView liveView() {
			GifView view = viewReference.get();
			if (view == null || view.released || generation != view.loadGeneration) {
				return null;
			}
			return view;
		}

		@Override
		public void onLoaded(List<GifLoader.Frame> loadedFrames) {
			GifView view = liveView();
			if (view != null) {
				view.handleDecodedFrames(loadedFrames);
			}
		}

		@Override
		public void onError(String message) {
			GifView view = liveView();
			if (view != null) {
				view.fireErrorEvent(message);
			}
		}
	}

	private void handleDecodedFrames(List<GifLoader.Frame> loadedFrames) {
		stopDriver();
		frames = loadedFrames;
		int[] delays = new int[loadedFrames.size()];
		for (int index = 0; index < loadedFrames.size(); index++) {
			delays[index] = loadedFrames.get(index).delayMs;
		}

		boolean hadPendingStart = engine.consumePendingStart();
		engine.setFrames(delays);
		imageView.setImageBitmap(loadedFrames.get(0).bitmap);

		KrollDict event = new KrollDict();
		event.put("success", true);
		event.put("frameCount", loadedFrames.size());
		proxy.fireEvent("load", event);

		if (autoStart || hadPendingStart) {
			play();
		}
	}

	public void play() {
		if (engine.play()) {
			startDriver();
		}
	}

	public void pause() {
		if (engine.pause()) {
			stopDriver();
		}
	}

	public void resume() {
		if (engine.resume()) {
			startDriver();
		}
	}

	public void stop() {
		engine.stop();
		stopDriver();
		if (frames != null && !frames.isEmpty()) {
			imageView.setImageBitmap(frames.get(0).bitmap);
		}
	}

	public int currentFrame() {
		return engine.currentFrame();
	}

	public int frameCount() {
		return engine.frameCount();
	}

	public boolean isPlaying() {
		return engine.isPlaying();
	}

	public boolean isPaused() {
		return engine.isPaused();
	}

	private void startDriver() {
		if (driverRunning) {
			return;
		}
		driverRunning = true;
		lastFrameTimeNanos = -1L;
		Choreographer.getInstance().postFrameCallback(this);
	}

	private void stopDriver() {
		driverRunning = false;
		lastFrameTimeNanos = -1L;
		Choreographer.getInstance().removeFrameCallback(this);
	}

	@Override
	public void doFrame(long frameTimeNanos) {
		if (!driverRunning) {
			return;
		}
		if (lastFrameTimeNanos > 0) {
			engine.tick(frameTimeNanos - lastFrameTimeNanos);
		}
		lastFrameTimeNanos = frameTimeNanos;
		if (engine.isPlaying()) {
			Choreographer.getInstance().postFrameCallback(this);
		} else {
			driverRunning = false;
		}
	}

	@Override
	public void onShowFrame(int index) {
		if (frames == null || index < 0 || index >= frames.size()) {
			return;
		}
		imageView.setImageBitmap(frames.get(index).bitmap);
		KrollDict event = new KrollDict();
		event.put("index", index);
		event.put("frameCount", frames.size());
		proxy.fireEvent("frame", event);
	}

	@Override
	public void onLoopComplete(int completedLoops, boolean finished) {
		KrollDict event = new KrollDict();
		event.put("loopCount", completedLoops);
		event.put("finished", finished);
		proxy.fireEvent("complete", event);
	}

	@Override
	public void release() {
		released = true;
		loadGeneration++;
		engine.stop();
		stopDriver();
		imageView.setImageDrawable(null);
		frames = null;
		super.release();
	}
}
