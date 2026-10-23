const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const context = {};
vm.createContext(context);
vm.runInContext(fs.readFileSync('web/wallpaper.js', 'utf8'), context);
let enabled = true, rejectPlay;
const events = {};
const video = {
  style: {}, plays: 0, pauses: 0, loads: 0, src: '',
  addEventListener: (name, fn) => { events[name] = fn; },
  setAttribute(name, value) { this[name] = value; },
  removeAttribute(name) { this[name] = ''; },
  play() { this.plays++; return {then(_, reject) { rejectPlay = reject; }}; },
  pause() { this.pauses++; }, load() { this.loads++; }
};
const changes = [];
const player = context.WallpaperVideo.create(video, () => enabled, (visible, pending) => changes.push({visible, pending}));
player.update(true);
player.update(true);
assert.equal(video.plays, 1, 'Repeated refresh must not start another playback session');
assert.deepEqual(changes[0], {visible: false, pending: true}, 'Hide static wallpaper while the first frame loads');
assert.equal(video.muted, true);
assert.equal(video.loop, true);
assert.notEqual(video.style.visibility, 'visible', 'Keep static until the first frame');
events.playing();
assert.equal(video.style.visibility, 'visible');
assert.deepEqual(changes[changes.length - 1], {visible: true, pending: false}, 'First frame ends the loading state');
const staleRejection = rejectPlay;
player.update(false);
assert.equal(video.src, '', 'Release decoder source while hidden or in screensaver');
assert.equal(video.style.visibility, 'hidden');
assert.deepEqual(changes[changes.length - 1], {visible: false, pending: true}, 'Pause must not reveal the static wallpaper');
player.update(true);
staleRejection();
assert.equal(video.src, 'backgrounds/Red_Eye_01.mp4', 'An old play rejection must not stop the new session');
enabled = false;
player.update(true);
assert.equal(video.src, '', 'Toggle off restores static wallpaper');
assert.deepEqual(changes[changes.length - 1], {visible: false, pending: false}, 'Toggle off reveals static wallpaper');
enabled = true;
player.update(true);
events.error();
assert.equal(video.src, '', 'Decoder failure restores static wallpaper');
assert.deepEqual(changes[changes.length - 1], {visible: false, pending: false}, 'Failure reveals static wallpaper');
const starts = video.plays;
player.update(true);
assert.equal(video.plays, starts, 'Do not repeatedly retry a broken decoder');
console.log('Web wallpaper: 16 lifecycle checks passed');
