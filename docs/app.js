const COPY = {
  hinglish: {
    badge: "Android 7.0+ • Auto-Mic on Incoming Call • 100% Free",
    heroTitle: 'Incoming Call Aate Hi Bas Bolo — <span class="gradient-text">“Baby, Call Attend Karo”</span>',
    heroDesc:
      "Haath busy ho, bike chala rahe ho ya khana bana rahe ho — jaise hi incoming call aata hai, Baby Assistant ka mic automatically ON ho jata hai. Hindi, Hinglish ya English mein bolkar call attend, reject, speaker ON ya mute karein.",
    downloadBtn: "Download Baby Assistant APK",
    tryDemoBtn: "Live Demo Try Karein",
    featuresTag: "Key Features",
    featuresTitle: "Full Hands-Free Call Control — Bina Screen Touch Kiye",
    featuresSub:
      "Fast on-device command engine jo Hindi, English aur Hinglish samajhta hai — koi login, subscription ya API key nahi chahiye.",
    commandsTag: "Voice Commands",
    commandsTitle: "Natural Hindi, Hinglish aur English Commands",
    commandsSub:
      "Negation-smart engine: “call attend mat karo” aur “call attend karo” mein kabhi confuse nahi hota.",
    installTag: "Install Guide",
    installTitle: "3 Easy Steps Mein Phone Par Install Karein",
    installSub:
      "Direct Android APK download — bas 1 minute mein setup complete ho jata hai.",
    downloadBoxTitle: "Latest Official Android APK Download Karein",
    downloadBoxSub:
      "Har naye GitHub build ke saath yahan latest verified Baby Assistant APK automatically update hota hai.",
    shareBtn: "Website Link Share Karein",
    releasesBtn: "GitHub Releases Dekhein",
    qrTitle: "Scan to Download on Phone",
    qrDesc: "Phone camera se scan karein aur seedha APK download karein",
    copyApkBtn: "Copy Direct APK Link",
    faqTag: "FAQ",
    faqTitle: "Aam Sawaal (Questions & Answers)"
  },
  english: {
    badge: "Android 7.0+ • Auto-Mic on Incoming Call • 100% Free",
    heroTitle: 'Hands-Free Incoming Calls — Just Say <span class="gradient-text">“Baby, Answer the Call”</span>',
    heroDesc:
      "When your hands are busy driving, cooking, or working, Baby Assistant automatically turns on the microphone the moment an incoming call rings. Answer, ignore, reject, toggle speaker, or mute using Hindi, English, or Hinglish.",
    downloadBtn: "Download Baby Assistant APK",
    tryDemoBtn: "Try Live Demo",
    featuresTag: "Key Features",
    featuresTitle: "Effortless Hands-Free Calling Without Touching Your Phone",
    featuresSub:
      "Built for speed and privacy with an on-device multilingual command engine — no sign-in, no subscription, and no API key needed.",
    commandsTag: "Voice Commands",
    commandsTitle: "Natural Hindi, Hinglish & English Commands",
    commandsSub:
      "Negation-aware matching accurately separates “do not answer” from “answer the call.”",
    installTag: "Install Guide",
    installTitle: "Install on Android in 3 Quick Steps",
    installSub:
      "Direct APK download ready for Android 7.0+ devices in under a minute.",
    downloadBoxTitle: "Download the Latest Official Android APK",
    downloadBoxSub:
      "Every new GitHub build automatically publishes the latest verified Baby Assistant APK here.",
    shareBtn: "Share Download Page",
    releasesBtn: "View GitHub Releases",
    qrTitle: "Scan to Download on Phone",
    qrDesc: "Scan with your phone camera to download the APK directly",
    copyApkBtn: "Copy Direct APK Link",
    faqTag: "FAQ",
    faqTitle: "Frequently Asked Questions"
  }
};

