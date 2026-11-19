(() => {
  const WIDTH = 720;
  const HEIGHT = 1280;
  const TOTAL_DURATION = 28.0; // seconds (fits 30s WhatsApp Status limit)

  function detectSiteUrl() {
    const host = window.location.hostname.toLowerCase();
    if (host.endsWith(".github.io")) {
      const segments = window.location.pathname.split("/").filter(Boolean);
      const repo = segments.length > 0 ? segments[0] : "-rinku";
      return `https://${host}/${repo}/`;
    }
    return "https://khilesh6263.github.io/-rinku/";
  }

  const SITE_URL = detectSiteUrl();
  const SITE_SHORT = SITE_URL.replace(/^https?:\/\//, "").replace(/\/$/, "");

  const SCENES = [
    {
      start: 0,
      end: 6.5,
      name: "Scene 1/4: Intro & Hook",
      voiceText:
        "Haath busy hai ya bike chala rahe ho? Ab incoming call aane par phone chhune ki bilkul zarurat nahi! Miliye Baby Call Assistant se!"
    },
    {
      start: 6.5,
      end: 14.0,
      name: "Scene 2/4: Auto-Mic Live Demo",
      voiceText:
        "Jaise hi phone ring hota hai, Baby ka mic apne aap on ho jata hai. Bas bolo Baby call attend karo, ya call attend mat karo, speaker on karo, ya call reject karo!"
    },
    {
      start: 14.0,
      end: 21.5,
      name: "Scene 3/4: 3-Step Setup Guide",
      voiceText:
        "Use karna bahut aasaan hai. Step one, website se free APK download karein. Step two, mic aur phone permission allow karke assistant on karein. Step three, incoming call par bas voice command bolein!"
    },
    {
      start: 21.5,
      end: 28.0,
      name: "Scene 4/4: Download CTA",
      voiceText:
        "To der kis baat ki? Neeche diye gaye link par click karein aur abhi Baby Call Assistant bilkul free download karein!"
    }
  ];

  const canvas = document.getElementById("promo-canvas");
  const ctx = canvas.getContext("2d");

  const boyImg = new Image();
  boyImg.src = "./assets/promo_cartoon_boy.jpg";

  const iconImg = new Image();
  iconImg.src = "./assets/baby-icon.svg";

  let isPlaying = false;
  let isRecording = false;
  let currentTime = 0;
  let lastFrameTs = 0;
  let rafId = null;
  let lastSpokenScene = -1;
  let audioCtx = null;
  let audioDest = null;
  let beatTimer = null;

  const statusTextEl = document.getElementById("promo-status");
  const progressBarEl = document.getElementById("promo-progress-fill");
  const playBtn = document.getElementById("btn-play-preview");
  const recordBtn = document.getElementById("btn-record-video");
  const voiceToggle = document.getElementById("toggle-voiceover");
  const siteUrlDisplay = document.getElementById("promo-site-url");
  const captionBox = document.getElementById("whatsapp-caption-text");

  if (siteUrlDisplay) siteUrlDisplay.textContent = SITE_URL;
  if (captionBox) {
    captionBox.value =
      `🎙️ *Baby Call Assistant — Hands-Free Incoming Call App!*\n\n` +
      `Haath busy ho, bike chala rahe ho ya kaam kar rahe ho — ab call aate hi mic apne aap ON ho jayega!\n` +
      `Bas bolo:\n` +
      `✅ "Baby, call attend karo"\n` +
      `🔕 "Baby, call attend mat karo"\n` +
      `🔊 "Baby, speaker on karo"\n` +
      `❌ "Baby, call reject karo"\n\n` +
      `👇 *Neeche link dabakar abhi FREE APK download karein:*\n` +
      `${SITE_URL}`;
  }

  function showToast(msg) {
    const toast = document.getElementById("toast");
    if (!toast) return;
    toast.textContent = msg;
    toast.classList.add("show");
    clearTimeout(window.__promoToast);
    window.__promoToast = setTimeout(() => toast.classList.remove("show"), 3000);
  }

  function clamp01(v) {
    return Math.max(0, Math.min(1, v));
  }

  function easeOutBack(x) {
    const c1 = 1.70158;
    const c3 = c1 + 1;
    return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
  }

  function drawRoundedRect(x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, y + h - r);
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    ctx.lineTo(x + r, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - r);
    ctx.lineTo(x, y + r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.closePath();
  }

  function drawWrappedText(text, cx, startY, maxWidth, lineHeight, align = "center") {
    const words = text.split(" ");
    let line = "";
    let y = startY;
    ctx.textAlign = align;
    for (let n = 0; n < words.length; n++) {
      const testLine = line + (line ? " " : "") + words[n];
      const metrics = ctx.measureText(testLine);
      if (metrics.width > maxWidth && n > 0) {
        ctx.fillText(line, cx, y);
        line = words[n];
        y += lineHeight;
      } else {
        line = testLine;
      }
    }
    if (line) {
      ctx.fillText(line, cx, y);
    }
    return y;
  }

  function drawBackground(t) {
    const bgGrad = ctx.createLinearGradient(0, 0, WIDTH, HEIGHT);
    bgGrad.addColorStop(0, "#07081A");
    bgGrad.addColorStop(0.5, "#11133A");
    bgGrad.addColorStop(1, "#060816");
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, WIDTH, HEIGHT);

    // Animated nebula glow blobs
    const blob1X = 160 + Math.sin(t * 0.7) * 55;
    const blob1Y = 260 + Math.cos(t * 0.5) * 40;
    const g1 = ctx.createRadialGradient(blob1X, blob1Y, 20, blob1X, blob1Y, 310);
    g1.addColorStop(0, "rgba(124, 58, 237, 0.34)");
    g1.addColorStop(1, "rgba(124, 58, 237, 0)");
    ctx.fillStyle = g1;
    ctx.fillRect(0, 0, WIDTH, HEIGHT);

    const blob2X = 560 + Math.cos(t * 0.6) * 50;
    const blob2Y = 920 + Math.sin(t * 0.8) * 50;
    const g2 = ctx.createRadialGradient(blob2X, blob2Y, 20, blob2X, blob2Y, 320);
    g2.addColorStop(0, "rgba(34, 211, 238, 0.25)");
    g2.addColorStop(1, "rgba(34, 211, 238, 0)");
    ctx.fillStyle = g2;
    ctx.fillRect(0, 0, WIDTH, HEIGHT);

    // Subtle floating particles
    for (let i = 0; i < 22; i++) {
      const px = ((i * 97 + t * 18) % (WIDTH + 40)) - 20;
      const py = ((i * 151 - t * 24) % HEIGHT + HEIGHT) % HEIGHT;
      const alpha = 0.18 + 0.25 * Math.sin(t * 2 + i);
      ctx.fillStyle = i % 2 === 0 ? `rgba(34, 211, 238, ${alpha})` : `rgba(167, 139, 250, ${alpha})`;
      ctx.beginPath();
      ctx.arc(px, py, 2.5 + (i % 3), 0, Math.PI * 2);
      ctx.fill();
    }

    // Top branding pill
    drawRoundedRect(140, 44, 440, 56, 28);
    ctx.fillStyle = "rgba(15, 20, 52, 0.82)";
    ctx.fill();
    ctx.strokeStyle = "rgba(34, 211, 238, 0.45)";
    ctx.lineWidth = 2;
    ctx.stroke();

    if (iconImg.complete && iconImg.naturalWidth > 0) {
      ctx.drawImage(iconImg, 158, 52, 40, 40);
    }
    ctx.fillStyle = "#F8FAFC";
    ctx.font = '700 24px "Space Grotesk", sans-serif';
    ctx.textAlign = "left";
    ctx.fillText("BABY CALL ASSISTANT", 212, 80);

    // Bottom progress strip
    const prog = clamp01(t / TOTAL_DURATION);
    ctx.fillStyle = "rgba(255,255,255,0.12)";
    ctx.fillRect(40, HEIGHT - 28, WIDTH - 80, 8);
    const pg = ctx.createLinearGradient(40, 0, WIDTH - 40, 0);
    pg.addColorStop(0, "#7C3AED");
    pg.addColorStop(1, "#22D3EE");
    ctx.fillStyle = pg;
    ctx.fillRect(40, HEIGHT - 28, (WIDTH - 80) * prog, 8);
  }

  function drawCartoonBoyCard(cx, cy, radius, t, showGlow = true) {
    const floatY = Math.sin(t * 3.4) * 8;
    const y = cy + floatY;

    if (showGlow) {
      for (let r = 0; r < 2; r++) {
        const wave = ((t * 0.9 + r * 0.5) % 1);
        ctx.beginPath();
        ctx.arc(cx, y, radius + 12 + wave * 52, 0, Math.PI * 2);
        ctx.strokeStyle = `rgba(34, 211, 238, ${(1 - wave) * 0.45})`;
        ctx.lineWidth = 4;
        ctx.stroke();
      }
    }

    // Outer neon ring
    const ringGrad = ctx.createLinearGradient(cx - radius, y - radius, cx + radius, y + radius);
    ringGrad.addColorStop(0, "#22D3EE");
    ringGrad.addColorStop(0.5, "#8B5CF6");
    ringGrad.addColorStop(1, "#EC4899");

    ctx.beginPath();
    ctx.arc(cx, y, radius + 7, 0, Math.PI * 2);
    ctx.fillStyle = ringGrad;
    ctx.fill();

    // Clipped cartoon boy image
    ctx.save();
    ctx.beginPath();
    ctx.arc(cx, y, radius, 0, Math.PI * 2);
    ctx.clip();
    if (boyImg.complete && boyImg.naturalWidth > 0) {
      const scale = Math.max((radius * 2) / boyImg.naturalWidth, (radius * 2) / boyImg.naturalHeight);
      const dw = boyImg.naturalWidth * scale;
      const dh = boyImg.naturalHeight * scale;
      ctx.drawImage(boyImg, cx - dw / 2, y - dh / 2 + 12, dw, dh);
    } else {
      ctx.fillStyle = "#1E1B4B";
      ctx.fillRect(cx - radius, y - radius, radius * 2, radius * 2);
    }
    ctx.restore();
  }

  function drawSpeechBubble(x, y, w, h, title, body, pointerX) {
    drawRoundedRect(x, y, w, h, 28);
    ctx.fillStyle = "rgba(18, 24, 58, 0.94)";
    ctx.fill();
    ctx.strokeStyle = "rgba(34, 211, 238, 0.7)";
    ctx.lineWidth = 3;
    ctx.stroke();

    if (typeof pointerX === "number") {
      ctx.beginPath();
      ctx.moveTo(pointerX - 18, y);
      ctx.lineTo(pointerX, y - 22);
      ctx.lineTo(pointerX + 18, y);
      ctx.closePath();
      ctx.fillStyle = "rgba(18, 24, 58, 0.94)";
      ctx.fill();
      ctx.stroke();
    }

    ctx.fillStyle = "#22D3EE";
    ctx.font = '800 26px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText(title, x + w / 2, y + 48);

    ctx.fillStyle = "#F8FAFC";
    ctx.font = '600 28px "Plus Jakarta Sans", sans-serif';
    drawWrappedText(body, x + w / 2, y + 96, w - 56, 40, "center");
  }

  function drawScene1(t) {
    const local = t;
    const pop = easeOutBack(clamp01(local / 0.7));

    ctx.save();
    ctx.translate(WIDTH / 2, 175);
    ctx.scale(pop, pop);
    ctx.fillStyle = "#22D3EE";
    ctx.font = '800 28px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("HANDS BUSY? BIKE CHALA RAHE HO?", 0, 0);
    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 46px "Space Grotesk", sans-serif';
    ctx.fillText("Bina Phone Touch Kiye", 0, 56);
    ctx.fillText("Call Control Karein!", 0, 110);
    ctx.restore();

    drawCartoonBoyCard(WIDTH / 2, 560, 215, t, true);

    const bubbleAlpha = clamp01((local - 0.6) / 0.5);
    ctx.save();
    ctx.globalAlpha = bubbleAlpha;
    drawSpeechBubble(
      56,
      835,
      WIDTH - 112,
      250,
      "CARTOON GUIDE SAYS:",
      "“Ab incoming call aate hi mic apne aap ON ho jayega — Hindi, Hinglish ya English mein bas bolkar call uthao!”",
      WIDTH / 2
    );
    ctx.restore();

    // Feature badges
    const tags = ["🎙️ Auto-Mic ON", "🇮🇳 Hindi + English", "🔒 100% Free"];
    tags.forEach((tag, i) => {
      const bx = 56 + i * 208;
      drawRoundedRect(bx, 1125, 192, 54, 27);
      ctx.fillStyle = "rgba(124, 58, 237, 0.28)";
      ctx.fill();
      ctx.strokeStyle = "rgba(167, 139, 250, 0.55)";
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.fillStyle = "#F8FAFC";
      ctx.font = '700 20px "Plus Jakarta Sans", sans-serif';
      ctx.textAlign = "center";
      ctx.fillText(tag, bx + 96, 1160);
    });
  }

  function drawScene2(t) {
    const local = t - 6.5;

    // Header + small cartoon presenter
    drawCartoonBoyCard(124, 200, 68, t, false);
    ctx.textAlign = "left";
    ctx.fillStyle = "#22D3EE";
    ctx.font = '800 24px "Space Grotesk", sans-serif';
    ctx.fillText("DEKHO KAISE KAAM KARTA HAI", 216, 178);
    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 36px "Space Grotesk", sans-serif';
    ctx.fillText("Call Aate Hi Auto-Mic ON!", 216, 224);

    // Animated Phone Frame
    const px = 90;
    const py = 305;
    const pw = 540;
    const ph = 700;
    drawRoundedRect(px, py, pw, ph, 44);
    ctx.fillStyle = "rgba(12, 16, 42, 0.96)";
    ctx.fill();
    ctx.strokeStyle = "#22D3EE";
    ctx.lineWidth = 4;
    ctx.stroke();

    // Notch
    drawRoundedRect(WIDTH / 2 - 64, py + 16, 128, 22, 11);
    ctx.fillStyle = "#04050D";
    ctx.fill();

    // Status pill inside phone
    const isConnected = local > 2.4;
    drawRoundedRect(WIDTH / 2 - 190, py + 62, 380, 48, 24);
    ctx.fillStyle = isConnected ? "rgba(16, 185, 129, 0.22)" : "rgba(34, 211, 238, 0.2)";
    ctx.fill();
    ctx.strokeStyle = isConnected ? "#10B981" : "#22D3EE";
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.fillStyle = isConnected ? "#6EE7B7" : "#22D3EE";
    ctx.font = '800 21px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText(
      isConnected ? "✅ CALL CONNECTED HANDS-FREE" : "🎙️ INCOMING CALL • AUTO-MIC ON",
      WIDTH / 2,
      py + 94
    );

    ctx.fillStyle = "#FFFFFF";
    ctx.font = '700 34px "Space Grotesk", sans-serif';
    ctx.fillText("Mom (+91 98••• ••210)", WIDTH / 2, py + 160);

    // Glowing mic waveform inside phone
    for (let i = 0; i < 11; i++) {
      const barH = 18 + Math.abs(Math.sin(t * 7 + i * 0.7)) * 56;
      const bx = WIDTH / 2 - 132 + i * 24;
      drawRoundedRect(bx, py + 235 - barH / 2, 12, barH, 6);
      const barGrad = ctx.createLinearGradient(0, py + 190, 0, py + 280);
      barGrad.addColorStop(0, "#22D3EE");
      barGrad.addColorStop(1, "#8B5CF6");
      ctx.fillStyle = barGrad;
      ctx.fill();
    }

    // Popping voice commands list
    const commands = [
      { delay: 0.7, phrase: '🗣️ "Baby, call attend karo"', badge: "ANSWER", color: "#10B981" },
      { delay: 2.3, phrase: '🗣️ "Baby, call attend mat karo"', badge: "IGNORE", color: "#F59E0B" },
      { delay: 3.9, phrase: '🗣️ "Baby, speaker on karo"', badge: "SPEAKER", color: "#22D3EE" },
      { delay: 5.3, phrase: '🗣️ "Baby, call reject karo"', badge: "REJECT", color: "#F43F5E" }
    ];

    commands.forEach((cmd, idx) => {
      const a = clamp01((local - cmd.delay) / 0.45);
      if (a <= 0) return;
      const cy = py + 310 + idx * 88;
      ctx.save();
      ctx.globalAlpha = a;
      ctx.translate(0, (1 - a) * 20);
      drawRoundedRect(px + 28, cy, pw - 56, 72, 18);
      ctx.fillStyle = "rgba(255, 255, 255, 0.07)";
      ctx.fill();
      ctx.strokeStyle = cmd.color;
      ctx.lineWidth = 2;
      ctx.stroke();

      ctx.fillStyle = "#F8FAFC";
      ctx.font = '700 23px "Plus Jakarta Sans", sans-serif';
      ctx.textAlign = "left";
      ctx.fillText(cmd.phrase, px + 46, cy + 44);

      drawRoundedRect(px + pw - 154, cy + 16, 112, 40, 20);
      ctx.fillStyle = cmd.color;
      ctx.fill();
      ctx.fillStyle = "#060714";
      ctx.font = '800 17px "Space Grotesk", sans-serif';
      ctx.textAlign = "center";
      ctx.fillText(cmd.badge, px + pw - 98, cy + 42);
      ctx.restore();
    });

    // Bottom subtitle
    drawRoundedRect(56, 1045, WIDTH - 112, 150, 24);
    ctx.fillStyle = "rgba(18, 24, 58, 0.9)";
    ctx.fill();
    ctx.strokeStyle = "rgba(139, 92, 246, 0.55)";
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.fillStyle = "#F8FAFC";
    ctx.font = '700 27px "Plus Jakarta Sans", sans-serif';
    drawWrappedText(
      "Koi button dabane ki zarurat nahi — call ring hote hi Baby aapki aawaz sun leti hai!",
      WIDTH / 2,
      1104,
      WIDTH - 160,
      38,
      "center"
    );
  }

  function drawScene3(t) {
    const local = t - 14.0;

    drawCartoonBoyCard(124, 200, 68, t, false);
    ctx.textAlign = "left";
    ctx.fillStyle = "#22D3EE";
    ctx.font = '800 24px "Space Grotesk", sans-serif';
    ctx.fillText("SIRF 1 MINUTE SETUP", 216, 178);
    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 38px "Space Grotesk", sans-serif';
    ctx.fillText("Use Kaise Karein?", 216, 226);

    const steps = [
      {
        delay: 0.3,
        num: "1",
        title: "Website Se APK Download Karein",
        desc: "Neeche diye link par click karein aur 'Download Baby Assistant APK' dabakar install karein.",
        accent: "#22D3EE"
      },
      {
        delay: 2.2,
        num: "2",
        title: "Permissions & Auto-Mic ON Karein",
        desc: "App ke Setup Wizard mein Mic & Phone permission allow karein aur Voice Assistant switch ON kar dein.",
        accent: "#A78BFA"
      },
      {
        delay: 4.1,
        num: "3",
        title: "Bas Bolo — Hands-Free Control!",
        desc: "Ab jab bhi call aaye, bina screen chhuye bolo 'Baby, call attend karo' ya 'call reject karo'!",
        accent: "#10B981"
      }
    ];

    steps.forEach((step, idx) => {
      const a = clamp01((local - step.delay) / 0.55);
      if (a <= 0) return;
      const sy = 320 + idx * 265;
      ctx.save();
      ctx.globalAlpha = a;
      ctx.translate((1 - a) * 45, 0);

      drawRoundedRect(54, sy, WIDTH - 108, 232, 28);
      ctx.fillStyle = "rgba(18, 22, 54, 0.92)";
      ctx.fill();
      ctx.strokeStyle = step.accent;
      ctx.lineWidth = 3;
      ctx.stroke();

      // Number badge
      ctx.beginPath();
      ctx.arc(116, sy + 64, 34, 0, Math.PI * 2);
      ctx.fillStyle = step.accent;
      ctx.fill();
      ctx.fillStyle = "#060714";
      ctx.font = '800 34px "Space Grotesk", sans-serif';
      ctx.textAlign = "center";
      ctx.fillText(step.num, 116, sy + 76);

      ctx.fillStyle = "#FFFFFF";
      ctx.font = '800 29px "Space Grotesk", sans-serif';
      ctx.textAlign = "left";
      ctx.fillText(step.title, 172, sy + 74);

      ctx.fillStyle = "#CBD5E1";
      ctx.font = '500 25px "Plus Jakarta Sans", sans-serif';
      drawWrappedText(step.desc, 86, sy + 132, WIDTH - 172, 36, "left");

      ctx.restore();
    });

    // Privacy footer note
    ctx.fillStyle = "#94A3B8";
    ctx.font = '700 22px "Plus Jakarta Sans", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("🔒 No Login • No API Key • 100% On-Device Privacy", WIDTH / 2, 1175);
  }

  function drawScene4(t) {
    const local = t - 21.5;
    const pop = easeOutBack(clamp01(local / 0.6));

    ctx.save();
    ctx.translate(WIDTH / 2, 175);
    ctx.scale(pop, pop);
    ctx.fillStyle = "#22D3EE";
    ctx.font = '800 28px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("100% FREE ANDROID APP", 0, 0);
    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 46px "Space Grotesk", sans-serif';
    ctx.fillText("Abhi Download Karein!", 0, 56);
    ctx.restore();

    drawCartoonBoyCard(WIDTH / 2, 465, 175, t, true);

    // Big CTA box
    drawRoundedRect(54, 695, WIDTH - 108, 270, 32);
    const ctaGrad = ctx.createLinearGradient(54, 695, WIDTH - 54, 965);
    ctaGrad.addColorStop(0, "rgba(124, 58, 237, 0.92)");
    ctaGrad.addColorStop(0.55, "rgba(37, 99, 235, 0.92)");
    ctaGrad.addColorStop(1, "rgba(8, 145, 178, 0.92)");
    ctx.fillStyle = ctaGrad;
    ctx.fill();
    ctx.strokeStyle = "#22D3EE";
    ctx.lineWidth = 4;
    ctx.stroke();

    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 36px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("NEECHE LINK PAR CLICK KAREIN", WIDTH / 2, 762);

    ctx.fillStyle = "#E0F2FE";
    ctx.font = '600 25px "Plus Jakarta Sans", sans-serif';
    ctx.fillText("Website kholein aur Baby Call Assistant APK download karein", WIDTH / 2, 812);

    // Website link pill
    drawRoundedRect(88, 845, WIDTH - 176, 82, 41);
    ctx.fillStyle = "#060714";
    ctx.fill();
    ctx.strokeStyle = "#22D3EE";
    ctx.lineWidth = 2.5;
    ctx.stroke();
    ctx.fillStyle = "#22D3EE";
    ctx.font = '700 26px "JetBrains Mono", monospace';
    ctx.fillText(SITE_SHORT, WIDTH / 2, 895);

    // Animated bouncing downward arrows pointing to WhatsApp Status link
    const bounce = Math.sin(t * 7.5) * 16;
    ctx.fillStyle = "#22D3EE";
    ctx.font = '800 58px "Space Grotesk", sans-serif';
    ctx.fillText("👇   👇   👇", WIDTH / 2, 1085 + bounce);

    ctx.fillStyle = "#F8FAFC";
    ctx.font = '800 26px "Plus Jakarta Sans", sans-serif';
    ctx.fillText("TAP LINK BELOW • SHARE WITH FRIENDS", WIDTH / 2, 1185);
  }

  function renderFrame(t) {
    drawBackground(t);
    if (t < 6.5) {
      drawScene1(t);
    } else if (t < 14.0) {
      drawScene2(t);
    } else if (t < 21.5) {
      drawScene3(t);
    } else {
      drawScene4(t);
    }

    if (progressBarEl) {
      progressBarEl.style.width = `${(clamp01(t / TOTAL_DURATION) * 100).toFixed(1)}%`;
    }
  }

  function speakSceneIfNeeded(t) {
    if (!voiceToggle || !voiceToggle.checked || !("speechSynthesis" in window)) return;
    const idx = SCENES.findIndex((s) => t >= s.start && t < s.end);
    if (idx !== -1 && idx !== lastSpokenScene) {
      lastSpokenScene = idx;
      try {
        window.speechSynthesis.cancel();
        const utter = new SpeechSynthesisUtterance(SCENES[idx].voiceText);
        utter.lang = "hi-IN";
        utter.rate = 1.04;
        utter.pitch = 1.05;
        window.speechSynthesis.speak(utter);
      } catch (_) {
        // Ignore TTS unsupported browsers
      }
    }
  }

  function startAmbientMusic(forRecording) {
    stopAmbientMusic();
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      audioCtx = new AudioCtx();
      audioDest = audioCtx.createMediaStreamDestination();

      const masterGain = audioCtx.createGain();
      masterGain.gain.value = 0.14;
      masterGain.connect(audioDest);
      masterGain.connect(audioCtx.destination);

      const notes = [261.63, 329.63, 392.0, 523.25, 392.0, 329.63, 293.66, 392.0];
      let step = 0;

      const playNote = () => {
        if (!audioCtx || audioCtx.state === "closed") return;
        const now = audioCtx.currentTime;
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.type = step % 2 === 0 ? "triangle" : "sine";
        osc.frequency.setValueAtTime(notes[step % notes.length], now);
        gain.gain.setValueAtTime(0.001, now);
        gain.gain.exponentialRampToValueAtTime(0.32, now + 0.04);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.38);
        osc.connect(gain);
        gain.connect(masterGain);
        osc.start(now);
        osc.stop(now + 0.42);
        step++;
      };

      playNote();
      beatTimer = setInterval(playNote, 420);
      return forRecording ? audioDest.stream : null;
    } catch (_) {
      return null;
    }
  }

  function stopAmbientMusic() {
    if (beatTimer) {
      clearInterval(beatTimer);
      beatTimer = null;
    }
    if (audioCtx) {
      try {
        audioCtx.close();
      } catch (_) {
      }
      audioCtx = null;
      audioDest = null;
    }
  }

  function stopPlayback() {
    isPlaying = false;
    if (rafId) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
    stopAmbientMusic();
    if ("speechSynthesis" in window) {
      try {
        window.speechSynthesis.cancel();
      } catch (_) {
      }
    }
    if (playBtn) playBtn.textContent = "▶ Preview Ad (28s)";
  }

  function runAnimationLoop(onFinish) {
    lastFrameTs = performance.now();
    const tick = (now) => {
      if (!isPlaying) return;
      const dt = Math.min(0.1, (now - lastFrameTs) / 1000);
      lastFrameTs = now;
      currentTime += dt;

      if (currentTime >= TOTAL_DURATION) {
        currentTime = TOTAL_DURATION;
        renderFrame(currentTime);
        stopPlayback();
        if (onFinish) onFinish();
        return;
      }

      renderFrame(currentTime);
      const scene = SCENES.find((s) => currentTime >= s.start && currentTime < s.end);
      if (statusTextEl && scene) {
        const prefix = isRecording ? "🔴 Recording" : "▶ Playing";
        statusTextEl.textContent = `${prefix} ${scene.name} (${Math.ceil(TOTAL_DURATION - currentTime)}s left)`;
      }
      speakSceneIfNeeded(currentTime);
      rafId = requestAnimationFrame(tick);
    };
    rafId = requestAnimationFrame(tick);
  }

  function startPreview() {
    if (isRecording) return;
    if (isPlaying) {
      stopPlayback();
      if (statusTextEl) statusTextEl.textContent = "Preview paused. Tap Preview or Download Video.";
      return;
    }
    currentTime = 0;
    lastSpokenScene = -1;
    isPlaying = true;
    if (playBtn) playBtn.textContent = "⏹ Stop Preview";
    startAmbientMusic(false);
    runAnimationLoop(() => {
      if (statusTextEl) statusTextEl.textContent = "Preview complete! Tap 'Download Video' for WhatsApp Status.";
    });
  }

  function startRecordingDownload() {
    if (isRecording) return;
    if (typeof MediaRecorder === "undefined" || !canvas.captureStream) {
      showToast("Yeh browser direct video export support nahi karta — Chrome desktop/Android use karein.");
      return;
    }

    stopPlayback();
    isRecording = true;
    isPlaying = true;
    currentTime = 0;
    lastSpokenScene = -1;
    if (recordBtn) {
      recordBtn.disabled = true;
      recordBtn.textContent = "🔴 Recording 28s Video...";
    }
    if (playBtn) playBtn.disabled = true;

    const canvasStream = canvas.captureStream(30);
    const audioStream = startAmbientMusic(true);
    const combinedTracks = [...canvasStream.getVideoTracks()];
    if (audioStream) {
      combinedTracks.push(...audioStream.getAudioTracks());
    }
    const mixedStream = new MediaStream(combinedTracks);

    const mimeCandidates = [
      "video/mp4;codecs=avc1.42E01E,mp4a.40.2",
      "video/mp4",
      "video/webm;codecs=vp9,opus",
      "video/webm;codecs=vp8,opus",
      "video/webm"
    ];
    const chosenMime = mimeCandidates.find((m) => MediaRecorder.isTypeSupported(m)) || "";
    const isMp4 = chosenMime.includes("mp4");

    const chunks = [];
    let recorder;
    try {
      recorder = chosenMime
        ? new MediaRecorder(mixedStream, { mimeType: chosenMime, videoBitsPerSecond: 4_500_000 })
        : new MediaRecorder(mixedStream);
    } catch (_) {
      recorder = new MediaRecorder(mixedStream);
    }

    recorder.ondataavailable = (ev) => {
      if (ev.data && ev.data.size > 0) chunks.push(ev.data);
    };

    recorder.onstop = () => {
      isRecording = false;
      if (recordBtn) {
        recordBtn.disabled = false;
        recordBtn.textContent = "⬇ Download Video (28s WhatsApp Status)";
      }
      if (playBtn) playBtn.disabled = false;

      const blob = new Blob(chunks, { type: chosenMime || "video/webm" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = isMp4
        ? "Baby-Assistant-WhatsApp-Status.mp4"
        : "Baby-Assistant-WhatsApp-Status.webm";
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 10000);

      if (statusTextEl) {
        statusTextEl.textContent = "✅ Video downloaded! Ab WhatsApp Status mein upload karein aur caption paste karein.";
      }
      showToast("WhatsApp Status video download ho gaya!");
    };

    recorder.start(250);
    showToast("28-second HD WhatsApp Status video record ho raha hai...");

    runAnimationLoop(() => {
      if (recorder.state !== "inactive") {
        recorder.stop();
      }
    });
  }

  // Initial static render once image loads
  const drawInitial = () => renderFrame(0.8);
  boyImg.onload = drawInitial;
  iconImg.onload = drawInitial;
  drawInitial();

  if (playBtn) playBtn.addEventListener("click", startPreview);
  if (recordBtn) recordBtn.addEventListener("click", startRecordingDownload);

  const copyCaptionBtn = document.getElementById("btn-copy-caption");
  if (copyCaptionBtn && captionBox) {
    copyCaptionBtn.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(captionBox.value);
        showToast("WhatsApp Status caption & link copied!");
      } catch (_) {
        captionBox.select();
        document.execCommand("copy");
        showToast("Caption copied!");
      }
    });
  }

  const copyPromptBtn = document.getElementById("btn-copy-ai-prompt");
  const promptBox = document.getElementById("ai-video-prompt-text");
  if (copyPromptBtn && promptBox) {
    promptBox.value =
      `Create a 28-second vertical 9:16 Pixar-style 3D animated advertisement video for WhatsApp Status in vibrant Midnight Indigo (#0A0B20), Electric Violet, and Neon Cyan lighting.\n` +
      `Character: A friendly, expressive young Indian cartoon boy in a purple-and-cyan hoodie holding a glowing Android smartphone.\n\n` +
      `[0:00-0:07] Boy smiles at camera while his phone rings: "Haath busy hai ya bike chala rahe ho? Ab phone chhune ki zarurat nahi! Miliye Baby Call Assistant se!"\n` +
      `[0:07-0:15] Close-up of phone screen showing incoming call with glowing neon badge "AUTO-MIC ON". Boy speaks hands-free: "Baby, call attend karo!" Call connects automatically. Pop-up graphics flash: "call attend mat karo", "speaker on karo", "call reject karo".\n` +
      `[0:15-0:22] 3 clean animated floating UI cards appear beside the boy: Step 1: Website se Free APK Download -> Step 2: Mic & Phone Permission Allow + Assistant ON -> Step 3: Incoming Call par bas bolo "Baby, call attend karo!"\n` +
      `[0:22-0:28] Boy enthusiastically points downward with glowing animated down-arrows toward the bottom link area with bold text "NEECHE LINK DABAKAR DOWNLOAD KAREIN" and URL "${SITE_SHORT}". Upbeat modern tech background music, clear energetic Hinglish voiceover.`;

    copyPromptBtn.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(promptBox.value);
        showToast("AI Video prompt copied! (Veo / Runway / Kling mein paste karein)");
      } catch (_) {
        promptBox.select();
        document.execCommand("copy");
        showToast("AI Video prompt copied!");
      }
    });
  }
})();
