package dev.reginaldomorais.claudedock

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.reginaldomorais.claudedock.settings.ClaudeDockSettings
import dev.reginaldomorais.claudedock.settings.TtsEngine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * T-1.62 e T-1.63 — o caminho negativo de [ClaudeTtaSessions.playText] deixou de ser mudo (RF-50,
 * RNF-33, D-42).
 *
 * **Por que o teste vive aqui e não na ação.** O aviso foi posto no serviço justamente porque os
 * dois chamadores — o item "Tocar seleção" do menu e o botão do popup — passam por ele. Testar no
 * chamador mediria a fiação; testar aqui mede a guarda.
 *
 * A síntese roda em thread de pool, então a verificação é por `CountDownLatch`: sem espera, o
 * teste terminaria antes de o `canSynthesize` sequer ter ido ao disco.
 */
class ClaudeTtaSessionsTest : BasePlatformTestCase() {

    /**
     * T-1.62 — Piper não configurado **notifica**, e o estado volta a `Idle`.
     *
     * Antes da v1.10 este caminho só chamava `LOG.warn`. Do cabeçalho isso passava despercebido,
     * porque o `update()` do menu desabilita o item; o popup não tem `update()`, e o clique seria
     * o no-op mudo do DEF-07 outra vez.
     */
    fun `test T-1_62 motor ausente notifica e volta a Idle`() {
        // Modelo inexistente força `canSynthesize` a `false` sem depender do que há na máquina —
        // se o Piper estiver instalado de verdade aqui, o teste continua medindo o mesmo ramo.
        withEngine(TtsEngine.PIPER, "/tmp/modelo-que-nao-existe.onnx") {
            val received = captureNotifications()

            ClaudeTtaSessions.getInstance(project).playText("qualquer trecho")

            val notification = received.await()
            assertNotNull("O caminho negativo precisa avisar (RNF-33)", notification)
            assertTrue(
                "A mensagem deve dizer o que fazer, e não apenas que falhou: ${notification!!.content}",
                notification.content.contains("Piper"),
            )
            assertEquals(TtsState.Idle, ClaudeTtaSessions.getInstance(project).getState())
        }
    }

    /**
     * T-1.63 — texto em branco continua saindo cedo, **sem** notificar.
     *
     * O popup nunca chega a chamar com branco (o `normalize` já barrou), e o menu tampouco. Um
     * aviso aqui seria ruído sobre um caminho que ninguém percorre — RNF-33 pede aviso para falha,
     * não para nada-a-fazer.
     */
    fun `test T-1_63 texto em branco sai cedo e nao notifica`() {
        val received = captureNotifications()

        ClaudeTtaSessions.getInstance(project).playText("   ")

        assertNull("Branco não é falha; não deve virar balão", received.awaitBriefly())
        assertEquals(TtsState.Idle, ClaudeTtaSessions.getInstance(project).getState())
    }

    // -- apoio ------------------------------------------------------------------------------

    private fun withEngine(engine: TtsEngine, model: String, body: () -> Unit) {
        val settings = ClaudeDockSettings.getInstance()
        val previousEngine = settings.ttsEngine
        val previousModel = settings.piperModel
        settings.ttsEngine = engine
        settings.piperModel = model
        try {
            body()
        } finally {
            settings.ttsEngine = previousEngine
            settings.piperModel = previousModel
        }
    }

    private fun captureNotifications(): Captured {
        val captured = Captured()
        project.messageBus.connect(testRootDisposable)
            .subscribe(
                Notifications.TOPIC,
                object : Notifications {
                    override fun notify(notification: Notification) = captured.record(notification)
                },
            )
        return captured
    }

    /** Coletor com espera: a síntese é assíncrona, e sem latch o teste mediria o vazio. */
    private class Captured {
        private val latch = CountDownLatch(1)

        @Volatile
        private var notification: Notification? = null

        fun record(n: Notification) {
            if (n.groupId != ClaudeDockSessions.NOTIFICATION_GROUP) return
            notification = n
            latch.countDown()
        }

        fun await(): Notification? {
            latch.await(WAIT_SECONDS, TimeUnit.SECONDS)
            return notification
        }

        /** Para o caso negativo: espera curta, porque o que se quer provar é a ausência. */
        fun awaitBriefly(): Notification? {
            latch.await(1, TimeUnit.SECONDS)
            return notification
        }

        private companion object {
            const val WAIT_SECONDS = 10L
        }
    }
}
