/**
 * TiGifviewGifViewProxy
 *
 * Exposed to JS via:
 *   var GifView = require('ti.gifview');
 *   var gif = GifView.createGifView({
 *     url: 'myAnimation.gif',
 *     autoStart: true,
 *     loop: 3,        // 0 = infinite
 *     boomerang: true
 *   });
 *
 * Your Copyright Here
 */
#import "TiViewProxy.h"

@interface TiGifviewGifViewProxy : TiViewProxy

// JS-callable methods
- (void)start:(id)args;
- (void)play:(id)args; // alias for start
- (void)pause:(id)args;
- (void)resume:(id)args;
- (void)stop:(id)args;
- (id)currentFrame:(id)args;
- (id)frameCount:(id)args;
- (id)playing:(id)args;
- (id)paused:(id)args;

@end
