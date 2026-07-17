package ti.gifview;

import android.graphics.Bitmap;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.gifdecoder.StandardGifDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.view.TiDrawableReference;

/** Resolves and decodes GIF data on a serial background queue. */
public final class GifLoader {

	public static final class Frame {
		public final Bitmap bitmap;
		public final int delayMs;

		Frame(Bitmap bitmap, int delayMs) {
			this.bitmap = bitmap;
			this.delayMs = delayMs;
		}
	}

	public interface Callback {
		void onLoaded(List<Frame> frames);

		void onError(String message);
	}

	private static final ExecutorService DECODE_EXECUTOR = Executors.newSingleThreadExecutor();

	private static final GifDecoder.BitmapProvider BITMAP_PROVIDER = new GifDecoder.BitmapProvider() {
		@Override
		public Bitmap obtain(int width, int height, Bitmap.Config config) {
			return Bitmap.createBitmap(width, height, config);
		}

		@Override
		public void release(Bitmap bitmap) {
			bitmap.recycle();
		}

		@Override
		public byte[] obtainByteArray(int size) {
			return new byte[size];
		}

		@Override
		public void release(byte[] bytes) {
		}

		@Override
		public int[] obtainIntArray(int size) {
			return new int[size];
		}

		@Override
		public void release(int[] array) {
		}
	};

	private GifLoader() {
	}

	public static void load(final KrollProxy proxy, final String url, final Callback callback) {
		DECODE_EXECUTOR.execute(() -> {
			try {
				byte[] data = readAllBytes(proxy, url);
				List<Frame> frames = decode(data);
				if (frames.isEmpty()) {
					postError(callback, "GIF contained no readable frames");
				} else {
					TiMessenger.postOnMain(() -> callback.onLoaded(frames));
				}
			} catch (Exception exception) {
				String message = exception.getMessage() != null
					? exception.getMessage()
					: "Unable to decode GIF";
				postError(callback, message);
			}
		});
	}

	private static void postError(Callback callback, String message) {
		TiMessenger.postOnMain(() -> callback.onError(message));
	}

	private static byte[] readAllBytes(KrollProxy proxy, String url) throws IOException {
		InputStream input = TiDrawableReference.fromUrl(proxy, url).getInputStream();
		if (input == null) {
			throw new IOException("Failed to download GIF");
		}

		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] chunk = new byte[16 * 1024];
			int read;
			while ((read = input.read(chunk)) != -1) {
				buffer.write(chunk, 0, read);
			}
			return buffer.toByteArray();
		} finally {
			try {
				input.close();
			} catch (IOException ignored) {
			}
		}
	}

	private static List<Frame> decode(byte[] data) throws IOException {
		if (data == null || data.length == 0) {
			throw new IOException("No GIF data provided");
		}

		GifHeaderParser parser = new GifHeaderParser();
		parser.setData(data);
		GifHeader header = parser.parseHeader();
		parser.clear();
		if (header.getNumFrames() <= 0) {
			throw new IOException("Data is not a valid image source");
		}

		StandardGifDecoder decoder = new StandardGifDecoder(BITMAP_PROVIDER);
		decoder.setData(header, data);
		List<Frame> frames = new ArrayList<>(decoder.getFrameCount());

		try {
			for (int index = 0; index < decoder.getFrameCount(); index++) {
				decoder.advance();
				Bitmap bitmap = decoder.getNextFrame();
				if (bitmap == null) {
					continue;
				}
				frames.add(new Frame(bitmap, normalizeDelay(decoder.getDelay(index))));
			}
		} finally {
			decoder.clear();
		}

		return frames;
	}

	private static int normalizeDelay(int delayMs) {
		if (delayMs <= 0) {
			delayMs = 100;
		}
		return Math.max(delayMs, 20);
	}
}
