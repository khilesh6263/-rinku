(() => {
  const WIDTH = 720;
  const HEIGHT = 1280;
  const TOTAL_DURATION = 60.0; // Full 1-minute (60s) WhatsApp Status advertisement

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
      start: 0.0,
      end: 8.0,
      name: "Scene 1/7: Intro & Hook",
      voiceText:
        "Haath busy hai, bike chala rahe ho ya khana bana rahe ho? Ab incoming call aane par phone chhune ki bilkul zarurat nahi! Miliye Baby Call Assistant se, jo sirf aapki aawaz se poora call control karta hai!"
    },
    {
      start: 8.0,
      end: 18.0,
      name: "Scene 2/7: Auto-Mic on Incoming Call",
      voiceText:
        "Feature number one: Auto Mic on Incoming Call! Jaise hi aapke phone par call ring hota hai, aapko koi button nahi dabana. Baby ka mic apne aap ON ho jata hai aur jab tak phone ring hota hai, aapki aawaz sunta rehta hai!"
    },
    {
      start: 18.0,
      end: 28.0,
      name: "Scene 3/7: Answer, Ignore & Reject",
      voiceText:
        "Feature number two: Smart Call Commands! Bolo Baby call attend karo, aur call turant uth jayega. Bolo Baby call attend mat karo, to call chhod dega. Aur bolo Baby call reject karo, to call turant cut ho jayega!"
    },
    {
      start: 28.0,
      end: 37.0,
      name: "Scene 4/7: Speaker & Mic Mute Control",
      voiceText:
        "Feature number three: Call ke dauran Speaker aur Mute control! Bas bolo Baby speaker on karo ya speaker off karo, aur bolo Baby mute karo ya mute hatao — sab kuch bina screen chhuye!"
    },
    {
      start: 37.0,
      end: 45.0,
      name: "Scene 5/7: Test Lab, Languages & Privacy",
      voiceText:
        "Feature number four: Hindi, Hinglish aur English teeno languages! Built-in Test Lab mein aap bina real call ke test kar sakte hain. Koi login ya API key nahi, aur koi voice audio upload nahi hoti — 100 percent private!"
    },
    {
      start: 45.0,
      end: 53.0,
      name: "Scene 6/7: Kaise Download & Use Karein",
      voiceText:
        "Use karna sirf ek minute ka kaam hai! Step one, website se free APK download karein. Step two, setup wizard mein mic aur phone permission allow karke Assistant ON karein. Step three, call aate hi bas bolkar control karein!"
    },
    {
      start: 53.0,
      end: 60.0,
      name: "Scene 7/7: Download Now CTA",
      voiceText:
        "To abhi neeche diye gaye link par click karein, website kholein aur Baby Call Assistant bilkul free download karein!"
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
  const timeLabelEl = document.getElementById("promo-time-label");
  const playBtn = document.getElementById("btn-play-preview");
  const recordBtn = document.getElementById("btn-record-video");
  const voiceToggle = document.getElementById("toggle-voiceover");
  const siteUrlDisplay = document.getElementById("promo-site-url");
  const captionBox = document.getElementById("whatsapp-caption-text");

  if (siteUrlDisplay) siteUrlDisplay.textContent = SITE_URL;
  if (captionBox) {
    captionBox.value =
      `🎙️ *Baby Call Assistant — 100% Free Hands-Free Voice Call App!*\n\n` +
      `✨ *Top Features:*\n` +
      `🔥 Incoming call aate hi *Auto-Mic ON* (bina tap kiye!)\n` +
      `📞 *"Baby, call attend karo"* — call pick karein\n` +
      `🔕 *"Baby, call attend mat karo"* — ignore karein\n` +
      `❌ *"Baby, call reject karo"* — call cut karein\n` +
      `🔊 *"Baby, speaker on/off karo"* & *"mute karo"*\n` +
      `🇮🇳 Hindi + Hinglish + English • Built-in Test Lab • 100% Private (No Login / No API Key)\n\n` +
      `👇 *Neeche link dabakar website se FREE APK download karein:*\n` +
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

  function formatSec(sec) {
    const s = Math.max(0, Math.floor(sec));
    const m = Math.floor(s / 60);
    const rem = s % 60;
    return `${m}:${rem < 10 ? "0" : ""}${rem}`;
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
      if (ctx.measureText(testLine).width > maxWidth && n > 0) {
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

  function drawBackground(t, sceneNum) {
    const bgGrad = ctx.createLinearGradient(0, 0, WIDTH, HEIGHT);
    bgGrad.addColorStop(0, "#060818");
    bgGrad.addColorStop(0.5, "#11143B");
    bgGrad.addColorStop(1, "#050714");
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, WIDTH, HEIGHT);

    const blob1X = 160 + Math.sin(t * 0.65) * 65;
    const blob1Y = 260 + Math.cos(t * 0.45) * 45;
    const g1 = ctx.createRadialGradient(blob1X, blob1Y, 20, blob1X, blob1Y, 320);
    g1.addColorStop(0, "rgba(124, 58, 237, 0.34)");
    g1.addColorStop(1, "rgba(124, 58, 237, 0)");
    ctx.fillStyle = g1;
    ctx.fillRect(0, 0, WIDTH, HEIGHT);

    const blob2X = 560 + Math.cos(t * 0.55) * 55;
    const blob2Y = 920 + Math.sin(t * 0.75) * 55;
    const g2 = ctx.createRadialGradient(blob2X, blob2Y, 20, blob2X, blob2Y, 330);
    g2.addColorStop(0, "rgba(34, 211, 238, 0.25)");
    g2.addColorStop(1, "rgba(34, 211, 238, 0)");
    ctx.fillStyle = g2;
    ctx.fillRect(0, 0, WIDTH, HEIGHT);

    // Subtle floating glow particles
    for (let i = 0; i < 24; i++) {
      const px = ((i * 97 + t * 18) % (WIDTH + 40)) - 20;
      const py = ((i * 151 - t * 22) % HEIGHT + HEIGHT) % HEIGHT;
      const alpha = 0.16 + 0.24 * Math.sin(t * 2 + i);
      ctx.fillStyle = i % 2 === 0 ? `rgba(34, 211, 238, ${alpha})` : `rgba(167, 139, 250, ${alpha})`;
      ctx.beginPath();
      ctx.arc(px, py, 2.5 + (i % 3), 0, Math.PI * 2);
      ctx.fill();
    }

    // Top branding bar + scene counter
    drawRoundedRect(46, 36, 420, 56, 28);
    ctx.fillStyle = "rgba(15, 20, 52, 0.86)";
    ctx.fill();
    ctx.strokeStyle = "rgba(34, 211, 238, 0.45)";
    ctx.lineWidth = 2;
    ctx.stroke();

    if (iconImg.complete && iconImg.naturalWidth > 0) {
      ctx.drawImage(iconImg, 60, 44, 40, 40);
    }
    ctx.fillStyle = "#F8FAFC";
    ctx.font = '700 22px "Space Grotesk", sans-serif';
    ctx.textAlign = "left";
    ctx.fillText("BABY CALL ASSISTANT", 112, 72);

    // Right timer badge
    drawRoundedRect(486, 36, 188, 56, 28);
    ctx.fillStyle = "rgba(124, 58, 237, 0.26)";
    ctx.fill();
    ctx.strokeStyle = "rgba(167, 139, 250, 0.5)";
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.fillStyle = "#22D3EE";
    ctx.font = '700 20px "JetBrains Mono", monospace';
    ctx.textAlign = "center";
    ctx.fillText(`${sceneNum}/7 • ${formatSec(t)}`, 580, 72);

    // Bottom 60s progress bar
    const prog = clamp01(t / TOTAL_DURATION);
    ctx.fillStyle = "rgba(255,255,255,0.12)";
    ctx.fillRect(42, HEIGHT - 28, WIDTH - 84, 8);
    const pg = ctx.createLinearGradient(42, 0, WIDTH - 42, 0);
    pg.addColorStop(0, "#7C3AED");
    pg.addColorStop(0.5, "#3B82F6");
    pg.addColorStop(1, "#22D3EE");
    ctx.fillStyle = pg;
    ctx.fillRect(42, HEIGHT - 28, (WIDTH - 84) * prog, 8);
  }

  function drawCartoonBoy(cx, cy, radius, t, showRings = true) {
    const floatY = Math.sin(t * 3.2) * 7;
    const y = cy + floatY;

    if (showRings) {
      for (let r = 0; r < 2; r++) {
        const wave = (t * 0.85 + r * 0.5) % 1;
        ctx.beginPath();
        ctx.arc(cx, y, radius + 10 + wave * 48, 0, Math.PI * 2);
        ctx.strokeStyle = `rgba(34, 211, 238, ${(1 - wave) * 0.45})`;
        ctx.lineWidth = 3.5;
        ctx.stroke();
      }
    }

    const ringGrad = ctx.createLinearGradient(cx - radius, y - radius, cx + radius, y + radius);
    ringGrad.addColorStop(0, "#22D3EE");
    ringGrad.addColorStop(0.5, "#8B5CF6");
    ringGrad.addColorStop(1, "#EC4899");

    ctx.beginPath();
    ctx.arc(cx, y, radius + 6, 0, Math.PI * 2);
    ctx.fillStyle = ringGrad;
    ctx.fill();

    ctx.save();
    ctx.beginPath();
    ctx.arc(cx, y, radius, 0, Math.PI * 2);
    ctx.clip();
    if (boyImg.complete && boyImg.naturalWidth > 0) {
      const scale = Math.max((radius * 2) / boyImg.naturalWidth, (radius * 2) / boyImg.naturalHeight);
      const dw = boyImg.naturalWidth * scale;
      const dh = boyImg.naturalHeight * scale;
      ctx.drawImage(boyImg, cx - dw / 2, y - dh / 2 + 10, dw, dh);
    } else {
      ctx.fillStyle = "#1E1B4B";
      ctx.fillRect(cx - radius, y - radius, radius * 2, radius * 2);
    }
    ctx.restore();
  }

  function drawPresenterHeader(t, tagText, titleText) {
    drawCartoonBoy(114, 182, 62, t, false);
    ctx.textAlign = "left";
    ctx.fillStyle = "#22D3EE";
    ctx.font = '800 22px "Space Grotesk", sans-serif';
    ctx.fillText(tagText, 198, 162);
    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 34px "Space Grotesk", sans-serif';
    ctx.fillText(titleText, 198, 206);
  }

  function drawSpeechBox(x, y, w, h, label, message) {
    drawRoundedRect(x, y, w, h, 26);
    ctx.fillStyle = "rgba(16, 22, 54, 0.94)";
    ctx.fill();
    ctx.strokeStyle = "rgba(34, 211, 238, 0.65)";
    ctx.lineWidth = 2.5;
    ctx.stroke();

    ctx.fillStyle = "#22D3EE";
    ctx.font = '800 22px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText(label, x + w / 2, y + 40);

    ctx.fillStyle = "#F8FAFC";
    ctx.font = '600 26px "Plus Jakarta Sans", sans-serif';
    drawWrappedText(message, x + w / 2, y + 84, w - 52, 36, "center");
  }

  function drawPhoneShell(px, py, pw, ph, borderColor = "#22D3EE") {
    drawRoundedRect(px, py, pw, ph, 42);
    ctx.fillStyle = "rgba(10, 14, 38, 0.96)";
    ctx.fill();
    ctx.strokeStyle = borderColor;
    ctx.lineWidth = 4;
    ctx.stroke();

    // Notch
    drawRoundedRect(px + pw / 2 - 58, py + 14, 116, 20, 10);
    ctx.fillStyle = "#04050D";
    ctx.fill();
  }

  function drawWaveform(cx, cy, t, barCount = 11, colorA = "#22D3EE", colorB = "#8B5CF6") {
    const spacing = 22;
    const startX = cx - ((barCount - 1) * spacing) / 2;
    for (let i = 0; i < barCount; i++) {
      const h = 16 + Math.abs(Math.sin(t * 7.5 + i * 0.65)) * 54;
      const bx = startX + i * spacing - 5;
      drawRoundedRect(bx, cy - h / 2, 10, h, 5);
      const grad = ctx.createLinearGradient(0, cy - 35, 0, cy + 35);
      grad.addColorStop(0, colorA);
      grad.addColorStop(1, colorB);
      ctx.fillStyle = grad;
      ctx.fill();
    }
  }

  // SCENE 1 (0–8s): Hook & Cartoon Boy Introduction
  function drawScene1(t) {
    const pop = easeOutBack(clamp01(t / 0.7));

    ctx.save();
    ctx.translate(WIDTH / 2, 165);
    ctx.scale(pop, pop);
    ctx.fillStyle = "#22D3EE";
    ctx.font = '800 25px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("BIKE CHALA RAHE HO? HAATH BUSY HAI?", 0, 0);
    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 44px "Space Grotesk", sans-serif';
    ctx.fillText("Ab Phone Touch Kiye Bina", 0, 52);
    ctx.fillText("Call Control Karein!", 0, 104);
    ctx.restore();

    drawCartoonBoy(WIDTH / 2, 525, 195, t, true);

    // Floating use-case chips
    const chips = [
      { x: 52, y: 760, label: "🏍️ Bike Riding" },
      { x: 266, y: 760, label: "🍳 Cooking" },
      { x: 480, y: 760, label: "🚗 Driving" }
    ];
    chips.forEach((c, i) => {
      const a = clamp01((t - 0.5 - i * 0.25) / 0.4);
      ctx.save();
      ctx.globalAlpha = a;
      drawRoundedRect(c.x, c.y, 188, 52, 26);
      ctx.fillStyle = "rgba(124, 58, 237, 0.28)";
      ctx.fill();
      ctx.strokeStyle = "rgba(167, 139, 250, 0.6)";
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.fillStyle = "#F8FAFC";
      ctx.font = '700 21px "Plus Jakarta Sans", sans-serif';
      ctx.textAlign = "center";
      ctx.fillText(c.label, c.x + 94, c.y + 34);
      ctx.restore();
    });

    drawSpeechBox(
      48,
      850,
      WIDTH - 96,
      245,
      "CARTOON BOY EXPLAINS:",
      "“Miliye Baby Call Assistant se — ek smart voice app jo sirf aapki Hindi, Hinglish ya English aawaz se incoming call attend, reject, speaker aur mute control karta hai!”"
    );

    ctx.fillStyle = "#94A3B8";
    ctx.font = '700 22px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("Chaliye iske saare features ek-ek karke dekhte hain ➔", WIDTH / 2, 1170);
  }

  // SCENE 2 (8–18s): Feature 1 — Automatic Mic ON on Incoming Call + Continuous Ring Listening
  function drawScene2(t) {
    const local = t - 8.0;
    drawPresenterHeader(t, "FEATURE 1 • ZERO-TAP HANDS-FREE", "Call Aate Hi Auto-Mic ON!");

    drawPhoneShell(82, 275, 556, 680, "#22D3EE");

    // Incoming Call Banner
    const ringShake = Math.sin(t * 22) * 3;
    drawRoundedRect(130, 326, 460, 56, 28);
    ctx.fillStyle = "rgba(34, 211, 238, 0.2)";
    ctx.fill();
    ctx.strokeStyle = "#22D3EE";
    ctx.lineWidth = 2.5;
    ctx.stroke();
    ctx.fillStyle = "#22D3EE";
    ctx.font = '800 23px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("📞 INCOMING CALL RINGING...", WIDTH / 2 + ringShake, 362);

    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 36px "Space Grotesk", sans-serif';
    ctx.fillText("Mom (+91 98••• ••210)", WIDTH / 2, 434);

    // Auto-Mic Activated Highlight Card
    const micPop = easeOutBack(clamp01((local - 0.6) / 0.6));
    ctx.save();
    ctx.translate(WIDTH / 2, 575);
    ctx.scale(micPop, micPop);
    drawRoundedRect(-232, -96, 464, 192, 28);
    const micGrad = ctx.createLinearGradient(-232, -96, 232, 96);
    micGrad.addColorStop(0, "rgba(124, 58, 237, 0.45)");
    micGrad.addColorStop(1, "rgba(34, 211, 238, 0.35)");
    ctx.fillStyle = micGrad;
    ctx.fill();
    ctx.strokeStyle = "#22D3EE";
    ctx.lineWidth = 3;
    ctx.stroke();

    ctx.fillStyle = "#6EE7B7";
    ctx.font = '800 24px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("⚡ MIC STARTED AUTOMATICALLY!", 0, -48);
    ctx.fillStyle = "#FFFFFF";
    ctx.font = '600 21px "Plus Jakarta Sans", sans-serif';
    ctx.fillText("No 'Tap to Speak' button needed", 0, -14);
    ctx.restore();

    drawWaveform(WIDTH / 2, 622, t, 13, "#6EE7B7", "#22D3EE");

    // Two feature callouts inside phone
    const pills = [
      {
        delay: 2.2,
        y: 710,
        icon: "🔁",
        title: "Continuous Ring Listening",
        sub: "Ring bajte rehne tak mic auto re-arm hota rehta hai"
      },
      {
        delay: 4.4,
        y: 822,
        icon: "🔔",
        title: "Armed Foreground Monitor",
        sub: "Background mein bhi incoming call turant detect hota hai"
      }
    ];

    pills.forEach((p) => {
      const a = clamp01((local - p.delay) / 0.5);
      if (a <= 0) return;
      ctx.save();
      ctx.globalAlpha = a;
      drawRoundedRect(114, p.y, 492, 92, 20);
      ctx.fillStyle = "rgba(255,255,255,0.07)";
      ctx.fill();
      ctx.strokeStyle = "rgba(167, 139, 250, 0.45)";
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.fillStyle = "#FFFFFF";
      ctx.font = '700 23px "Space Grotesk", sans-serif';
      ctx.textAlign = "left";
      ctx.fillText(`${p.icon}  ${p.title}`, 136, p.y + 38);
      ctx.fillStyle = "#CBD5E1";
      ctx.font = '500 19px "Plus Jakarta Sans", sans-serif';
      ctx.fillText(p.sub, 136, p.y + 70);
      ctx.restore();
    });

    drawSpeechBox(
      48,
      988,
      WIDTH - 96,
      205,
      "BINA BUTTON DABAYE LISTEN START:",
      "“Call aate hi Baby ka mic khud chalu ho jata hai — bas apna command bolo aur kaam ho gaya!”"
    );
  }

  // SCENE 3 (18–28s): Feature 2 — Answer, Do Not Answer (Negation-Smart) & Reject Call
  function drawScene3(t) {
    const local = t - 18.0;
    drawPresenterHeader(t, "FEATURE 2 • SMART CALL ACTIONS", "Answer, Ignore & Reject!");

    // Determine active sub-demo (3 phases across 10 seconds)
    let phase = 0;
    if (local >= 6.7) phase = 2;
    else if (local >= 3.3) phase = 1;

    const phases = [
      {
        accent: "#10B981",
        status: "✅ CALL CONNECTED!",
        spoken: '🗣️ "Baby, call attend karo"',
        alt: 'Also works: "Baby, call utha lo" / "Answer the call"',
        reply: '🔊 Baby Reply: "Ji, call attend kar rahi hoon."',
        note: "TelecomManager incoming call ko turant receive kar leta hai."
      },
      {
        accent: "#F59E0B",
        status: "🔕 LEFT UNANSWERED (IGNORED)",
        spoken: '🗣️ "Baby, call attend mat karo"',
        alt: 'Negation-Smart: "attend mat karo" ≠ "attend karo"!',
        reply: '🔊 Baby Reply: "Theek hai, call attend nahi karungi."',
        note: "Smart engine negation samajhta hai aur call ko chhod deta hai."
      },
      {
        accent: "#F43F5E",
        status: "❌ INCOMING CALL REJECTED",
        spoken: '🗣️ "Baby, call reject karo"',
        alt: 'Also works: "Baby, call kaat do" / "Decline call"',
        reply: '🔊 Baby Reply: "Call reject kar diya gaya."',
        note: "Busy hone par bolkar incoming call turant reject kar dein."
      }
    ];

    const cur = phases[phase];
    drawPhoneShell(82, 275, 556, 680, cur.accent);

    // Top 3 phase tabs inside phone
    const tabLabels = ["1. ANSWER", "2. IGNORE", "3. REJECT"];
    const tabColors = ["#10B981", "#F59E0B", "#F43F5E"];
    tabLabels.forEach((lbl, idx) => {
      const tx = 112 + idx * 168;
      const active = idx === phase;
      drawRoundedRect(tx, 322, 158, 46, 23);
      ctx.fillStyle = active ? tabColors[idx] : "rgba(255,255,255,0.08)";
      ctx.fill();
      ctx.fillStyle = active ? "#060714" : "#94A3B8";
      ctx.font = '800 18px "Space Grotesk", sans-serif';
      ctx.textAlign = "center";
      ctx.fillText(lbl, tx + 79, 352);
    });

    // Spoken command card
    drawRoundedRect(114, 405, 492, 145, 24);
    ctx.fillStyle = "rgba(124, 58, 237, 0.24)";
    ctx.fill();
    ctx.strokeStyle = "rgba(167, 139, 250, 0.65)";
    ctx.lineWidth = 2.5;
    ctx.stroke();

    ctx.fillStyle = "#CBD5E1";
    ctx.font = '700 19px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("YOU SAY:", WIDTH / 2, 442);
    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 31px "Plus Jakarta Sans", sans-serif';
    ctx.fillText(cur.spoken, WIDTH / 2, 490);
    ctx.fillStyle = "#22D3EE";
    ctx.font = '600 18px "Plus Jakarta Sans", sans-serif';
    ctx.fillText(cur.alt, WIDTH / 2, 528);

    // Big Status Outcome Banner
    drawRoundedRect(114, 585, 492, 118, 26);
    ctx.fillStyle = `${cur.accent}2B`;
    ctx.fill();
    ctx.strokeStyle = cur.accent;
    ctx.lineWidth = 3.5;
    ctx.stroke();
    ctx.fillStyle = cur.accent;
    ctx.font = '800 29px "Space Grotesk", sans-serif';
    ctx.fillText(cur.status, WIDTH / 2, 654);

    // Baby TTS Voice Reply card
    drawRoundedRect(114, 738, 492, 175, 24);
    ctx.fillStyle = "rgba(255,255,255,0.06)";
    ctx.fill();
    ctx.strokeStyle = "rgba(34, 211, 238, 0.45)";
    ctx.lineWidth = 2;
    ctx.stroke();

    ctx.fillStyle = "#22D3EE";
    ctx.font = '700 23px "Plus Jakarta Sans", sans-serif';
    drawWrappedText(cur.reply, WIDTH / 2, 795, 440, 34, "center");

    ctx.fillStyle = "#CBD5E1";
    ctx.font = '500 20px "Plus Jakarta Sans", sans-serif';
    drawWrappedText(cur.note, WIDTH / 2, 864, 440, 28, "center");

    drawSpeechBox(
      48,
      988,
      WIDTH - 96,
      205,
      "NEGATION-SMART AI CLASSIFIER:",
      "“Call attend karo aur call attend mat karo mein kabhi confuse nahi hota — 100% accurate Hindi & Hinglish voice recognition!”"
    );
  }

  // SCENE 4 (28–37s): Feature 3 — In-Call Speaker ON/OFF & Mic Mute/Unmute
  function drawScene4(t) {
    const local = t - 28.0;
    const isMutePhase = local >= 4.5;
    drawPresenterHeader(t, "FEATURE 3 • IN-CALL AUDIO CONTROL", "Speaker & Mic Mute Voice!");

    drawPhoneShell(82, 275, 556, 680, isMutePhase ? "#A78BFA" : "#22D3EE");

    // Active Call header
    drawRoundedRect(156, 326, 408, 48, 24);
    ctx.fillStyle = "rgba(16, 185, 129, 0.22)";
    ctx.fill();
    ctx.fillStyle = "#6EE7B7";
    ctx.font = '800 21px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("🟢 ACTIVE CALL • 00:18", WIDTH / 2, 358);

    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 34px "Space Grotesk", sans-serif';
    ctx.fillText("Mom (+91 98••• ••210)", WIDTH / 2, 424);

    // Visual Toggle Tiles (Speaker & Mic)
    const speakerOn = !isMutePhase ? local > 1.0 : true;
    const micMuted = isMutePhase && local > 5.4;

    // Speaker tile
    drawRoundedRect(118, 468, 230, 210, 26);
    ctx.fillStyle = speakerOn ? "rgba(34, 211, 238, 0.26)" : "rgba(255,255,255,0.06)";
    ctx.fill();
    ctx.strokeStyle = speakerOn ? "#22D3EE" : "rgba(255,255,255,0.2)";
    ctx.lineWidth = speakerOn ? 3.5 : 2;
    ctx.stroke();
    ctx.font = '700 58px sans-serif';
    ctx.fillText("🔊", 233, 558);
    ctx.fillStyle = speakerOn ? "#22D3EE" : "#94A3B8";
    ctx.font = '800 24px "Space Grotesk", sans-serif';
    ctx.fillText(speakerOn ? "SPEAKER: ON" : "SPEAKER: OFF", 233, 620);
    ctx.fillStyle = "#CBD5E1";
    ctx.font = '600 17px "Plus Jakarta Sans", sans-serif';
    ctx.fillText("Loudspeaker Mode", 233, 652);

    // Mic Mute tile
    drawRoundedRect(372, 468, 230, 210, 26);
    ctx.fillStyle = micMuted ? "rgba(244, 63, 94, 0.26)" : "rgba(255,255,255,0.06)";
    ctx.fill();
    ctx.strokeStyle = micMuted ? "#F43F5E" : "rgba(255,255,255,0.2)";
    ctx.lineWidth = micMuted ? 3.5 : 2;
    ctx.stroke();
    ctx.font = '700 58px sans-serif';
    ctx.fillText(micMuted ? "🔇" : "🎙️", 487, 558);
    ctx.fillStyle = micMuted ? "#FDA4AF" : "#6EE7B7";
    ctx.font = '800 24px "Space Grotesk", sans-serif';
    ctx.fillText(micMuted ? "MIC: MUTED" : "MIC: LIVE", 487, 620);
    ctx.fillStyle = "#CBD5E1";
    ctx.font = '600 17px "Plus Jakarta Sans", sans-serif';
    ctx.fillText(micMuted ? "Voice Paused" : "Audio Active", 487, 652);

    // Live command cards
    const cmd1 = isMutePhase ? '🗣️ "Baby, mute karo"' : '🗣️ "Baby, speaker on karo"';
    const cmd2 = isMutePhase ? '🗣️ "Baby, mute hatao"' : '🗣️ "Baby, speaker off karo"';
    const reply = isMutePhase
      ? '🔊 "Mic mute kar diya hai."'
      : '🔊 "Speaker on kar diya hai."';

    drawRoundedRect(118, 712, 484, 195, 24);
    ctx.fillStyle = "rgba(18, 24, 58, 0.9)";
    ctx.fill();
    ctx.strokeStyle = isMutePhase ? "#F43F5E" : "#22D3EE";
    ctx.lineWidth = 2.5;
    ctx.stroke();
    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 28px "Plus Jakarta Sans", sans-serif';
    ctx.fillText(cmd1, WIDTH / 2, 766);
    ctx.fillStyle = "#94A3B8";
    ctx.font = '600 22px "Plus Jakarta Sans", sans-serif';
    ctx.fillText(cmd2, WIDTH / 2, 814);
    ctx.fillStyle = "#6EE7B7";
    ctx.font = '700 23px "Plus Jakarta Sans", sans-serif';
    ctx.fillText(reply, WIDTH / 2, 872);

    drawSpeechBox(
      48,
      988,
      WIDTH - 96,
      205,
      "FULL HANDS-FREE CALLING:",
      "“Call uthane ke baad bhi phone mat chhuo — speaker ON/OFF aur mic mute/unmute sirf bolkar control karein!”"
    );
  }

  // SCENE 5 (37–45s): Feature 4 — Test Lab, 3 Languages, Smart Confirmation & 100% Privacy
  function drawScene5(t) {
    const local = t - 37.0;
    drawPresenterHeader(t, "FEATURE 4 • SAFE, SMART & PRIVATE", "Test Lab + 3 Languages!");

    const cards = [
      {
        delay: 0.2,
        y: 280,
        accent: "#22D3EE",
        icon: "🧪",
        title: "Built-in Test Lab (Simulate Call)",
        desc: "Bina kisi real call ke app ke andar hi incoming call simulate karke saare voice commands test karein!"
      },
      {
        delay: 1.6,
        y: 472,
        accent: "#A78BFA",
        icon: "🇮🇳",
        title: "Hindi • Hinglish • English",
        desc: "Natural bolchal support + optional 'Baby' wake word + noise filter aur uncertain commands par 'Haan/Naa' confirmation."
      },
      {
        delay: 3.0,
        y: 664,
        accent: "#10B981",
        icon: "🔒",
        title: "100% Private • No Login • No API Key",
        desc: "Koi voice recording save ya cloud server par upload nahi hoti. Sirf local command history aapke phone mein rehti hai!"
      }
    ];

    cards.forEach((card) => {
      const a = clamp01((local - card.delay) / 0.5);
      if (a <= 0) return;
      ctx.save();
      ctx.globalAlpha = a;
      ctx.translate(0, (1 - a) * 26);

      drawRoundedRect(54, card.y, WIDTH - 108, 172, 26);
      ctx.fillStyle = "rgba(18, 22, 54, 0.94)";
      ctx.fill();
      ctx.strokeStyle = card.accent;
      ctx.lineWidth = 3;
      ctx.stroke();

      ctx.font = '700 40px sans-serif';
      ctx.textAlign = "left";
      ctx.fillText(card.icon, 80, card.y + 62);

      ctx.fillStyle = "#FFFFFF";
      ctx.font = '800 26px "Space Grotesk", sans-serif';
      ctx.fillText(card.title, 142, card.y + 58);

      ctx.fillStyle = "#CBD5E1";
      ctx.font = '500 22px "Plus Jakarta Sans", sans-serif';
      drawWrappedText(card.desc, 82, card.y + 104, WIDTH - 164, 30, "left");

      ctx.restore();
    });

    // Trust badges strip
    const badges = ["✅ Free Forever", "✅ Works on Android 7+", "✅ Zero Ads"];
    badges.forEach((b, i) => {
      const bx = 54 + i * 208;
      drawRoundedRect(bx, 874, 196, 56, 28);
      ctx.fillStyle = "rgba(16, 185, 129, 0.2)";
      ctx.fill();
      ctx.strokeStyle = "rgba(16, 185, 129, 0.55)";
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.fillStyle = "#6EE7B7";
      ctx.font = '700 20px "Plus Jakarta Sans", sans-serif';
      ctx.textAlign = "center";
      ctx.fillText(b, bx + 98, 910);
    });

    drawSpeechBox(
      48,
      988,
      WIDTH - 96,
      205,
      "BILKUL SAFE AUR EASY:",
      "“Na koi account banana hai, na koi API key — download karo aur turant Test Lab mein khud try karo!”"
    );
  }

  // SCENE 6 (45–53s): Feature 5 — How to Download & Set Up in 3 Simple Steps
  function drawScene6(t) {
    const local = t - 45.0;
    drawPresenterHeader(t, "KAISE DOWNLOAD & USE KAREIN", "Sirf 3 Easy Steps!");

    const steps = [
      {
        delay: 0.2,
        num: "1",
        accent: "#22D3EE",
        title: "Neeche Link Dabakar APK Download Karein",
        desc: "Website kholein → 'Download Baby Assistant APK' button dabayein (ya QR code scan karein) aur phone mein install karein."
      },
      {
        delay: 2.2,
        num: "2",
        accent: "#A78BFA",
        title: "Setup Wizard & Auto-Mic ON Karein",
        desc: "App open karke Mic, Phone & Notification permissions allow karein, Voice Assistant ON karein aur Battery Unrestricted set karein."
      },
      {
        delay: 4.2,
        num: "3",
        accent: "#10B981",
        title: "Incoming Call Aate Hi Bas Bolo!",
        desc: "Ab jab bhi phone ring hoga, mic apne aap ON ho jayega — bas bolo 'Baby, call attend karo' aur hands-free baat karein!"
      }
    ];

    steps.forEach((step, idx) => {
      const a = clamp01((local - step.delay) / 0.55);
      if (a <= 0) return;
      const sy = 288 + idx * 285;
      ctx.save();
      ctx.globalAlpha = a;
      ctx.translate((1 - a) * 42, 0);

      drawRoundedRect(50, sy, WIDTH - 100, 252, 28);
      ctx.fillStyle = "rgba(18, 22, 54, 0.94)";
      ctx.fill();
      ctx.strokeStyle = step.accent;
      ctx.lineWidth = 3;
      ctx.stroke();

      ctx.beginPath();
      ctx.arc(108, sy + 62, 34, 0, Math.PI * 2);
      ctx.fillStyle = step.accent;
      ctx.fill();
      ctx.fillStyle = "#060714";
      ctx.font = '800 34px "Space Grotesk", sans-serif';
      ctx.textAlign = "center";
      ctx.fillText(step.num, 108, sy + 74);

      ctx.fillStyle = "#FFFFFF";
      ctx.font = '800 26px "Space Grotesk", sans-serif';
      ctx.textAlign = "left";
      drawWrappedText(step.title, 162, sy + 54, WIDTH - 235, 32, "left");

      ctx.fillStyle = "#CBD5E1";
      ctx.font = '500 23px "Plus Jakarta Sans", sans-serif';
      drawWrappedText(step.desc, 80, sy + 142, WIDTH - 160, 33, "left");

      ctx.restore();
    });

    ctx.fillStyle = "#22D3EE";
    ctx.font = '700 22px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("⏱️ Setup takes less than 1 minute!", WIDTH / 2, 1185);
  }

  // SCENE 7 (53–60s): Grand CTA + Animated Bouncing Down Arrows for WhatsApp Status Link
  function drawScene7(t) {
    const local = t - 53.0;
    const pop = easeOutBack(clamp01(local / 0.6));

    ctx.save();
    ctx.translate(WIDTH / 2, 162);
    ctx.scale(pop, pop);
    ctx.fillStyle = "#22D3EE";
    ctx.font = '800 26px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("100% FREE ANDROID APP", 0, 0);
    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 46px "Space Grotesk", sans-serif';
    ctx.fillText("Abhi Download Karein!", 0, 56);
    ctx.restore();

    drawCartoonBoy(WIDTH / 2, 445, 172, t, true);

    drawRoundedRect(50, 672, WIDTH - 100, 290, 32);
    const ctaGrad = ctx.createLinearGradient(50, 672, WIDTH - 50, 962);
    ctaGrad.addColorStop(0, "rgba(124, 58, 237, 0.94)");
    ctaGrad.addColorStop(0.55, "rgba(37, 99, 235, 0.94)");
    ctaGrad.addColorStop(1, "rgba(8, 145, 178, 0.94)");
    ctx.fillStyle = ctaGrad;
    ctx.fill();
    ctx.strokeStyle = "#22D3EE";
    ctx.lineWidth = 4;
    ctx.stroke();

    ctx.fillStyle = "#FFFFFF";
    ctx.font = '800 35px "Space Grotesk", sans-serif';
    ctx.textAlign = "center";
    ctx.fillText("NEECHE LINK DABAKAR", WIDTH / 2, 738);
    ctx.fillText("DOWNLOAD KAREIN!", WIDTH / 2, 784);

    ctx.fillStyle = "#E0F2FE";
    ctx.font = '600 22px "Plus Jakarta Sans", sans-serif';
    ctx.fillText("Website kholein → Baby Call Assistant APK install karein", WIDTH / 2, 830);

    drawRoundedRect(82, 858, WIDTH - 164, 78, 39);
    ctx.fillStyle = "#060714";
    ctx.fill();
    ctx.strokeStyle = "#22D3EE";
    ctx.lineWidth = 2.5;
    ctx.stroke();
    ctx.fillStyle = "#22D3EE";
    ctx.font = '700 25px "JetBrains Mono", monospace';
    ctx.fillText(SITE_SHORT, WIDTH / 2, 906);

    const bounce = Math.sin(t * 7.5) * 16;
    ctx.fillStyle = "#22D3EE";
    ctx.font = '800 58px "Space Grotesk", sans-serif';
    ctx.fillText("👇   👇   👇", WIDTH / 2, 1082 + bounce);

    ctx.fillStyle = "#F8FAFC";
    ctx.font = '800 24px "Plus Jakarta Sans", sans-serif';
    ctx.fillText("TAP LINK BELOW • SHARE ON WHATSAPP!", WIDTH / 2, 1184);
  }

  function getSceneIndex(t) {
    const idx = SCENES.findIndex((s) => t >= s.start && t < s.end);
    return idx === -1 ? SCENES.length - 1 : idx;
  }

  function renderFrame(t) {
    const sceneIdx = getSceneIndex(t);
    drawBackground(t, sceneIdx + 1);

    switch (sceneIdx) {
      case 0:
        drawScene1(t);
        break;
      case 1:
        drawScene2(t);
        break;
      case 2:
        drawScene3(t);
        break;
      case 3:
        drawScene4(t);
        break;
      case 4:
        drawScene5(t);
        break;
      case 5:
        drawScene6(t);
        break;
      default:
        drawScene7(t);
        break;
    }

    if (progressBarEl) {
      progressBarEl.style.width = `${(clamp01(t / TOTAL_DURATION) * 100).toFixed(1)}%`;
    }
    if (timeLabelEl) {
      timeLabelEl.textContent = `${formatSec(t)} / 1:00`;
    }
  }

  function speakSceneIfNeeded(t) {
    if (!voiceToggle || !voiceToggle.checked || !("speechSynthesis" in window)) return;
    const idx = getSceneIndex(t);
    if (idx !== lastSpokenScene) {
      lastSpokenScene = idx;
      try {
        window.speechSynthesis.cancel();
        const utter = new SpeechSynthesisUtterance(SCENES[idx].voiceText);
        utter.lang = "hi-IN";
        utter.rate = 1.05;
        utter.pitch = 1.04;
        window.speechSynthesis.speak(utter);
      } catch (_) {
        // Ignore unsupported browsers
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
      masterGain.gain.value = 0.13;
      masterGain.connect(audioDest);
      masterGain.connect(audioCtx.destination);

      const melody = [
        261.63, 329.63, 392.0, 523.25, 392.0, 329.63, 293.66, 392.0,
        246.94, 329.63, 392.0, 493.88, 392.0, 329.63, 261.63, 329.63
      ];
      let step = 0;

      const playNote = () => {
        if (!audioCtx || audioCtx.state === "closed") return;
        const now = audioCtx.currentTime;
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.type = step % 4 === 0 ? "triangle" : "sine";
        osc.frequency.setValueAtTime(melody[step % melody.length], now);
        gain.gain.setValueAtTime(0.001, now);
        gain.gain.exponentialRampToValueAtTime(0.3, now + 0.04);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.36);
        osc.connect(gain);
        gain.connect(masterGain);
        osc.start(now);
        osc.stop(now + 0.4);
        step++;
      };

      playNote();
      beatTimer = setInterval(playNote, 400);
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
    if (playBtn) playBtn.textContent = "▶ Preview Full 1-Min Ad";
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
      const scene = SCENES[getSceneIndex(currentTime)];
      if (statusTextEl && scene) {
        const prefix = isRecording ? "🔴 Recording" : "▶ Playing";
        const pct = Math.round((currentTime / TOTAL_DURATION) * 100);
        statusTextEl.textContent = `${prefix} ${scene.name} (${pct}% • ${Math.ceil(TOTAL_DURATION - currentTime)}s left)`;
      }
      speakSceneIfNeeded(currentTime);
      rafId = requestAnimationFrame(tick);
    };
    rafId = requestAnimationFrame(tick);
  }

  function startPreview(fromSec = 0) {
    if (isRecording) return;
    if (isPlaying && fromSec === 0) {
      stopPlayback();
      if (statusTextEl) statusTextEl.textContent = "Preview paused. Tap Preview or Download 1-Min Video.";
      return;
    }
    stopPlayback();
    currentTime = fromSec;
    lastSpokenScene = -1;
    isPlaying = true;
    if (playBtn) playBtn.textContent = "⏹ Stop Preview";
    startAmbientMusic(false);
    runAnimationLoop(() => {
      if (statusTextEl) statusTextEl.textContent = "1-Minute Preview complete! Tap 'Download 1-Min Video' for WhatsApp Status.";
    });
  }

  function startRecordingDownload() {
    if (isRecording) return;
    if (typeof MediaRecorder === "undefined" || !canvas.captureStream) {
      showToast("Yeh browser direct video export support nahi karta — Chrome use karein.");
      return;
    }

    stopPlayback();
    isRecording = true;
    isPlaying = true;
    currentTime = 0;
    lastSpokenScene = -1;
    if (recordBtn) {
      recordBtn.disabled = true;
      recordBtn.textContent = "🔴 Recording 60s HD Video...";
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
        recordBtn.textContent = "⬇ Download 1-Min Video (60s)";
      }
      if (playBtn) playBtn.disabled = false;

      const blob = new Blob(chunks, { type: chosenMime || "video/webm" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = isMp4
        ? "Baby-Assistant-1Min-WhatsApp-Status.mp4"
        : "Baby-Assistant-1Min-WhatsApp-Status.webm";
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 10000);

      if (statusTextEl) {
        statusTextEl.textContent = "✅ 1-Minute HD Video downloaded! Ab WhatsApp Status par upload karein.";
      }
      showToast("1-Minute WhatsApp Status video download ho gaya!");
    };

    recorder.start(250);
    showToast("1-Minute (60s) HD video record ho raha hai — tab open rakhein...");

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

  if (playBtn) playBtn.addEventListener("click", () => startPreview(0));
  if (recordBtn) recordBtn.addEventListener("click", startRecordingDownload);

  // Scene jump pills
  document.querySelectorAll("[data-jump-sec]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const sec = parseFloat(btn.dataset.jumpSec || "0");
      if (isRecording) return;
      startPreview(sec);
    });
  });

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
      `Create a 60-second (1-minute) vertical 9:16 Pixar-style 3D animated advertisement video for WhatsApp Status in vibrant Midnight Indigo (#0A0B20), Electric Violet, and Neon Cyan lighting.\n` +
      `Host Character: A friendly, expressive young Indian cartoon boy in a purple-and-cyan tech hoodie holding a glowing Android smartphone, speaking enthusiastically in clear Hinglish with lip-sync and animated UI overlays.\n\n` +
      `[0:00-0:08] Hook: Cartoon boy smiles while showing busy-hand icons (bike riding, cooking, driving): "Haath busy hai, bike chala rahe ho ya khana bana rahe ho? Ab incoming call par phone chhune ki zarurat nahi! Miliye Baby Call Assistant se!"\n` +
      `[0:08-0:18] Feature 1 (Auto-Mic): Close-up phone mockup rings ("Mom Calling..."). Neon banner flashes "MIC STARTED AUTOMATICALLY — NO TAP NEEDED" with animated waveform and Continuous Ring Listening badge.\n` +
      `[0:18-0:28] Feature 2 (Answer, Ignore & Reject): Boy visually demonstrates 3 phone states: says "Baby, call attend karo" -> Green Call Connected ("Ji, call attend kar rahi hoon"); says "Baby, call attend mat karo" -> Amber Ignored (Negation-Smart!); says "Baby, call reject karo" -> Red Call Rejected.\n` +
      `[0:28-0:37] Feature 3 (Speaker & Mute): Active call UI tiles toggle live as boy says "Baby, speaker on karo / off karo" (loudspeaker glows cyan) and "Baby, mute karo / mute hatao" (mic toggles mute).\n` +
      `[0:37-0:45] Feature 4 (Test Lab, 3 Languages & Privacy): Showcase Test Lab call simulator, Hindi/Hinglish/English language chips, and glowing Privacy Shield ("100% On-Device • No Audio Upload • No Login • No API Key").\n` +
      `[0:45-0:53] Feature 5 (3-Step Setup): Animated 1-2-3 guide cards: Step 1 Download APK from website -> Step 2 Allow Mic & Phone permissions + Voice Assistant ON -> Step 3 Speak hands-free whenever a call arrives.\n` +
      `[0:53-1:00] Grand CTA: Cartoon boy excitedly points downward with glowing animated 👇 arrows toward the bottom link area displaying bold text "NEECHE LINK DABAKAR DOWNLOAD KAREIN" and URL "${SITE_SHORT}". Upbeat modern tech music throughout.`;

    copyPromptBtn.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(promptBox.value);
        showToast("1-Min AI Video prompt copied! (Veo / Runway / Kling mein paste karein)");
      } catch (_) {
        promptBox.select();
        document.execCommand("copy");
        showToast("AI Video prompt copied!");
      }
    });
  }
})();
