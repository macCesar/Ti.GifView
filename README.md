# Ti.GifView

Ti.GifView is a native Titanium module for GIF playback on iOS and Android. Use it when a regular image view is not enough: you can pause on a frame, resume later, set a loop limit, or play backward when the animation reaches the end.

![Titanium](https://img.shields.io/badge/Titanium-13.1.0+-red.svg) ![Platform](https://img.shields.io/badge/platform-iOS%20%7C%20Android-lightgrey.svg) ![License](https://img.shields.io/badge/license-MIT-blue.svg) ![Maintained](https://img.shields.io/badge/Maintained-Yes-green.svg)


## Features

- Start, pause, resume, and stop playback.
- Play a fixed number of loops or keep playing indefinitely.
- Use boomerang mode to reverse at each end instead of jumping back to frame 0.
- Choose aspect fit, aspect fill, or stretch-to-fill scaling.
- Respect the delay stored in each GIF frame instead of forcing one playback speed.
- Change `url` after the view has been created.
- Listen for `load`, `frame`, `complete`, and `error` events.
- Load GIFs from app assets or `http(s)://` URLs.

## Installation

1. Copy the iOS module to `modules/iphone/`, the Android module to `modules/android/`, or both.
2. Add the platforms you use to `tiapp.xml`:

```xml
<modules>
    <module platform="iphone">ti.gifview</module>
    <module platform="android">ti.gifview</module>
</modules>
```

## Quick start

```javascript
const GifView = require('ti.gifview');

const gif = GifView.createGifView({
    url: 'myAnimation.gif',
    top: 40,
    width: 300,
    height: 300,
    autoStart: true,
    loop: 0,          // 0 = infinite
    boomerang: false,
    scalingMode: GifView.IMAGE_SCALING_ASPECT_FIT
});

gif.addEventListener('load', (e) => {
    console.log(`GIF loaded, ${e.frameCount} frames`);
});

gif.addEventListener('complete', (e) => {
    console.log(`Loop ${e.loopCount} complete`, e.finished ? '(finished)' : '');
});

win.add(gif);
```

## API reference

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `url` | String | Path or URL to the GIF. Accepts local app assets (`'myAnimation.gif'`) or remote `http(s)://` URLs. You can set it during creation or later. Assigning a new value loads that GIF; playback follows `autoStart` or a pending `start()` call. |
| `autoStart` | Boolean | Whether playback starts automatically once the GIF finishes decoding. Default: `true`. |
| `loop` | Number | Number of times to loop before stopping. `0` = infinite. Default: `0`. |
| `boomerang` | Boolean | When `true`, playback reverses direction at each end instead of restarting from frame 0 (ping-pong). Default: `false`. |
| `scalingMode` | Number | One of the `IMAGE_SCALING_*` constants. Default: `GifView.IMAGE_SCALING_ASPECT_FIT`. |

### Methods

#### `start()` / `play()`

Starts playback. A call made while the GIF is still decoding is queued until the frames are ready, even when `autoStart` is `false`.

```javascript
gif.start();
```

#### `pause()`

Freezes playback at the current frame.

```javascript
gif.pause();
```

#### `resume()`

Resumes playback from where it was paused.

```javascript
gif.resume();
```

#### `stop()`

Stops playback and resets to frame 0. Also cancels any pending `start()` call that hasn't been honored yet.

```javascript
gif.stop();
```

#### `currentFrame()`

Returns the index of the currently displayed frame.

```javascript
const index = gif.currentFrame();
```

#### `frameCount()`

Returns the total number of frames in the loaded GIF (`0` if nothing has loaded yet).

```javascript
const total = gif.frameCount();
```

#### `playing()`

Returns `true` if the GIF is currently animating.

```javascript
if (gif.playing()) { /* ... */ }
```

#### `paused()`

Returns `true` if playback is paused (as opposed to stopped).

```javascript
if (gif.paused()) { /* ... */ }
```

`currentFrame`, `frameCount`, `playing`, and `paused` are methods, not properties. Call them with `()`: use `gif.frameCount()`, not `gif.frameCount`.

### Events

#### `load`

Fires after the GIF has finished decoding.

```javascript
gif.addEventListener('load', (e) => {
    console.log('frames:', e.frameCount);
});
```

Event fields:

- `success` (Boolean)
- `frameCount` (Number)

#### `frame`

Fires whenever the displayed frame changes. This can happen often, so keep the handler light.

```javascript
gif.addEventListener('frame', (e) => {
    console.log('frame:', e.index, '/', e.frameCount);
});
```

Event fields:

- `index` (Number): index of the frame now being shown
- `frameCount` (Number): total frames

#### `complete`

Fires when a full loop finishes.

```javascript
gif.addEventListener('complete', (e) => {
    if (e.finished) {
        console.log('Reached loop limit, stopped.');
    }
});
```

Event fields:

- `loopCount` (Number): how many loops have completed so far
- `finished` (Boolean): `true` if this was the last loop allowed by the `loop` property

#### `error`

Fires when the GIF cannot be downloaded or decoded.

```javascript
gif.addEventListener('error', (e) => {
    console.error(e.message);
});
```

Event fields:

- `success` (Boolean): always `false`
- `message` (String): reason for the failure

### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `IMAGE_SCALING_ASPECT_FIT` | 0 | Fit inside the view's bounds, keep aspect ratio, may letterbox. Default. |
| `IMAGE_SCALING_ASPECT_FILL` | 1 | Fill the view's bounds, keep aspect ratio, may crop. |
| `IMAGE_SCALING_FILL` | 2 | Stretch to exactly fill the view's bounds, ignoring aspect ratio. |

## Examples

### Deferred loading with a fade-in

Create the view without a URL. Once the URL is available, load the GIF and fade the view in:

```javascript
const gif = GifView.createGifView({
    height: Ti.UI.FILL,
    width: Ti.UI.FILL,
    autoStart: false,
    loop: 1,
    opacity: 0
});
container.add(gif);

// ... later, once you know what to show ...
gif.url = params.gif;

gif.animate({
    opacity: 1,
    duration: 450,
    curve: Ti.UI.ANIMATION_CURVE_EASE_OUT
});

gif.start();
```

### Cropped thumbnail grid

```javascript
function createThumb(url) {
    return GifView.createGifView({
        url: url,
        width: 100,
        height: 100,
        scalingMode: GifView.IMAGE_SCALING_ASPECT_FILL,
        loop: 0
    });
}
```

### Play a GIF exactly once, then swap to a static image

```javascript
const gif = GifView.createGifView({
    url: 'celebration.gif',
    loop: 1,
    width: 200,
    height: 200
});

gif.addEventListener('complete', (e) => {
    if (e.finished) {
        gif.opacity = 0; // or swap in a Ti.UI.ImageView with the last frame
    }
});

win.add(gif);
```

### Status readout

```javascript
statusLabel.addEventListener('click', () => {
    statusLabel.text = [
        `frame: ${gif.currentFrame()}/${gif.frameCount()}`,
        `playing: ${gif.playing()}`,
        `paused: ${gif.paused()}`
    ].join(' | ');
});
```

## Platform notes

The public API, defaults, events, and playback behavior are the same on both platforms.

- iOS decodes GIF data with Image I/O and advances playback with `CADisplayLink`.
- Android uses the vendored Glide 4.16.0 GIF decoder under its MIT license and advances playback with `Choreographer`.
- The Android module has no runtime Maven dependencies; its decoder is packaged with the module.

## Contributing

Bug reports and pull requests are welcome.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a pull request
