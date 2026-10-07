package com.soundmorph.voicechanger.model

enum class VoiceEffect(
    val title: String,
    val description: String,
    val emoji: String,
    val speedMultiplier: Float = 1.0f,
    val pitchShiftRatio: Float = 1.0f
) {
    ORIGINAL("Giọng Gốc", "Giữ nguyên âm thanh ban đầu", "🎧", 1.0f, 1.0f),
    CHIPMUNK("Sóc Chuột (Helium)", "Giọng cao lảnh lót cực vui nhộn", "🐿️", 1.5f, 1.5f),
    MONSTER("Quái Vật (Monster)", "Giọng trầm đục, rùng rợn và uy lực", "👹", 0.7f, 0.7f),
    ROBOT("Người Máy (Robot)", "Hiệu ứng giọng kim loại máy móc", "🤖", 1.0f, 1.0f),
    ALIEN("Người Ngoài Hành Tinh", "Biến điệu rung rinh phong cách vũ trụ", "👽", 1.0f, 1.0f),
    ECHO("Tiếng Vang (Echo)", "Hiệu ứng tiếng vang vọng phòng lớn", "🦇", 1.0f, 1.0f),
    CAVE("Hang Động (Reverb)", "Âm thanh vang sâu trong hẻm núi", "🗻", 1.0f, 1.0f),
    REVERSE("Tua Ngược (Reverse)", "Đọc ngược âm thanh từ đuôi lên đầu", "🔄", 1.0f, 1.0f),
    FAST("Tia Chớp (Fast)", "Tốc độ nói siêu nhanh 1.4x", "⚡", 1.4f, 1.0f),
    SLOW("Say Xỉn (Slow)", "Tốc độ chậm chạp lề mề 0.7x", "🐢", 0.7f, 1.0f),
    UNDERWATER("Dưới Nước", "Âm thanh ngập chìm dưới đáy biển", "🌊", 1.0f, 1.0f),
    RADIO("Bộ Đàm / Radio", "Dải tần radio cổ điển, hơi rè", "📻", 1.0f, 1.0f),
    MEGAPHONE("Loa Phóng Thanh", "Âm thanh loa phường chói chang", "📢", 1.0f, 1.0f),
    GHOST("Bóng Ma (Ghost)", "Âm hưởng ma mị, huyền ảo", "👻", 0.85f, 0.85f),
    GIRLY_FLIRTY("Nữ Điệu Dẹo", "Giọng nữ nũng nịu, ngọt ngào, điệu đà", "🎀", 1.28f, 1.28f);
}

