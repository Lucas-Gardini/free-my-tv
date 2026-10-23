/* Silent background playback, isolated from home navigation and data loading. */
var WallpaperVideo = {
  create: function (video, enabled, changed) {
    'use strict';
    var active = false, failed = false, generation = 0;
    video.style.visibility = 'hidden';
    function stop() {
      if (!active) return;
      active = false;
      generation++;
      video.style.visibility = 'hidden';
      if (changed) changed(false);
      try { video.pause(); video.removeAttribute('src'); video.load(); } catch (e) {}
    }
    function fail() { failed = true; stop(); }
    video.addEventListener('playing', function () {
      if (active) { video.style.visibility = 'visible'; if (changed) changed(true); }
    });
    video.addEventListener('error', function () { if (active) fail(); });
    return {update: function (visible) {
      if (!enabled()) { failed = false; stop(); return; }
      if (!visible) { stop(); return; }
      if (active || failed) return;
      active = true;
      var current = ++generation;
      try {
        video.muted = true;
        video.loop = true;
        video.setAttribute('src', 'backgrounds/Red_Eye_01.mp4');
        var playing = video.play();
        if (playing && playing.then) playing.then(null, function () {
          if (active && generation === current) fail();
        });
      } catch (e) { fail(); }
    }};
  }
};
