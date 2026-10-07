// SoundMorph - Audio Engine & Voice Changer Logic
(() => {
  let audioContext = null;
  let mediaStream = null;
  let mediaRecorder = null;
  let recordedChunks = [];
  let sourceAudioBuffer = null;
  let currentSourceNode = null;
  let currentPlayingEffectId = null;

  let analyser = null;
  let animationFrameId = null;
  let isRecording = false;
  let recordSeconds = 0;
  let recordInterval = null;

  // DOM Elements
  const recordBtn = document.getElementById('recordBtn');
  const recordTimer = document.getElementById('recordTimer');
  const statusText = document.getElementById('statusText');
  const audioFileInput = document.getElementById('audioFileInput');
  const effectsSection = document.getElementById('effectsSection');
  const effectsGrid = document.getElementById('effectsGrid');
  const reRecordBtn = document.getElementById('reRecordBtn');
  const stopAllBtn = document.getElementById('stopAllBtn');
  const playingEffectLabel = document.getElementById('playingEffectLabel');
  const canvas = document.getElementById('visualizerCanvas');
  const canvasCtx = canvas.getContext('2d');

  // List of Voice Effects
  const EFFECTS = [
    { id: 'original', name: 'Giọng Gốc', emoji: '🎧', desc: 'Âm thanh tự nhiên ban đầu', rate: 1.0 },
    { id: 'chipmunk', name: 'Sóc Chuột', emoji: '🐿️', desc: 'Giọng cao vút lảnh lót cực hài', rate: 1.55 },
    { id: 'monster', name: 'Quái Vật', emoji: '👹', desc: 'Trầm đục, rùng rợn và uy lực', rate: 0.72 },
    { id: 'robot', name: 'Người Máy', emoji: '🤖', desc: 'Biến điệu giọng kim loại công nghệ', rate: 1.0 },
    { id: 'alien', name: 'Người Ngoài Hành Tinh', emoji: '👽', desc: 'Biến điệu rung rinh phong cách UFO', rate: 1.0 },
    { id: 'echo', name: 'Tiếng Vang (Echo)', emoji: '🦇', desc: 'Vang vọng phản xạ phòng kín', rate: 1.0 },
    { id: 'cave', name: 'Hang Động (Reverb)', emoji: '🗻', desc: 'Âm vang sâu thẳm giữa hẻm núi', rate: 1.0 },
    { id: 'reverse', name: 'Tua Ngược', emoji: '🔄', desc: 'Đọc ngược từ đuôi lên đầu cực dị', rate: 1.0 },
    { id: 'fast', name: 'Tia Chớp', emoji: '⚡', desc: 'Tốc độ nói siêu nhanh 1.4x', rate: 1.4 },
    { id: 'slow', name: 'Say Xỉn (Slow)', emoji: '🐢', desc: 'Lề mề chậm chạp uể oải 0.7x', rate: 0.7 },
    { id: 'underwater', name: 'Dưới Nước', emoji: '🌊', desc: 'Âm thanh trầm chìm dưới đáy biển', rate: 1.0 },
    { id: 'radio', name: 'Bộ Đàm / Radio', emoji: '📻', desc: 'Dải tần vô tuyến cổ điển rè nhẹ', rate: 1.0 },
    { id: 'megaphone', name: 'Loa Phóng Thanh', emoji: '📢', desc: 'Loa phường chói chang vang xa', rate: 1.0 },
    { id: 'ghost', name: 'Bóng Ma (Ghost)', emoji: '👻', desc: 'Âm hưởng ma mị, vang rợn người', rate: 0.85 },
    { id: 'girly', name: 'Nữ Điệu Dẹo', emoji: '🎀', desc: 'Giọng nữ nũng nịu, ngọt ngào, điệu đà', rate: 1.28 }
  ];

  // Initialize
  function getAudioContext() {
    if (!audioContext) {
      audioContext = new (window.AudioContext || window.webkitAudioContext)();
    }
    if (audioContext.state === 'suspended') {
      audioContext.resume();
    }
    return audioContext;
  }

  // Draw Audio Visualizer (Live Waveform)
  function startVisualizer(stream) {
    const ctx = getAudioContext();
    analyser = ctx.createAnalyser();
    analyser.fftSize = 256;
    const source = ctx.createMediaStreamSource(stream);
    source.connect(analyser);

    const bufferLength = analyser.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);

    function draw() {
      animationFrameId = requestAnimationFrame(draw);
      analyser.getByteTimeDomainData(dataArray);

      canvasCtx.fillStyle = 'rgba(11, 15, 25, 0.4)';
      canvasCtx.fillRect(0, 0, canvas.width, canvas.height);

      canvasCtx.lineWidth = 2.5;
      canvasCtx.strokeStyle = isRecording ? '#ef4444' : '#06b6d4';
      canvasCtx.beginPath();

      const sliceWidth = canvas.width * 1.0 / bufferLength;
      let x = 0;

      for (let i = 0; i < bufferLength; i++) {
        const v = dataArray[i] / 128.0;
        const y = v * (canvas.height / 2);

        if (i === 0) canvasCtx.moveTo(x, y);
        else canvasCtx.lineTo(x, y);

        x += sliceWidth;
      }

      canvasCtx.lineTo(canvas.width, canvas.height / 2);
      canvasCtx.stroke();
    }
    draw();
  }

  function stopVisualizer() {
    if (animationFrameId) {
      cancelAnimationFrame(animationFrameId);
      animationFrameId = null;
    }
    canvasCtx.clearRect(0, 0, canvas.width, canvas.height);
  }

  // Recording Handlers
  async function startRecording() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      mediaStream = stream;
      getAudioContext();

      recordedChunks = [];
      mediaRecorder = new MediaRecorder(stream);
      mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) recordedChunks.push(e.data);
      };

      mediaRecorder.onstop = async () => {
        const audioBlob = new Blob(recordedChunks, { type: 'audio/webm' });
        const arrayBuffer = await audioBlob.arrayBuffer();
        const ctx = getAudioContext();
        sourceAudioBuffer = await ctx.decodeAudioData(arrayBuffer);
        showEffectsUI();
      };

      mediaRecorder.start();
      isRecording = true;
      recordBtn.classList.add('recording');
      statusText.textContent = 'Đang ghi âm... Chạm lại để hoàn tất!';
      statusText.style.color = '#ef4444';

      startVisualizer(stream);

      // Start timer
      recordSeconds = 0;
      recordTimer.textContent = '00:00';
      clearInterval(recordInterval);
      recordInterval = setInterval(() => {
        recordSeconds++;
        const mins = String(Math.floor(recordSeconds / 60)).padStart(2, '0');
        const secs = String(recordSeconds % 60).padStart(2, '0');
        recordTimer.textContent = `${mins}:${secs}`;
      }, 1000);
    } catch (err) {
      alert('Không thể mở micro: ' + err.message + '\nVui lòng cấp quyền micro cho trình duyệt!');
    }
  }

  function stopRecording() {
    if (mediaRecorder && isRecording) {
      mediaRecorder.stop();
      isRecording = false;
      recordBtn.classList.remove('recording');
      clearInterval(recordInterval);
      stopVisualizer();

      if (mediaStream) {
        mediaStream.getTracks().forEach(track => track.stop());
        mediaStream = null;
      }
    }
  }

  recordBtn.addEventListener('click', () => {
    if (isRecording) {
      stopRecording();
    } else {
      stopAudioPlayback();
      startRecording();
    }
  });

  // Import Audio File Handler
  audioFileInput.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    try {
      const ctx = getAudioContext();
      statusText.textContent = 'Đang tải file âm thanh...';
      const arrayBuffer = await file.arrayBuffer();
      sourceAudioBuffer = await ctx.decodeAudioData(arrayBuffer);
      showEffectsUI();
    } catch (err) {
      alert('Lỗi định dạng file âm thanh: ' + err.message);
    }
  });

  function showEffectsUI() {
    effectsSection.style.display = 'block';
    renderEffectsList();
    statusText.textContent = 'Đã sẵn sàng! Hãy chọn hiệu ứng bên dưới:';
    statusText.style.color = '#67e8f9';
    // Scroll smoothly to effects
    effectsSection.scrollIntoView({ behavior: 'smooth' });
  }

  reRecordBtn.addEventListener('click', () => {
    stopAudioPlayback();
    effectsSection.style.display = 'none';
    statusText.textContent = 'Chạm nút Mic đỏ để bắt đầu ghi âm';
    statusText.style.color = '#67e8f9';
    recordTimer.textContent = '00:00';
  });

  // Render list of effect cards
  function renderEffectsList() {
    effectsGrid.innerHTML = '';
    EFFECTS.forEach(eff => {
      const card = document.createElement('div');
      card.className = 'effect-card';
      card.id = `card-${eff.id}`;

      card.innerHTML = `
        <div class="effect-info">
          <div class="effect-emoji">${eff.emoji}</div>
          <div class="effect-texts">
            <span class="effect-title">${eff.name}</span>
            <span class="effect-desc">${eff.desc}</span>
          </div>
        </div>
        <div class="effect-actions">
          <button class="action-btn play-btn" data-action="play" data-id="${eff.id}" title="Nghe thử">▶</button>
          <button class="action-btn icon-btn" data-action="download" data-id="${eff.id}" title="Tải file WAV">💾</button>
          <button class="action-btn icon-btn" data-action="share" data-id="${eff.id}" title="Chia sẻ">📤</button>
        </div>
      `;

      effectsGrid.appendChild(card);
    });

    // Delegate event listener
    effectsGrid.onclick = async (e) => {
      const btn = e.target.closest('button');
      if (!btn) return;
      const action = btn.dataset.action;
      const effectId = btn.dataset.id;
      const effect = EFFECTS.find(x => x.id === effectId);

      if (action === 'play') {
        if (currentPlayingEffectId === effectId) {
          stopAudioPlayback();
        } else {
          playEffect(effect);
        }
      } else if (action === 'download') {
        downloadEffectAudio(effect);
      } else if (action === 'share') {
        shareEffectAudio(effect);
      }
    };
  }

  // Core Audio Graph Builder (Web Audio API)
  function createEffectGraph(ctx, sourceNode, effect) {
    let lastNode = sourceNode;

    switch (effect.id) {
      case 'chipmunk':
        sourceNode.playbackRate.value = 1.55;
        break;

      case 'monster': {
        sourceNode.playbackRate.value = 0.72;
        // Lowpass filter
        const filter = ctx.createBiquadFilter();
        filter.type = 'lowpass';
        filter.frequency.value = 1200;
        // Distortion
        const waveShaper = ctx.createWaveShaper();
        waveShaper.curve = makeDistortionCurve(30);
        lastNode.connect(filter);
        filter.connect(waveShaper);
        lastNode = waveShaper;
        break;
      }

      case 'robot': {
        // Ring modulation: 50Hz Carrier oscillator
        const osc = ctx.createOscillator();
        const oscGain = ctx.createGain();
        osc.frequency.value = 50;
        osc.type = 'sine';

        const ringGain = ctx.createGain();
        ringGain.gain.value = 0.0;

        lastNode.connect(ringGain.gain);
        osc.connect(ringGain);
        osc.start();
        lastNode = ringGain;
        break;
      }

      case 'alien': {
        // Vibrato / Tremolo LFO
        const lfo = ctx.createOscillator();
        const lfoGain = ctx.createGain();
        const tremoloGain = ctx.createGain();

        lfo.frequency.value = 14;
        lfoGain.gain.value = 0.5;
        tremoloGain.gain.value = 0.5;

        lfo.connect(lfoGain);
        lfoGain.connect(tremoloGain.gain);
        lastNode.connect(tremoloGain);
        lfo.start();
        lastNode = tremoloGain;
        break;
      }

      case 'echo': {
        const delay = ctx.createDelay(1.0);
        delay.delayTime.value = 0.25;
        const feedback = ctx.createGain();
        feedback.gain.value = 0.45;

        const merger = ctx.createGain();
        lastNode.connect(merger);
        lastNode.connect(delay);
        delay.connect(feedback);
        feedback.connect(delay);
        delay.connect(merger);
        lastNode = merger;
        break;
      }

      case 'cave': {
        // Multi-tap reverb
        const merger = ctx.createGain();
        lastNode.connect(merger);
        const delays = [0.04, 0.09, 0.16, 0.24, 0.35];
        const decays = [0.5, 0.38, 0.26, 0.18, 0.1];

        delays.forEach((dTime, idx) => {
          const d = ctx.createDelay();
          d.delayTime.value = dTime;
          const g = ctx.createGain();
          g.gain.value = decays[idx];
          lastNode.connect(d);
          d.connect(g);
          g.connect(merger);
        });
        lastNode = merger;
        break;
      }

      case 'fast':
        sourceNode.playbackRate.value = 1.4;
        break;

      case 'slow':
        sourceNode.playbackRate.value = 0.7;
        break;

      case 'underwater': {
        const lp = ctx.createBiquadFilter();
        lp.type = 'lowpass';
        lp.frequency.value = 450;
        lastNode.connect(lp);
        lastNode = lp;
        break;
      }

      case 'radio': {
        const bp = ctx.createBiquadFilter();
        bp.type = 'bandpass';
        bp.frequency.value = 1500;
        bp.Q.value = 2.0;

        const shaper = ctx.createWaveShaper();
        shaper.curve = makeDistortionCurve(15);
        lastNode.connect(bp);
        bp.connect(shaper);
        lastNode = shaper;
        break;
      }

      case 'megaphone': {
        const bp = ctx.createBiquadFilter();
        bp.type = 'bandpass';
        bp.frequency.value = 1800;
        bp.Q.value = 1.5;

        const dist = ctx.createWaveShaper();
        dist.curve = makeDistortionCurve(45);
        const boost = ctx.createGain();
        boost.gain.value = 1.8;

        lastNode.connect(bp);
        bp.connect(dist);
        dist.connect(boost);
        lastNode = boost;
        break;
      }

      case 'ghost': {
        sourceNode.playbackRate.value = 0.85;
        const delay = ctx.createDelay(1.0);
        delay.delayTime.value = 0.32;
        const fb = ctx.createGain();
        fb.gain.value = 0.55;
        const merger = ctx.createGain();

        lastNode.connect(merger);
        lastNode.connect(delay);
        delay.connect(fb);
        fb.connect(delay);
        delay.connect(merger);
        lastNode = merger;
        break;
      }

      case 'girly': {
        sourceNode.playbackRate.value = 1.28;

        // Treble & airy boost
        const highShelf = ctx.createBiquadFilter();
        highShelf.type = 'highshelf';
        highShelf.frequency.value = 3000;
        highShelf.gain.value = 4.5;

        // "Dẹo dẹo" subtle chorus & swaying vibrato
        const delay = ctx.createDelay(0.05);
        delay.delayTime.value = 0.0035;

        const lfo = ctx.createOscillator();
        const lfoGain = ctx.createGain();
        lfo.frequency.value = 3.2; // 3.2Hz gentle sway
        lfoGain.gain.value = 0.0018; // 1.8ms modulation depth
        lfo.connect(delay.delayTime);
        lfo.start();

        // Sweet subtle room reverb
        const roomDelay = ctx.createDelay(0.5);
        roomDelay.delayTime.value = 0.065;
        const roomGain = ctx.createGain();
        roomGain.gain.value = 0.22;

        const merger = ctx.createGain();
        lastNode.connect(highShelf);
        highShelf.connect(merger);
        highShelf.connect(delay);
        delay.connect(merger);

        // Add subtle room touch
        merger.connect(roomDelay);
        roomDelay.connect(roomGain);
        roomGain.connect(merger);

        lastNode = merger;
        break;
      }
    }

    return lastNode;
  }

  function makeDistortionCurve(amount) {
    const k = typeof amount === 'number' ? amount : 50;
    const n_samples = 44100;
    const curve = new Float32Array(n_samples);
    const deg = Math.PI / 180;
    for (let i = 0; i < n_samples; ++i) {
      const x = (i * 2) / n_samples - 1;
      curve[i] = ((3 + k) * x * 20 * deg) / (Math.PI + k * Math.abs(x));
    }
    return curve;
  }

  // Create reversed buffer copy for Reverse effect
  function getEffectBuffer(ctx, effect) {
    if (effect.id === 'reverse') {
      const revBuffer = ctx.createBuffer(
        sourceAudioBuffer.numberOfChannels,
        sourceAudioBuffer.length,
        sourceAudioBuffer.sampleRate
      );
      for (let ch = 0; ch < sourceAudioBuffer.numberOfChannels; ch++) {
        const src = sourceAudioBuffer.getChannelData(ch);
        const dst = revBuffer.getChannelData(ch);
        const len = src.length;
        for (let i = 0; i < len; i++) {
          dst[i] = src[len - 1 - i];
        }
      }
      return revBuffer;
    }
    return sourceAudioBuffer;
  }

  // Playback Control
  function playEffect(effect) {
    stopAudioPlayback();
    if (!sourceAudioBuffer) return;

    const ctx = getAudioContext();
    const bufferToPlay = getEffectBuffer(ctx, effect);

    const source = ctx.createBufferSource();
    source.buffer = bufferToPlay;

    const outputNode = createEffectGraph(ctx, source, effect);
    outputNode.connect(ctx.destination);

    source.onended = () => {
      stopAudioPlayback();
    };

    source.start();
    currentSourceNode = source;
    currentPlayingEffectId = effect.id;

    // Update UI
    playingEffectLabel.textContent = `Đang phát: ${effect.emoji} ${effect.name}`;
    stopAllBtn.style.display = 'inline-block';

    const card = document.getElementById(`card-${effect.id}`);
    if (card) {
      card.classList.add('active');
      const pBtn = card.querySelector('.play-btn');
      if (pBtn) {
        pBtn.textContent = '⏹';
        pBtn.classList.add('playing');
      }
    }
  }

  function stopAudioPlayback() {
    if (currentSourceNode) {
      try { currentSourceNode.stop(); } catch (_) {}
      currentSourceNode = null;
    }
    if (currentPlayingEffectId) {
      const card = document.getElementById(`card-${currentPlayingEffectId}`);
      if (card) {
        card.classList.remove('active');
        const pBtn = card.querySelector('.play-btn');
        if (pBtn) {
          pBtn.textContent = '▶';
          pBtn.classList.remove('playing');
        }
      }
      currentPlayingEffectId = null;
    }
    playingEffectLabel.textContent = 'Chưa chọn hiệu ứng';
    stopAllBtn.style.display = 'none';
  }

  stopAllBtn.addEventListener('click', stopAudioPlayback);

  // Render & Export to WAV Blob
  async function renderProcessedAudioBlob(effect) {
    if (!sourceAudioBuffer) return null;

    let rate = effect.rate || 1.0;
    const duration = (sourceAudioBuffer.duration / rate) + 0.6; // add margin for reverb/echo
    const sampleRate = 44100;
    const offlineCtx = new OfflineAudioContext(1, Math.ceil(sampleRate * duration), sampleRate);

    const bufferToUse = getEffectBuffer(offlineCtx, effect);
    const source = offlineCtx.createBufferSource();
    source.buffer = bufferToUse;

    const outputNode = createEffectGraph(offlineCtx, source, effect);
    outputNode.connect(offlineCtx.destination);
    source.start();

    const renderedBuffer = await offlineCtx.startRendering();
    return audioBufferToWavBlob(renderedBuffer);
  }

  // Convert AudioBuffer to RIFF/WAV Blob
  function audioBufferToWavBlob(buffer) {
    const numOfChan = 1;
    const length = buffer.length * numOfChan * 2 + 44;
    const out = new DataView(new ArrayBuffer(length));
    const channels = [buffer.getChannelData(0)];
    const sampleRate = buffer.sampleRate;

    function setUint16(data) { out.setUint16(pos, data, true); pos += 2; }
    function setUint32(data) { out.setUint32(pos, data, true); pos += 4; }

    let pos = 0;
    // RIFF chunk
    out.setUint32(pos, 0x46464952, false); pos += 4; // "RIFF"
    setUint32(length - 8);
    out.setUint32(pos, 0x45564157, false); pos += 4; // "WAVE"

    // FMT chunk
    out.setUint32(pos, 0x20746d66, false); pos += 4; // "fmt "
    setUint32(16); // Subchunk1Size (16 for PCM)
    setUint16(1);  // AudioFormat (1 = PCM)
    setUint16(numOfChan);
    setUint32(sampleRate);
    setUint32(sampleRate * 2 * numOfChan); // ByteRate
    setUint16(numOfChan * 2); // BlockAlign
    setUint16(16); // BitsPerSample

    // DATA chunk
    out.setUint32(pos, 0x61746164, false); pos += 4; // "data"
    setUint32(length - pos - 4);

    // PCM Samples
    let offset = 0;
    while (offset < buffer.length) {
      for (let i = 0; i < numOfChan; i++) {
        let sample = Math.max(-1, Math.min(1, channels[i][offset]));
        sample = (0.5 + sample < 0 ? sample * 32768 : sample * 32767) | 0;
        out.setInt16(pos, sample, true);
        pos += 2;
      }
      offset++;
    }

    return new Blob([out.buffer], { type: 'audio/wav' });
  }

  // Download Handler
  async function downloadEffectAudio(effect) {
    const blob = await renderProcessedAudioBlob(effect);
    if (!blob) return;

    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = url;
    a.download = `SoundMorph_${effect.id}_${Date.now()}.wav`;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }, 100);
  }

  // Share Handler
  async function shareEffectAudio(effect) {
    const blob = await renderProcessedAudioBlob(effect);
    if (!blob) return;

    const filename = `SoundMorph_${effect.id}.wav`;
    const file = new File([blob], filename, { type: 'audio/wav' });

    if (navigator.canShare && navigator.canShare({ files: [file] })) {
      try {
        await navigator.share({
          files: [file],
          title: `Giọng đổi: ${effect.name}`,
          text: `Nghe giọng đổi thú vị này được tạo từ SoundMorph!`
        });
      } catch (e) {
        if (e.name !== 'AbortError') alert('Không thể chia sẻ: ' + e.message);
      }
    } else {
      // Fallback: download directly
      downloadEffectAudio(effect);
      alert('Trình duyệt không hỗ trợ chia sẻ trực tiếp, file đã được tải về máy của bạn!');
    }
  }

  // Register Service Worker for Offline PWA
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('sw.js').catch(() => {});
    });
  }
})();
