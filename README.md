# Ti.GifView

A native module for Titanium that renders animated GIFs with full playback control — play, pause, resume, stop, loop count and boomerang — instead of the "fire and forget" animation `UIImage` gives you by default.

![Titanium](https://img.shields.io/badge/Titanium-13.1.0+-red.svg) ![Platform](https://img.shields.io/badge/platform-iOS-lightgrey.svg) ![License](https://img.shields.io/badge/license-MIT-blue.svg) ![Maintained](https://img.shields.io/badge/Maintained-Yes-green.svg)


## Features

- ✅ **Full playback control**: `start()`, `pause()`, `resume()`, `stop()`
- ✅ **Loop control**: play a fixed number of times, or loop forever
- ✅ **Boomerang mode**: ping-pong playback instead of restarting from frame 0
- ✅ **Scaling modes**: aspect fit, aspect fill, or stretch to fill
- ✅ **Frame-accurate decoding**: each frame keeps its own GIF-encoded delay, driven by `CADisplayLink` — no duplicated frames, no dropped timing
- ✅ **Dynamic properties**: set or change the `url` at any time after creation, not just at construction
- ✅ **Complete event system**: `load`, `frame`, `complete`, `error`
- ✅ **Local and remote GIFs**: bundled app assets or `http(s)://` URLs

## Installation

1. Copy the module to your project's `modules/iphone/` folder
2. Add to `tiapp.xml`:

```xml
<modules>
    <module platform="iphone">ti.gifview</module>
</modules>
```

## Quick Start

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

## API Reference

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `url` | String | Path or URL to the GIF. Accepts local app assets (`'myAnimation.gif'`) or remote `http(s)://` URLs. Can be set at creation **or at any point afterward** — reassigning it loads and plays the new GIF. |
| `autoStart` | Boolean | Whether playback starts automatically once the GIF finishes decoding. Default: `true`. |
| `loop` | Number | Number of times to loop before stopping. `0` = infinite. Default: `0`. |
| `boomerang` | Boolean | When `true`, playback reverses direction at each end instead of restarting from frame 0 (ping-pong). Default: `false`. |
| `scalingMode` | Number | One of the `IMAGE_SCALING_*` constants. Default: `GifView.IMAGE_SCALING_ASPECT_FIT`. |

### Methods

#### `start()` / `play()`
Starts playback. If called before the GIF has finished decoding (e.g. right after setting `url`), the intent is remembered and honored as soon as decoding completes — even if `autoStart` is `false`.
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

> **Note:** `currentFrame`, `frameCount`, `playing` and `paused` are methods, not properties — call them with `()`, e.g. `gif.frameCount()` rather than `gif.frameCount`.

### Events

#### `load`
Fired once the GIF has finished decoding.
```javascript
gif.addEventListener('load', (e) => {
    console.log('frames:', e.frameCount);
});
```
**Event properties:**
- `success` (Boolean)
- `frameCount` (Number)

#### `frame`
Fired every time the displayed frame changes. Fires at GIF frame-rate, so avoid heavy logic in this handler.
```javascript
gif.addEventListener('frame', (e) => {
    console.log('frame:', e.index, '/', e.frameCount);
});
```
**Event properties:**
- `index` (Number): index of the frame now being shown
- `frameCount` (Number): total frames

#### `complete`
Fired every time a full loop finishes.
```javascript
gif.addEventListener('complete', (e) => {
    if (e.finished) {
        console.log('Reached loop limit, stopped.');
    }
});
```
**Event properties:**
- `loopCount` (Number): how many loops have completed so far
- `finished` (Boolean): `true` if this was the last loop allowed by the `loop` property

#### `error`
Fired if the GIF could not be downloaded or decoded.
```javascript
gif.addEventListener('error', (e) => {
    console.error(e.message);
});
```
**Event properties:**
- `success` (Boolean): always `false`
- `message` (String): reason for the failure

### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `IMAGE_SCALING_ASPECT_FIT` | 0 | Fit inside the view's bounds, keep aspect ratio, may letterbox. Default. |
| `IMAGE_SCALING_ASPECT_FILL` | 1 | Fill the view's bounds, keep aspect ratio, may crop. |
| `IMAGE_SCALING_FILL` | 2 | Stretch to exactly fill the view's bounds, ignoring aspect ratio. |

## Advanced Examples

### Deferred loading with a fade-in

Create the view empty, load a GIF later (e.g. once you know which one you need), and fade it in as soon as it starts playing:

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

### Manual scrub / status readout

```javascript
statusLabel.addEventListener('click', () => {
    statusLabel.text = [
        `frame: ${gif.currentFrame()}/${gif.frameCount()}`,
        `playing: ${gif.playing()}`,
        `paused: ${gif.paused()}`
    ].join(' | ');
});
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
