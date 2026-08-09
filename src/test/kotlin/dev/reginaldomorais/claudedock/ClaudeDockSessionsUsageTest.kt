package dev.reginaldomorais.claudedock

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * T-1.65 e T-1.66 — a ação "Uso" do cabeçalho (RF-51).
 *
 * O que dá para medir sem IDE real é o contrato: o comando exato e o caminho sem sessão. Que a
 * tela do CLI de fato apareça na pane em foco é T-3.65/T-3.66 — aqui nenhuma sessão chega a ter
 * PTY, pelo mesmo motivo já registrado em `ClaudeDockIntegrationTest`.
 */
class ClaudeDockSessionsUsageTest : BasePlatformTestCase() {

    /**
     * T-1.66 — o comando é `/usage` seguido de **CR**.
     *
     * O `\r` não é detalhe de estilo: é o que o TUI lê como Enter, e é a convenção que o
     * `/export` já usa desde D-16. Trocar por `\n` deixaria o comando digitado e não enviado —
     * falha silenciosa, do tipo que este projeto já pagou caro (DEF-07).
     */
    fun `test T-1_66 o comando enviado e usage com CR`() {
        assertEquals("/usage\r", ClaudeDockSessions.USAGE_COMMAND)
    }

    /**
     * T-1.65 — sem sessão selecionada, avisa em vez de não fazer nada.
     *
     * No headless o `ToolWindowManager` devolve `null` para qualquer id, então `selectedWidget()`
     * é `null` — que é exatamente o estado que se quer medir: a janela aberta e vazia (RF-18).
     */
    fun `test T-1_65 sem sessao a acao avisa e nao escreve em PTY`() {
        val recebidas = mutableListOf<Notification>()
        project.messageBus.connect(testRootDisposable)
            .subscribe(
                Notifications.TOPIC,
                object : Notifications {
                    override fun notify(notification: Notification) {
                        if (notification.groupId == ClaudeDockSessions.NOTIFICATION_GROUP) {
                            recebidas += notification
                        }
                    }
                },
            )

        ClaudeDockSessions.getInstance(project).openUsage()

        assertEquals("Um aviso, e um só", 1, recebidas.size)
        assertTrue(
            "A mensagem deve dizer que falta sessão: ${recebidas.first().content}",
            recebidas.first().content.contains("Nenhuma sessão"),
        )
    }
}