const SIM_SCENARIOS = {
  ringing: {
    pillText: "Incoming Call • Auto-Mic ON",
    pillClass: "call-status-pill ringing",
    title: "Baby Mic Automatically Listening...",
    text: 'Incoming call detect hote hi mic ON ho gaya. Neeche kisi bhi command par tap karke response dekhein!'
  },
  answer: {
    pillText: "Call Connected",
    pillClass: "call-status-pill",
    title: 'Heard: "Baby, call attend karo."',
    text: 'Intent: ANSWER_CALL • Reply: "Ji, call attend kar rahi hoon." (TelecomManager accepts ringing call)'
  },
  ignore: {
    pillText: "Left Unanswered",
    pillClass: "call-status-pill",
    title: 'Heard: "Baby, call attend mat karo."',
    text: 'Intent: DO_NOT_ANSWER • Reply: "Theek hai, call attend nahi karungi." (Mic stops for this ring)'
  },
  reject: {
    pillText: "Call Rejected",
    pillClass: "call-status-pill rejected",
    title: 'Heard: "Baby, call reject karo."',
    text: 'Intent: REJECT_CALL • Reply: "Call reject kar diya gaya." (Ends incoming call safely)'
  },
  speaker: {
    pillText: "Call Connected • Speaker ON",
    pillClass: "call-status-pill",
    title: 'Heard: "Baby, speaker on karo."',
    text: 'Intent: SPEAKER_ON • Reply: "Speaker on kar diya hai." (Routes audio to loudspeaker)'
  },
  mute: {
    pillText: "Call Connected • Mic Muted",
    pillClass: "call-status-pill",
    title: 'Heard: "Baby, mute karo."',
    text: 'Intent: MUTE_MIC • Reply: "Mic mute kar diya hai." (AudioManager microphone mute)'
  }
};

function showToast(message) {
  const toast = document.getElementById("toast");
  if (!toast) return;
  toast.textContent = message;
  toast.classList.add("show");
  clearTimeout(window.__toastTimer);
  window.__toastTimer = setTimeout(() => toast.classList.remove("show"), 2800);
}

function setLanguage(lang) {
  const dict = COPY[lang] || COPY.hinglish;
  document.querySelectorAll("[data-i18n]").forEach((el) => {
    const key = el.getAttribute("data-i18n");
    if (dict[key]) {
      if (key === "heroTitle") {
        el.innerHTML = dict[key];
      } else {
        el.textContent = dict[key];
      }
    }
  });
  document.querySelectorAll(".lang-btn").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.lang === lang);
  });
}

function updateSimulation(stateKey) {
  const state = SIM_SCENARIOS[stateKey] || SIM_SCENARIOS.ringing;
  const pill = document.getElementById("sim-status-pill");
  const title = document.getElementById("sim-response-title");
  const text = document.getElementById("sim-response-text");
  if (pill) {
    pill.className = state.pillClass;
    pill.textContent = state.pillText;
  }
  if (title) title.textContent = state.title;
  if (text) text.textContent = state.text;
}

const DEFAULT_PUBLIC_APK_URL =
  "https://github.com/khilesh6263/-babu/releases/latest/download/Baby-Assistant.apk";

let resolvedApkDownloadUrl = DEFAULT_PUBLIC_APK_URL;

