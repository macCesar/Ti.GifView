/**
 * TiGifviewGifView
 *
 * Renders and drives an animated GIF frame-by-frame via CADisplayLink,
 * giving full runtime control: autoStart, pause, resume, stop, loop count
 * and boomerang (ping-pong) playback, plus "frame" / "complete" / "load" /
 * "error" events back to JS.
 *
 * Your Copyright Here
 */
#import "TiUIView.h"
#import "TiGifviewDecoder.h"

// Values must stay in sync with the MAKE_SYSTEM_PROP constants in
// TiGifviewModule.m (IMAGE_SCALING_ASPECT_FIT, IMAGE_SCALING_ASPECT_FILL,
// IMAGE_SCALING_FILL).
typedef NS_ENUM(NSInteger, TiGifviewScalingMode) {
  TiGifviewScalingModeAspectFit = 0,
  TiGifviewScalingModeAspectFill = 1,
  TiGifviewScalingModeFill = 2,
};

@interface TiGifviewGifView : TiUIView

// Property setters, following Titanium convention: view-rendering
// properties are set on the View class (not the ViewProxy), which is
// how Titanium's creation-dictionary dispatch finds them.
- (void)setUrl_:(id)value;
- (void)setAutoStart_:(id)value;
- (void)setLoop_:(id)value;
- (void)setBoomerang_:(id)value;
- (void)setScalingMode_:(id)value;

// Loading (called internally by setUrl_:, also usable directly)
- (void)loadGIFFromURL:(NSURL *)url;
- (void)loadGIFFromData:(NSData *)data;

// Playback controls (all safe to call before the GIF has finished loading;
// they are queued and applied once frames are available)
- (void)play;
- (void)pause;
- (void)resume;
- (void)stop;

// Config, mirrored from the proxy's JS properties
@property (nonatomic, assign) BOOL autoStart; // default YES
@property (nonatomic, assign) NSInteger loopCount; // 0 = infinite, default 0
@property (nonatomic, assign) BOOL boomerang; // default NO

// Read-only state, queried by the proxy
@property (nonatomic, readonly) NSUInteger currentFrame;
@property (nonatomic, readonly) NSUInteger frameCount;
@property (nonatomic, readonly) BOOL isPlaying;
@property (nonatomic, readonly) BOOL isPaused;

@end
