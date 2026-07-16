/**
 * TiGifviewGifViewProxy
 *
 * Your Copyright Here
 */
#import "TiGifviewGifViewProxy.h"
#import "TiGifviewGifView.h"
#import "TiUtils.h"

@implementation TiGifviewGifViewProxy

#pragma mark View creation

- (TiUIView *)newView
{
  return [[TiGifviewGifView alloc] init];
}

- (TiGifviewGifView *)gifView
{
  return (TiGifviewGifView *)self.view;
}

#pragma mark JS-callable playback methods

- (void)start:(id)args
{
  TiThreadPerformOnMainThread(^{
    [[self gifView] play];
  },
      NO);
}

- (void)play:(id)args
{
  [self start:args];
}

- (void)pause:(id)args
{
  TiThreadPerformOnMainThread(^{
    [[self gifView] pause];
  },
      NO);
}

- (void)resume:(id)args
{
  TiThreadPerformOnMainThread(^{
    [[self gifView] resume];
  },
      NO);
}

- (void)stop:(id)args
{
  TiThreadPerformOnMainThread(^{
    [[self gifView] stop];
  },
      NO);
}

#pragma mark JS-readable state

- (id)currentFrame:(id)args
{
  return @([self gifView].currentFrame);
}

- (id)frameCount:(id)args
{
  return @([self gifView].frameCount);
}

- (id)playing:(id)args
{
  return @([self gifView].isPlaying);
}

- (id)paused:(id)args
{
  return @([self gifView].isPaused);
}

@end
