/**
 * Ti.GifView
 *
 * Your Copyright Here
 */
#import "TiGifviewModule.h"
#import "TiBase.h"
#import "TiHost.h"
#import "TiUtils.h"
#import "TiGifviewGifViewProxy.h"

@implementation TiGifviewModule

#pragma mark Internal

- (id)moduleGUID
{
  return @"73d0946e-19fe-44b1-bbde-89829b6872ab";
}

- (NSString *)moduleId
{
  return @"ti.gifview";
}

#pragma mark Lifecycle

- (void)startup
{
  [super startup];
  DebugLog(@"[DEBUG] %@ loaded", self);
}

#pragma mark Public APIs

- (id)createGifView:(id)args
{
  TiGifviewGifViewProxy *viewProxy = [[TiGifviewGifViewProxy alloc] _initWithPageContext:[self pageContext] args:args];
  return viewProxy;
}

#pragma mark - Constants

// Values must stay in sync with TiGifviewScalingMode in TiGifviewGifView.h
MAKE_SYSTEM_PROP(IMAGE_SCALING_ASPECT_FIT, 0); // default: fit inside bounds, keep aspect ratio, may letterbox
MAKE_SYSTEM_PROP(IMAGE_SCALING_ASPECT_FILL, 1); // fill bounds, keep aspect ratio, may crop
MAKE_SYSTEM_PROP(IMAGE_SCALING_FILL, 2); // stretch to exactly fill bounds, ignores aspect ratio

@end
