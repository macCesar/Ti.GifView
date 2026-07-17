// Ti.GifView — shared parity harness.
// This exact file must behave identically on iOS and Android.
const GifView = require('ti.gifview');

// Deliberately distinct from the local GIF so reloads are unmistakable.
const REMOTE_GIF = 'https://upload.wikimedia.org/wikipedia/commons/2/20/Newtons_cradle_animation_new.gif';
const LOCAL_GIF = 'earth.gif';
const BAD_URL = 'https://example.com/missing.gif';

const win = Ti.UI.createWindow({
	backgroundColor: '#101418',
	layout: 'vertical'
});

const status = Ti.UI.createLabel({
	top: 10,
	color: '#fff',
	font: { fontSize: 12, fontFamily: 'monospace' },
	text: 'loading…'
});

const gifHost = Ti.UI.createView({
	top: 40,
	width: 300,
	height: 180
});
win.add(gifHost);

function createGifView() {
	return GifView.createGifView({
		width: 300,
		height: 180, // Non-square on purpose: fit/fill/stretch remain distinguishable.
		url: LOCAL_GIF,
		autoStart: true,
		loop: 0,
		boomerang: false,
		scalingMode: GifView.IMAGE_SCALING_ASPECT_FIT
	});
}

let gif = createGifView();
let gifAttached = true;
attachGifListeners(gif);
gifHost.add(gif);
win.add(status);

function refreshStatus() {
	status.text = 'frame ' + gif.currentFrame() + '/' + gif.frameCount()
		+ '  playing:' + gif.playing() + '  paused:' + gif.paused();
}

function attachGifListeners(view) {
	view.addEventListener('load', function (e) {
		Ti.API.info('[gifview] load: ' + JSON.stringify(e));
		if (view === gif) refreshStatus();
	});
	view.addEventListener('frame', function (e) {
		// Per-frame logging lets us verify that teardown stops the driver.
		Ti.API.debug('[gifview] frame ' + e.index + '/' + e.frameCount);
		if (view === gif) refreshStatus();
	});
	view.addEventListener('complete', function (e) {
		Ti.API.info('[gifview] complete: ' + JSON.stringify(e));
		if (view === gif) refreshStatus();
	});
	view.addEventListener('error', function (e) {
		Ti.API.error('[gifview] error: ' + JSON.stringify(e));
		if (view === gif) status.text = 'ERROR: ' + e.message;
	});
}

function addFreshGifView() {
	if (gifAttached) return;
	gif = createGifView();
	attachGifListeners(gif);
	gifHost.add(gif);
	gifAttached = true;
	Ti.API.info('[gifview] fresh view added');
}

function addRow(buttons) {
	const row = Ti.UI.createView({
		top: 8,
		height: 44,
		layout: 'horizontal',
		horizontalWrap: false
	});
	buttons.forEach(function (pair) {
		const button = Ti.UI.createButton({
			title: pair[0],
			left: 6,
			font: { fontSize: 12 }
		});
		button.addEventListener('click', pair[1]);
		row.add(button);
	});
	win.add(row);
}

addRow([
	['start', function () { gif.start(); }],
	['pause', function () { gif.pause(); refreshStatus(); }],
	['resume', function () { gif.resume(); }],
	['stop', function () { gif.stop(); refreshStatus(); }]
]);

addRow([
	['local', function () { gif.url = LOCAL_GIF; }],
	['remote', function () { gif.url = REMOTE_GIF; }],
	['bad URL', function () { gif.stop(); gif.url = BAD_URL; }]
]);

let boom = false;
let loopOne = false;
let scaling = 0;
addRow([
	['boomerang', function () {
		boom = !boom;
		gif.boomerang = boom;
		Ti.API.info('[gifview] boomerang=' + boom);
	}],
	['loop 1/∞', function () {
		loopOne = !loopOne;
		gif.loop = loopOne ? 1 : 0;
		Ti.API.info('[gifview] loop=' + (loopOne ? 1 : 0));
	}],
	['scaling', function () {
		scaling = (scaling + 1) % 3;
		gif.scalingMode = scaling; // 0 fit, 1 fill, 2 stretch
		Ti.API.info('[gifview] scalingMode=' + scaling);
	}]
]);

// Restore every precondition so the parity scenarios do not leak state.
function resetGifState() {
	if (!gifAttached) addFreshGifView();
	gif.stop();
	boom = false;
	loopOne = false;
	scaling = 0;
	gif.boomerang = false;
	gif.loop = 0;
	gif.scalingMode = GifView.IMAGE_SCALING_ASPECT_FIT;
	gif.autoStart = true;
	gif.url = LOCAL_GIF;
	// A same-value URL assignment may not trigger a reload, so start explicitly.
	// If a different URL is still decoding, start() records the pending intent.
	gif.start();
	Ti.API.info('[gifview] state reset');
}

addRow([
	['reset', resetGifState],
	['pending', function () {
		// Remember start() while a new GIF is still being decoded.
		gif.stop();
		gif.autoStart = false;
		gif.url = REMOTE_GIF;
		gif.start();
	}],
	['remove view', function () {
		if (!gifAttached) return;
		gifHost.remove(gif);
		gifAttached = false;
		Ti.API.info('[gifview] view removed — frame logs must stop');
	}],
	['add view', addFreshGifView]
]);

win.open();
