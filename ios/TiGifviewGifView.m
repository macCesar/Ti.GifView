/**
 * TiGifviewGifView
 *
 * Your Copyright Here
 */
#import "TiGifviewGifView.h"
#import "TiUtils.h"

typedef NS_ENUM(NSInteger, TiGifviewDirection) {
  TiGifviewDirectionForward = 1,
  TiGifviewDirectionBackward = -1,
};

@interface TiGifviewGifView ()
@property (nonatomic, strong) UIImageView *imageView;
@property (nonatomic, strong) NSArray<TiGifviewFrame *> *frames;
@property (nonatomic, strong) CADisplayLink *displayLink;
@property (nonatomic, assign) NSUInteger frameIndex;
@property (nonatomic, assign) NSTimeInterval accumulatedTime;
@property (nonatomic, assign) TiGifviewDirection direction;
@property (nonatomic, assign) NSInteger completedLoops;
@property (nonatomic, assign) BOOL playing;
@property (nonatomic, assign) BOOL paused;
@property (nonatomic, assign) BOOL loading;
// If play/resume is called before frames finish decoding, remember the intent
// and apply it once the active load completes.
@property (nonatomic, assign) BOOL pendingAutoStartAfterLoad;
@end

@implementation TiGifviewGifView

#pragma mark Lifecycle

- (instancetype)init
{
  self = [super init];
  if (self) {
    _autoStart = YES;
    _loopCount = 0;
    _boomerang = NO;
    _direction = TiGifviewDirectionForward;

    _imageView = [[UIImageView alloc] initWithFrame:self.bounds];
    _imageView.contentMode = UIViewContentModeScaleAspectFit;
    _imageView.clipsToBounds = YES;
    [self addSubview:_imageView];
  }
  return self;
}

- (void)dealloc
{
  [self invalidateDisplayLink];
}

- (void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
  [super frameSizeChanged:frame bounds:bounds];
  self.imageView.frame = bounds;
}

// Stopping the display link here is what keeps this view from leaking or
// ticking in the background once it's torn down / removed from a window.
- (void)removeFromSuperview
{
  [self invalidateDisplayLink];
  [super removeFromSuperview];
}

#pragma mark Titanium property setters (view-level, per Ti convention)

- (void)setUrl_:(id)value
{
  ENSURE_TYPE_OR_NIL(value, NSString);
  if (value == nil) {
    return;
  }
  NSURL *url = [TiUtils toURL:value proxy:self.proxy];
  [self loadGIFFromURL:url];
}

- (void)setAutoStart_:(id)value
{
  ENSURE_TYPE(value, NSNumber);
  self.autoStart = [TiUtils boolValue:value def:YES];
}

- (void)setLoop_:(id)value
{
  ENSURE_TYPE(value, NSNumber);
  self.loopCount = [TiUtils intValue:value def:0];
}

- (void)setBoomerang_:(id)value
{
  ENSURE_TYPE(value, NSNumber);
  self.boomerang = [TiUtils boolValue:value def:NO];
}

- (void)setScalingMode_:(id)value
{
  ENSURE_TYPE(value, NSNumber);
  NSInteger mode = [TiUtils intValue:value def:TiGifviewScalingModeAspectFit];
  switch (mode) {
  case TiGifviewScalingModeAspectFill:
    self.imageView.contentMode = UIViewContentModeScaleAspectFill;
    self.imageView.clipsToBounds = YES; // otherwise the cropped overflow would show
    break;
  case TiGifviewScalingModeFill:
    self.imageView.contentMode = UIViewContentModeScaleToFill;
    break;
  case TiGifviewScalingModeAspectFit:
  default:
    self.imageView.contentMode = UIViewContentModeScaleAspectFit;
    break;
  }
}

#pragma mark Loading

- (void)loadGIFFromURL:(NSURL *)url
{
  if (url == nil) {
    return;
  }
  self.loading = YES;
  __weak __typeof__(self) weakSelf = self;
  [TiGifviewDecoder decodeGIFAtURL:url
                             completion:^(NSArray<TiGifviewFrame *> *_Nullable frames, NSError *_Nullable error) {
                               [weakSelf handleDecodedFrames:frames error:error];
                             }];
}

- (void)loadGIFFromData:(NSData *)data
{
  if (data == nil) {
    return;
  }
  self.loading = YES;
  __weak __typeof__(self) weakSelf = self;
  [TiGifviewDecoder decodeGIFData:data
                            completion:^(NSArray<TiGifviewFrame *> *_Nullable frames, NSError *_Nullable error) {
                              [weakSelf handleDecodedFrames:frames error:error];
                            }];
}