function resolveScannableApkUrl(rawHref) {
  if (!rawHref) return DEFAULT_PUBLIC_APK_URL;
  if (/^https?:\/\//i.test(rawHref)) return rawHref;
  if (window.location.protocol === "http:" || window.location.protocol === "https:") {
    try {
      return new URL(rawHref, window.location.href).href;
    } catch (_) {
      return DEFAULT_PUBLIC_APK_URL;
    }
  }
  return DEFAULT_PUBLIC_APK_URL;
}

function renderApkQrCode(url) {
  const container = document.getElementById("apk-qr-code");
  if (!container) return;

  resolvedApkDownloadUrl = url;
  container.setAttribute("aria-label", `QR code to download Baby Assistant APK (${url})`);

  if (typeof window.qrcode === "function") {
    try {
      const qr = window.qrcode(0, "M");
      qr.addData(url);
      qr.make();
      container.innerHTML = qr.createSvgTag({ scalable: true, margin: 1 });
      const svg = container.querySelector("svg");
      if (svg) {
        svg.setAttribute("role", "presentation");
        svg.setAttribute("aria-hidden", "true");
      }
      return;
    } catch (_) {
      // Fall through to remote image fallback
    }
  }

  const img = document.createElement("img");
  img.alt = "QR code to download Baby Assistant APK";
  img.width = 132;
  img.height = 132;
  img.loading = "lazy";
  img.src = `https://api.qrserver.com/v1/create-qr-code/?size=240x240&margin=6&data=${encodeURIComponent(url)}`;
  container.innerHTML = "";
  container.appendChild(img);
}

async function loadReleaseInfo() {
  renderApkQrCode(DEFAULT_PUBLIC_APK_URL);
  try {
    const res = await fetch("./downloads/release-info.json", { cache: "no-store" });
    if (!res.ok) return;
    const info = await res.json();

    const apkHref = info.apkUrl || DEFAULT_PUBLIC_APK_URL;
    document.querySelectorAll("[data-apk-download]").forEach((link) => {
      link.setAttribute("href", apkHref);
    });
    renderApkQrCode(resolveScannableApkUrl(apkHref));

    const releasesHref = info.fallbackReleasesUrl || "https://github.com/khilesh6263/-babu/releases";
    document.querySelectorAll("[data-releases-link]").forEach((link) => {
      link.setAttribute("href", releasesHref);
    });

    const versionEl = document.getElementById("spec-version");
    const sizeEl = document.getElementById("spec-size");
    const updatedEl = document.getElementById("spec-updated");
    const shaEl = document.getElementById("spec-sha256");

    if (versionEl && info.version) versionEl.textContent = `v${info.version}`;
    if (sizeEl && info.apkSize) sizeEl.textContent = info.apkSize;
    if (updatedEl && info.updatedAt) updatedEl.textContent = info.updatedAt;
    if (shaEl && info.sha256) {
      shaEl.textContent = info.sha256.length > 24 ? `${info.sha256.slice(0, 24)}…` : info.sha256;
      shaEl.title = info.sha256;
    }
  } catch (_) {
    // Keep default fallback GitHub Release link & QR code
  }
}

document.addEventListener("DOMContentLoaded", () => {
  loadReleaseInfo();

  document.querySelectorAll(".lang-btn").forEach((btn) => {
    btn.addEventListener("click", () => setLanguage(btn.dataset.lang));
  });

  document.querySelectorAll("[data-sim]").forEach((btn) => {
    btn.addEventListener("click", () => {
      updateSimulation(btn.dataset.sim);
    });
  });

  const copyApkBtn = document.getElementById("copy-apk-url-btn");
  if (copyApkBtn) {
    copyApkBtn.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(resolvedApkDownloadUrl);
        showToast("Direct APK link copied!");
      } catch (_) {
        showToast("APK link copy nahi ho paya — Download button use karein.");
      }
    });
  }

  const shareBtn = document.getElementById("share-site-btn");
  if (shareBtn) {
    shareBtn.addEventListener("click", async () => {
      const shareData = {
        title: "Baby Assistant — Hands-Free Voice Call Assistant APK",
        text: "Incoming call aate hi voice se call attend, reject, speaker ON ya mute karein! Download Baby Assistant Android APK:",
        url: window.location.href
      };
      if (navigator.share) {
        try {
          await navigator.share(shareData);
          return;
        } catch (_) {
          // fallback to clipboard
        }
      }
      try {
        await navigator.clipboard.writeText(window.location.href);
        showToast("Download link copied! Ab WhatsApp par share kar sakte hain.");
      } catch (_) {
        showToast("Link copy karne ke liye browser address bar use karein.");
      }
    });
  }

  document.querySelectorAll("[data-apk-download]").forEach((btn) => {
    btn.addEventListener("click", () => {
      showToast("Baby Assistant APK download start ho raha hai...");
    });
  });
});
