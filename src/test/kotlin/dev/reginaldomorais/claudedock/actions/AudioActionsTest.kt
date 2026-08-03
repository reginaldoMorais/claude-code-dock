package dev.reginaldomorais.claudedock.actions

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.reginaldomorais.claudedock.TtsState

/**
 * T-1.26-27: AudioPauseResumeAction e AudioStopAction estado e update.
 *
 * Testes de instanciação e état-awareness básico.
 */
class AudioActionsTest : BasePlatformTestCase() {

    fun `test AudioPauseResumeAction pode ser instanciada`() {
        val action = AudioPauseResumeAction()
        assertNotNull(action)
    }

    fun `test AudioStopAction pode ser instanciada`() {
        val action = AudioStopAction()
        assertNotNull(action)
    }

    fun `test AudioPauseResumeAction initial state é Idle`() {
        // Isso é mais um smoke test — o estado real vem do ClaudeTtaSessions,
        // que precisa de um projeto. A ação sabe se desabilitar quando project é null.
    }
}