- (void)handleDecodedFrames:(NSArray<TiGifviewFrame *> *)frames error:(NSError *)error
{
  self.loading = NO;
  if (error != nil || frames.count == 0) {
    self.pendingAutoStartAfterLoad = NO;
    [self.proxy fireEvent:@"error"
                withObject:@{
                  @"success" : @(NO),
                  @"message" : error.localizedDescription ?: @"Unable to decode GIF"
                }];
    return;
  }

  [self invalidateDisplayLink];
  self.frames = frames;
  self.frameIndex = 0;
  self.direction = TiGifviewDirectionForward;
  self.completedLoops = 0;
  self.imageView.image = frames.firstObject.image;

  [self.proxy fireEvent:@"load"
              withObject:@{
                @"success" : @(YES),
                @"frameCount" : @(frames.count)
              }];

  if (self.autoStart || self.pendingAutoStartAfterLoad) {
    self.pendingAutoStartAfterLoad = NO;
    [self play];
  }
}

#pragma mark Playback control

- (void)play
{
  if (self.loading || self.frames.count == 0) {
    // A new URL may be decoding while frames from the previous GIF are still
    // available. Remember the intent so
    // -handleDecodedFrames:error: honors it once frames are ready, even
    // when autoStart is NO.
    self.pendingAutoStartAfterLoad = YES;
    return;
  }
  if (self.frames.count <= 1) {
    return; // nothing to animate
  }

  self.paused = NO;
  self.playing = YES;

  if (self.displayLink == nil) {
    self.displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(tick:)];
    [self.displayLink addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSRunLoopCommonModes];
  }
  self.displayLink.paused = NO;
}

- (void)pause
{
  if (!self.playing) {
    return;
  }
  self.paused = YES;
  self.playing = NO;
  self.displayLink.paused = YES;
}

- (void)resume
{
  if (self.frames.count == 0) {
    self.pendingAutoStartAfterLoad = YES;
    return;
  }
  if (!self.paused) {
    return;
  }
  self.paused = NO;
  self.playing = YES;
  self.displayLink.paused = NO;
}

- (void)stop
{
  self.playing = NO;
  self.paused = NO;
  self.pendingAutoStartAfterLoad = NO;
  [self invalidateDisplayLink];
  self.frameIndex = 0;
  self.direction = TiGifviewDirectionForward;
  self.accumulatedTime = 0;
  self.completedLoops = 0;
  if (self.frames.count > 0) {
    self.imageView.image = self.frames.firstObject.image;
  }
}

- (void)invalidateDisplayLink
{
  [self.displayLink invalidate];
  self.displayLink = nil;
}

#pragma mark Driver

- (void)tick:(CADisplayLink *)link
{
  if (self.frames.count == 0) {
    return;
  }

  self.accumulatedTime += link.duration;

  TiGifviewFrame *currentFrame = self.frames[self.frameIndex];
  if (self.accumulatedTime < currentFrame.delay) {
    return;
  }
  self.accumulatedTime -= currentFrame.delay;

  [self advanceFrame];
}

- (void)advanceFrame
{
  NSInteger nextIndex = (NSInteger)self.frameIndex + self.direction;
  BOOL reachedEnd = (self.direction == TiGifviewDirectionForward && nextIndex >= (NSInteger)self.frames.count);
  BOOL reachedStart = (self.direction == TiGifviewDirectionBackward && nextIndex < 0);

  if (reachedEnd || reachedStart) {
    // One full pass finished.
    if (self.boomerang) {
      // Reverse direction and step back into range instead of restarting
      // from frame 0, so the motion actually "bounces" rather than jumping.
      self.direction = (self.direction == TiGifviewDirectionForward) ? TiGifviewDirectionBackward : TiGifviewDirectionForward;
      nextIndex = (NSInteger)self.frameIndex + self.direction;
      nextIndex = MAX(0, MIN(nextIndex, (NSInteger)self.frames.count - 1));

      // A boomerang loop is complete once we've gone forward-then-back
      // (i.e. we just turned around at the start again after a backward run).
      if (reachedStart) {
        [self handleLoopCompleted];
        if (!self.playing) {
          return; // stopped by loop-limit handling below
        }
      }
    } else {
      nextIndex = 0;
      [self handleLoopCompleted];
      if (!self.playing) {
        return;
      }
    }
  }

  self.frameIndex = (NSUInteger)nextIndex;
  self.imageView.image = self.frames[self.frameIndex].image;

  [self.proxy fireEvent:@"frame"
              withObject:@{
                @"index" : @(self.frameIndex),
                @"frameCount" : @(self.frames.count)
              }];
}

- (void)handleLoopCompleted
{
  self.completedLoops += 1;

  BOOL finished = (self.loopCount > 0 && self.completedLoops >= self.loopCount);

  [self.proxy fireEvent:@"complete"
              withObject:@{
                @"loopCount" : @(self.completedLoops),
                @"finished" : @(finished)
              }];

  if (finished) {
    self.playing = NO;
    self.paused = NO;
    self.displayLink.paused = YES;
  }
}

#pragma mark Accessors used by the proxy

- (NSUInteger)currentFrame
{
  return self.frameIndex;
}

- (NSUInteger)frameCount
{
  return self.frames.count;
}

- (BOOL)isPlaying
{
  return self.playing;
}

- (BOOL)isPaused
{
  return self.paused;
}

@end
