/**
 * TiGifviewDecoder
 *
 * Your Copyright Here
 */
#import "TiGifviewDecoder.h"
#import <ImageIO/ImageIO.h>

NSString *const TiGifviewErrorDomain = @"TiGifviewErrorDomain";

@implementation TiGifviewFrame
@end

@implementation TiGifviewDecoder

+ (dispatch_queue_t)decodeQueue
{
  static dispatch_queue_t queue;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    queue = dispatch_queue_create("ti.gifview.decode", DISPATCH_QUEUE_SERIAL);
  });
  return queue;
}

+ (NSTimeInterval)delaySecondsForImageAtIndex:(size_t)index source:(CGImageSourceRef)source
{
  // GIF stores per-frame delay in centiseconds; ImageIO exposes it to us in
  // seconds already via kCGImagePropertyGIFDelayTime / UnclampedDelayTime.
  NSTimeInterval delay = 0.1; // sane fallback matching common GIF encoders
  CFDictionaryRef properties = CGImageSourceCopyPropertiesAtIndex(source, index, NULL);
  if (properties) {
    CFDictionaryRef gifProperties = CFDictionaryGetValue(properties, kCGImagePropertyGIFDictionary);
    if (gifProperties) {
      NSNumber *unclamped = (__bridge NSNumber *)CFDictionaryGetValue(gifProperties, kCGImagePropertyGIFUnclampedDelayTime);
      NSNumber *clamped = (__bridge NSNumber *)CFDictionaryGetValue(gifProperties, kCGImagePropertyGIFDelayTime);
      NSNumber *chosen = unclamped ?: clamped;
      if (chosen != nil && chosen.doubleValue > 0) {
        delay = chosen.doubleValue;
      }
    }
    CFRelease(properties);
  }
  // Guard against pathologically small delays some GIF encoders emit,
  // which would otherwise spin the CADisplayLink needlessly fast.
  return MAX(delay, 0.02);
}

+ (void)decodeGIFData:(NSData *)data
            completion:(void (^)(NSArray<TiGifviewFrame *> *_Nullable, NSError *_Nullable))completion
{
  if (data == nil || data.length == 0) {
    NSError *error = [NSError errorWithDomain:TiGifviewErrorDomain
                                          code:TiGifviewErrorInvalidData
                                      userInfo:@{ NSLocalizedDescriptionKey : @"No GIF data provided" }];
    dispatch_async(dispatch_get_main_queue(), ^{
      completion(nil, error);
    });
    return;
  }

  dispatch_async([self decodeQueue], ^{
    CGImageSourceRef source = CGImageSourceCreateWithData((__bridge CFDataRef)data, NULL);
    if (source == NULL) {
      NSError *error = [NSError errorWithDomain:TiGifviewErrorDomain
                                            code:TiGifviewErrorInvalidData
                                        userInfo:@{ NSLocalizedDescriptionKey : @"Data is not a valid image source" }];
      dispatch_async(dispatch_get_main_queue(), ^{
        completion(nil, error);
      });
      return;
    }

    size_t count = CGImageSourceGetCount(source);
    NSMutableArray<TiGifviewFrame *> *frames = [NSMutableArray arrayWithCapacity:count];

    for (size_t i = 0; i < count; i++) {
      CGImageRef cgImage = CGImageSourceCreateImageAtIndex(source, i, NULL);
      if (cgImage == NULL) {
        continue;
      }
      TiGifviewFrame *frame = [[TiGifviewFrame alloc] init];
      frame.image = [UIImage imageWithCGImage:cgImage scale:[UIScreen mainScreen].scale orientation:UIImageOrientationUp];
      frame.delay = [self delaySecondsForImageAtIndex:i source:source];
      [frames addObject:frame];
      CGImageRelease(cgImage);
    }

    CFRelease(source);

    if (frames.count == 0) {
      NSError *error = [NSError errorWithDomain:TiGifviewErrorDomain
                                            code:TiGifviewErrorEmptySource
                                        userInfo:@{ NSLocalizedDescriptionKey : @"GIF contained no readable frames" }];
      dispatch_async(dispatch_get_main_queue(), ^{
        completion(nil, error);
      });
      return;
    }

    dispatch_async(dispatch_get_main_queue(), ^{
      completion(frames, nil);
    });
  });
}

+ (void)decodeGIFAtURL:(NSURL *)url
             completion:(void (^)(NSArray<TiGifviewFrame *> *_Nullable, NSError *_Nullable))completion
{
  if (url == nil) {
    NSError *error = [NSError errorWithDomain:TiGifviewErrorDomain
                                          code:TiGifviewErrorInvalidData
                                      userInfo:@{ NSLocalizedDescriptionKey : @"No URL provided" }];
    dispatch_async(dispatch_get_main_queue(), ^{
      completion(nil, error);
    });
    return;
  }

  if (url.isFileURL) {
    dispatch_async([self decodeQueue], ^{
      NSError *readError = nil;
      NSData *data = [NSData dataWithContentsOfURL:url options:NSDataReadingMappedIfSafe error:&readError];
      if (data == nil) {
        dispatch_async(dispatch_get_main_queue(), ^{
          completion(nil, readError);
        });
        return;
      }
      [self decodeGIFData:data completion:completion];
    });
    return;
  }

  // Remote URL: fetch first, then reuse the data decode path.
  NSURLSessionDataTask *task = [[NSURLSession sharedSession]
      dataTaskWithURL:url
    completionHandler:^(NSData *_Nullable data, NSURLResponse *_Nullable response, NSError *_Nullable error) {
      if (error != nil || data == nil) {
        NSError *downloadError = error ?: [NSError errorWithDomain:TiGifviewErrorDomain
                                                                code:TiGifviewErrorDownloadFailed
                                                            userInfo:@{ NSLocalizedDescriptionKey : @"Failed to download GIF" }];
        dispatch_async(dispatch_get_main_queue(), ^{
          completion(nil, downloadError);
        });
        return;
      }
      [self decodeGIFData:data completion:completion];
    }];
  [task resume];
}

@end
