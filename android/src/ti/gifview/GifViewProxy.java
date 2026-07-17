package ti.gifview;

import android.app.Activity;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

/** Android counterpart of ios/TiGifviewGifViewProxy.m. */
@Kroll.proxy(
	creatableInModule = TiGifviewModule.class,
	propertyAccessors = { "url", "autoStart", "loop", "boomerang", "scalingMode" }
)
public class GifViewProxy extends TiViewProxy {

	@Override
	public TiUIView createView(Activity activity) {
		return new GifView(this);
	}

	private GifView gifView() {
		return (GifView) peekView();
	}

	private GifView gifViewOrCreate() {
		return (GifView) getOrCreateView();
	}

	@Kroll.method(runOnUiThread = true)
	public void start() {
		GifView view = gifViewOrCreate();
		if (view != null) {
			view.play();
		}
	}

	@Kroll.method(runOnUiThread = true)
	public void play() {
		start();
	}

	@Kroll.method(runOnUiThread = true)
	public void pause() {
		GifView view = gifView();
		if (view != null) {
			view.pause();
		}
	}

	@Kroll.method(runOnUiThread = true)
	public void resume() {
		GifView view = gifViewOrCreate();
		if (view != null) {
			view.resume();
		}
	}

	@Kroll.method(runOnUiThread = true)
	public void stop() {
		GifView view = gifView();
		if (view != null) {
			view.stop();
		}
	}

	@Kroll.method
	public int currentFrame() {
		GifView view = gifView();
		return view != null ? view.currentFrame() : 0;
	}

	@Kroll.method
	public int frameCount() {
		GifView view = gifView();
		return view != null ? view.frameCount() : 0;
	}

	@Kroll.method
	public boolean playing() {
		GifView view = gifView();
		return view != null && view.isPlaying();
	}

	@Kroll.method
	public boolean paused() {
		GifView view = gifView();
		return view != null && view.isPaused();
	}
}
