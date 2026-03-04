(function () {

  /* ──────────────────────────────────────────────────────────
   * 1. PAGE TRANSITION
   * ────────────────────────────────────────────────────────── */
  function buildTransitionOverlay() {
    var el = document.createElement('div');
    el.id = 'page-transition';
    el.innerHTML =
      '<div class="pt-logo">' +
      '  <div class="pt-seal">' +
      '    <svg width="32" height="32" viewBox="0 0 36 36" fill="none">' +
      '      <circle cx="18" cy="18" r="15" stroke="rgba(255,255,255,0.3)" stroke-width="1.5" stroke-dasharray="4 3"/>' +
      '      <text x="18" y="23" text-anchor="middle" fill="rgba(255,255,255,0.6)" font-size="14">⚖</text>' +
      '    </svg>' +
      '  </div>' +
      '  <div class="pt-text">BlockBallot</div>' +
      '</div>';
    document.body.appendChild(el);
    return el;
  }

  var transitionEl = buildTransitionOverlay();
  var inTransition = false;

  function navigateTo(href) {
    if (inTransition) return;
    inTransition = true;
    transitionEl.classList.add('entering');
    setTimeout(function () {
      window.location.href = href;
    }, 500);
  }

  // Intercept all nav links
  document.addEventListener('click', function (e) {
    var link = e.target.closest('a[href]');
    if (!link) return;
    var href = link.getAttribute('href');
    if (!href || href.startsWith('#') || href.startsWith('http') || link.target === '_blank') return;
    e.preventDefault();
    navigateTo(href);
  });

  // Fade in on load
  window.addEventListener('pageshow', function () {
    transitionEl.classList.remove('entering');
    inTransition = false;
  });

  /* ──────────────────────────────────────────────────────────
   * 2. VOTE-SEAL ANIMATION (before form submit)
   * ────────────────────────────────────────────────────────── */
  function buildSealOverlay() {
    var el = document.createElement('div');
    el.id = 'vote-seal-overlay';
    el.innerHTML =
      '<div class="seal-ring">' +
      '  <svg viewBox="0 0 140 140" fill="none" xmlns="http://www.w3.org/2000/svg">' +
      '    <circle class="ring-outer" cx="70" cy="70" r="65" stroke="rgba(255,255,255,0.25)" stroke-width="1.5" stroke-dasharray="8 6" opacity="0.6"/>' +
      '    <circle class="ring-inner" cx="70" cy="70" r="50" stroke="rgba(255,255,255,0.4)" stroke-width="1" stroke-dasharray="5 8" opacity="0.4"/>' +
      '    <circle cx="70" cy="70" r="36" fill="#0a0a0a" stroke="rgba(255,255,255,0.3)" stroke-width="1.5"/>' +
      '    <polyline class="seal-checkmark" points="52,70 65,83 90,55"' +
      '      stroke="#f5f5f5" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" fill="none"/>' +
      '  </svg>' +
      '</div>' +
      '<div class="seal-label">Vote Sealed</div>' +
      '<div class="seal-sub">Recording to blockchain…</div>';
    document.body.appendChild(el);
    return el;
  }

  var sealOverlay = buildSealOverlay();

  function playSealAnimation(callback) {
    sealOverlay.classList.add('active');
    var check = sealOverlay.querySelector('.seal-checkmark');
    var label = sealOverlay.querySelector('.seal-label');
    var sub = sealOverlay.querySelector('.seal-sub');

    requestAnimationFrame(function () {
      check.classList.add('draw');
      setTimeout(function () { label.classList.add('show'); }, 900);
      setTimeout(function () { sub.classList.add('show'); }, 1200);
      setTimeout(function () { callback(); }, 2200);
    });
  }

  // Hook into vote form
  var voteForm = document.getElementById('voteForm');
  if (voteForm) {
    voteForm.addEventListener('submit', function (e) {
      var selectedRadio = voteForm.querySelector('input[name="selectedOptionId"]:checked');
      var selectedSelect = voteForm.querySelector('select[name="selectedOptionId"]');
      if (!selectedRadio && (!selectedSelect || !selectedSelect.value)) return;
      e.preventDefault();
      playSealAnimation(function () {
        voteForm.submit();
      });
    });
  }

  /* ──────────────────────────────────────────────────────────
   * 3. RESULTS BARS
   * ────────────────────────────────────────────────────────── */
  function initResultsBars() {
    var rows = Array.prototype.slice.call(document.querySelectorAll('.result-row'));
    if (!rows.length) return;
    var maxVotes = 0;
    rows.forEach(function (r) {
      var v = Number(r.getAttribute('data-votes') || 0);
      if (v > maxVotes) maxVotes = v;
    });
    rows.forEach(function (r, i) {
      var v = Number(r.getAttribute('data-votes') || 0);
      var fill = r.querySelector('.bar i');
      if (!fill) return;
      var w = maxVotes > 0 ? (v / maxVotes) * 100 : 0;
      r.style.animationDelay = (i * 0.1) + 's';
      setTimeout(function () {
        fill.style.width = w + '%';
      }, 300 + i * 120);

      // Leading badge
      if (maxVotes > 0 && v === maxVotes) {
        var nameEl = r.querySelector('.result-name');
        if (nameEl) {
          var badge = document.createElement('span');
          badge.className = 'leading-badge';
          badge.textContent = 'LEADING';
          nameEl.appendChild(badge);
        }
      }
    });
  }

  /* ──────────────────────────────────────────────────────────
   * 4. COPY BUTTONS
   * ────────────────────────────────────────────────────────── */
  function initCopyButtons() {
    document.querySelectorAll('[data-copy]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var text = btn.getAttribute('data-copy') || '';
        if (!text) return;
        navigator.clipboard.writeText(text).then(function () {
          var orig = btn.textContent;
          btn.textContent = 'Copied ✓';
          setTimeout(function () { btn.textContent = orig; }, 1200);
        }).catch(function () {
          window.prompt('Copy:', text);
        });
      });
    });
  }

  /* ──────────────────────────────────────────────────────────
   * 5. LEDGER SEARCH
   * ────────────────────────────────────────────────────────── */
  function initLedgerSearch() {
    var input = document.getElementById('ledgerSearch');
    var clear = document.getElementById('clearSearch');
    var rowsRoot = document.getElementById('ledgerRows');
    if (!input || !rowsRoot) return;
    var rows = Array.prototype.slice.call(rowsRoot.querySelectorAll('tr'));
    function applyFilter() {
      var term = input.value.trim().toLowerCase();
      rows.forEach(function (r) {
        r.style.display = r.textContent.toLowerCase().indexOf(term) >= 0 ? '' : 'none';
      });
    }
    input.addEventListener('input', applyFilter);
    if (clear) {
      clear.addEventListener('click', function () {
        input.value = '';
        applyFilter();
        input.focus();
      });
    }
  }

  /* ──────────────────────────────────────────────────────────
   * 6. WEBGL MONOCHROME PARTICLE NETWORK (Three.js)
   * ────────────────────────────────────────────────────────── */
  function initWebGL() {
    var canvas = document.createElement('canvas');
    canvas.id = 'webgl-canvas';
    canvas.style.cssText = 'position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:0;pointer-events:none;';
    document.body.prepend(canvas);

    var script = document.createElement('script');
    script.src = 'https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js';
    script.onload = function () { startScene(canvas); };
    document.head.appendChild(script);
  }

  function startScene(canvas) {
    var W = window.innerWidth, H = window.innerHeight;
    var scene = new THREE.Scene();
    var camera = new THREE.PerspectiveCamera(60, W / H, 0.1, 2000);
    camera.position.z = 500;

    var renderer = new THREE.WebGLRenderer({ canvas: canvas, alpha: true, antialias: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(W, H);
    renderer.setClearColor(0x000000, 0);

    /* ── Particles ── */
    var COUNT = 800;
    var posArr = new Float32Array(COUNT * 3);
    var sizes = new Float32Array(COUNT);
    for (var i = 0; i < COUNT; i++) {
      posArr[i * 3] = (Math.random() - .5) * 1800;
      posArr[i * 3 + 1] = (Math.random() - .5) * 1200;
      posArr[i * 3 + 2] = (Math.random() - .5) * 600;
      sizes[i] = Math.random() * 4 + 1.5;
    }
    var geo = new THREE.BufferGeometry();
    geo.setAttribute('position', new THREE.BufferAttribute(posArr, 3));
    geo.setAttribute('size', new THREE.BufferAttribute(sizes, 1));

    // Monochrome sprite texture
    var tc = document.createElement('canvas');
    tc.width = tc.height = 64;
    var ctx = tc.getContext('2d');
    var g = ctx.createRadialGradient(32, 32, 0, 32, 32, 32);
    g.addColorStop(0, 'rgba(255,255,255,1)');
    g.addColorStop(0.3, 'rgba(255,255,255,0.4)');
    g.addColorStop(1, 'rgba(255,255,255,0)');
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, 64, 64);
    var tex = new THREE.CanvasTexture(tc);

    var mat = new THREE.PointsMaterial({
      size: 8,
      map: tex,
      transparent: true,
      opacity: 0.3,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
      sizeAttenuation: true
    });
    var particles = new THREE.Points(geo, mat);
    scene.add(particles);

    /* ── Connection lines (network) ── */
    var lineGeo = new THREE.BufferGeometry();
    var lineVerts = [];
    var NODES = 70, nodePos = [];
    for (var n = 0; n < NODES; n++) {
      nodePos.push(
        (Math.random() - .5) * 1800,
        (Math.random() - .5) * 1200,
        (Math.random() - .5) * 300
      );
    }
    for (var a = 0; a < NODES; a++) {
      for (var b = a + 1; b < NODES; b++) {
        var dx = nodePos[a * 3] - nodePos[b * 3];
        var dy = nodePos[a * 3 + 1] - nodePos[b * 3 + 1];
        var dz = nodePos[a * 3 + 2] - nodePos[b * 3 + 2];
        var dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 350) {
          lineVerts.push(
            nodePos[a * 3], nodePos[a * 3 + 1], nodePos[a * 3 + 2],
            nodePos[b * 3], nodePos[b * 3 + 1], nodePos[b * 3 + 2]
          );
        }
      }
    }
    lineGeo.setAttribute('position', new THREE.BufferAttribute(new Float32Array(lineVerts), 3));
    var lineMat = new THREE.LineBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0.04 });
    var lines = new THREE.LineSegments(lineGeo, lineMat);
    scene.add(lines);

    /* ── Mouse parallax ── */
    var mouse = { x: 0, y: 0 };
    var target = { x: 0, y: 0 };
    document.addEventListener('mousemove', function (e) {
      mouse.x = (e.clientX / window.innerWidth - .5) * 2;
      mouse.y = (e.clientY / window.innerHeight - .5) * 2;
    });

    /* ── Scroll parallax ── */
    var scrollY = 0;
    window.addEventListener('scroll', function () { scrollY = window.scrollY; });

    window.addEventListener('resize', function () {
      W = window.innerWidth; H = window.innerHeight;
      camera.aspect = W / H;
      camera.updateProjectionMatrix();
      renderer.setSize(W, H);
    });

    var clock = new THREE.Clock();
    (function animate() {
      requestAnimationFrame(animate);
      var t = clock.getElapsedTime();

      // Ease mouse
      target.x += (mouse.x - target.x) * 0.03;
      target.y += (mouse.y - target.y) * 0.03;

      // Camera parallax
      camera.position.x = target.x * 50;
      camera.position.y = -target.y * 35 - scrollY * 0.04;
      camera.lookAt(scene.position);

      // Gentle rotation
      particles.rotation.y = t * 0.015;
      particles.rotation.x = t * 0.008;
      lines.rotation.y = t * 0.01;
      lines.rotation.x = t * 0.005;

      // Subtle pulsing opacity
      mat.opacity = 0.25 + Math.sin(t * 0.6) * 0.08;

      renderer.render(scene, camera);
    })();
  }

  /* ──────────────────────────────────────────────────────────
   * INIT
   * ────────────────────────────────────────────────────────── */
  document.addEventListener('DOMContentLoaded', function () {
    initResultsBars();
    initCopyButtons();
    initLedgerSearch();
    initWebGL();
  });

})();
