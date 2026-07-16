/**
 * TiGifviewDecoder
 *
 * Decodes an animated GIF into an ordered list of frame images and
 * per-frame delay times, so TiGifviewGifView can fully drive playback
 * (play / pause / stop / boomerang / loop count) instead of relying
 * on UIImage's built-in "fire and forget" animation.
 *
 * Your Copyright Here
 */
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

extern NSString *const TiGifviewErrorDomain;

typedef NS_ENUM(NSInteger, TiGifviewErrorCode) {
  TiGifviewErrorInvalidData = 1,
  TiGifviewErrorEmptySource = 2,
  TiGifviewErrorDownloadFailed = 3,
};

@interface TiGifviewFrame : NSObject
@property (nonatomic, strong) UIImage *image;
@property (nonatomic, assign) NSTimeInterval delay; // seconds for this specific frame
@end

@interface TiGifviewDecoder : NSObject

/**
 * Decodes GIF data on a background queue. `completion` is always called
 * back on the main queue with either a non-empty frame array, or an error.
 */
+ (void)decodeGIFData:(NSData *)data
            completion:(void (^)(NSArray<TiGifviewFrame *> *_Nullable frames, NSError *_Nullable error))completion;

/**
 * Convenience wrapper: resolves `url` (local file:// or remote http(s)://),
 * fetches the bytes if needed, then decodes them. Network fetch and decode
 * both happen off the main thread.
 */
+ (void)decodeGIFAtURL:(NSURL *)url
             completion:(void (^)(NSArray<TiGifviewFrame *> *_Nullable frames, NSError *_Nullable error))completion;

@end

NS_ASSUME_NONNULL_END
