import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    // 2.4.x é necessário para ler a metadata Kotlin 2.4.0 embutida na plataforma 2026.2.
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))

        // A tool window e a criação de sessões dependem do plugin de terminal.
        bundledPlugin("org.jetbrains.plugins.terminal")

        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    // O `buildSearchableOptions` sobe uma segunda instância do IDE, em headless, só para indexar
    // os campos da nossa tela de configuração na busca do Settings. Ele falha com "Only one
    // instance of IDEA can be run at a time" sempre que o IDE do autor está aberto — que é o caso
    // normal de quem está desenvolvendo o plugin.
    //
    // Custo de desligar: os campos da tela deixam de aparecer ao digitar no campo de busca do
    // Settings. A tela em si continua em Tools > Claude Code Dock, navegável como sempre. Este
    // plugin é de instalação local, sem publicação na Marketplace, então o índice não paga o
    // atrito de build.
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            // untilBuild deliberadamente vazio (D-03 / R-08).
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    test {
        useJUnit()
        // Sem isto o log do CI mostra só a linha da exceção, sem os valores comparados.
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}
