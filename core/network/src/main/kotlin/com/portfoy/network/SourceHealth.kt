package com.portfoy.network

import com.portfoy.model.SourceId
import java.time.Duration
import java.time.Instant

/**
 * Her kaynak için son başarılı çekim zamanını tutar. Bir kaynak 24 saat veri vermezse kullanıcıya
 * tek seferlik bir uyarı gösterilir (doküman 9.4/5).
 */
class SourceHealth(private val outageThreshold: Duration = Duration.ofHours(24)) {
    private val lastSuccess = mutableMapOf<SourceId, Instant>()
    private val firstFailure = mutableMapOf<SourceId, Instant>()
    private val warned = mutableSetOf<SourceId>()

    @Synchronized
    fun recordSuccess(source: SourceId, now: Instant) {
        lastSuccess[source] = now
        firstFailure.remove(source)
        warned.remove(source)
    }

    @Synchronized
    fun recordFailure(source: SourceId, now: Instant) {
        firstFailure.putIfAbsent(source, now)
    }

    @Synchronized
    fun lastSuccess(source: SourceId): Instant? = lastSuccess[source]

    /**
     * Kaynak eşik süresinden uzun süredir veri vermiyorsa ve bu kesinti için daha önce uyarılmadıysa
     * `true` döner (ve uyarıldı olarak işaretler). Kaynak toparlanınca yeniden uyarılabilir.
     */
    @Synchronized
    fun shouldWarn(source: SourceId, now: Instant): Boolean {
        if (source in warned) return false
        val since = lastSuccess[source] ?: firstFailure[source] ?: return false
        if (firstFailure[source] == null) return false // hiç hata yok
        if (Duration.between(since, now) < outageThreshold) return false
        warned += source
        return true
    }
}
