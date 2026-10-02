(function () {
  var ROOT_CLASS = 'zex-force-dark';

  function prefersDark() {
    try {
      return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    } catch (e) {
      return false;
    }
  }

  function luminanceOf(color) {
    if (!color) return null;
    var m = color.match(/rgba?\(([^)]+)\)/);
    if (!m) return null;
    var parts = m[1].split(',');
    var r = parseFloat(parts[0]);
    var g = parseFloat(parts[1]);
    var b = parseFloat(parts[2]);
    var a = parts.length >= 4 ? parseFloat(parts[3]) : 1;
    if (!isFinite(r) || !isFinite(g) || !isFinite(b)) return null;
    if (a === 0) return null;
    return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255;
  }

  // 从 body / html 及首个有背景色的可见块级元素推断页面明暗
  function pageAppearsLight() {
    var samples = [];
    var body = document.body;
    if (body) samples.push(getComputedStyle(body).backgroundColor);
    samples.push(getComputedStyle(document.documentElement).backgroundColor);
    if (body) {
      var children = body.children;
      for (var i = 0; i < children.length && samples.length < 8; i++) {
        var el = children[i];
        if (!el || !el.tagName) continue;
        var style = getComputedStyle(el);
        var bg = style.backgroundColor;
        var lum = luminanceOf(bg);
        if (lum !== null) samples.push(bg);
      }
    }
    for (var j = 0; j < samples.length; j++) {
      var l = luminanceOf(samples[j]);
      if (l !== null) return l > 0.5;
    }
    return true;
  }

  function apply() {
    var root = document.documentElement;
    if (!root) return;
    // 仅在应用为深色偏好时强制暗化
    if (!prefersDark()) {
      root.classList.remove(ROOT_CLASS);
      return;
    }
    // 网页自身已是深色主题时不再反转，避免出现黑底暗字
    if (pageAppearsLight()) {
      root.classList.add(ROOT_CLASS);
    } else {
      root.classList.remove(ROOT_CLASS);
    }
  }

  function schedule() {
    apply();
    if (document.readyState !== 'complete') {
      setTimeout(apply, 300);
      setTimeout(apply, 1200);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', schedule);
  } else {
    schedule();
  }
  window.addEventListener('load', schedule);
})();
