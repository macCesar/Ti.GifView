package ti.gifview;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

/** Android counterpart of ios/Classes/TiGifviewModule.m. */
@Kroll.module(name = "TiGifview", id = "ti.gifview")
public class TiGifviewModule extends KrollModule {

	@Kroll.constant
	public static final int IMAGE_SCALING_ASPECT_FIT = 0;

	@Kroll.constant
	public static final int IMAGE_SCALING_ASPECT_FILL = 1;

	@Kroll.constant
	public static final int IMAGE_SCALING_FILL = 2;

	public TiGifviewModule() {
		super();
	}
}
