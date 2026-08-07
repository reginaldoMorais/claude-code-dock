package dev.reginaldomorais.claudedock.actions

import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings
import org.junit.Assert.assertEquals
import org.junit.Test

/** T-1.53: o submenu "Velocidade" é montado sobre a tabela do settings (RF-47). */
class SpeechSpeedActionsTest {

    @Test
    fun `um item por velocidade oferecida`() {
        val group = SpeechSpeedMenuAction()

        assertEquals(
            ClaudeDockSettings.SPEECH_SPEEDS.size,
            group.getChildren(null).size,
        )
    }

    /** O rótulo do item é o mesmo que a tela de configuração mostra — nada de lista paralela. */
    @Test
    fun `rotulo do item vem da tabela compartilhada`() {
        assertEquals(
            ClaudeDockSettings.SPEECH_SPEEDS.getValue(175),
            SpeechSpeedAction(175).templatePresentation.text,
        )
    }
}
