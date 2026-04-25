# Tarlo Speak

Tarlo Speak e un'app Android in Kotlin e Jetpack Compose che importa EPUB e li legge come audiolibro con un reader a pagine visuali.

Package/applicationId: `com.tarlo.speak`.

## Funzioni

- Import EPUB con titolo da metadata o nome file.
- Reader a pagine con avanzamento `Pagina X di N`.
- Evidenziazione parola/frase/paragrafo sulla pagina gia visibile.
- Lettura naturale a frasi e piccoli paragrafi.
- Android TextToSpeech locale sempre disponibile come fallback.
- Google Cloud Text-to-Speech WaveNet opzionale, con una sola API key attiva inserita dall'utente.
- Preset Audiolibro PRO, Naturale, Studio, Lento e chiaro, Veloce e Focus.
- Controlli persistenti per velocita, pitch, pause, font reader e interlinea.
- Gestione API key con salvataggio locale, mascheramento, test, cancellazione e cambio manuale con conferma.
- Contatore mensile Google Cloud con avvisi a 850.000/900.000 caratteri e blocco sicurezza a 950.000.

## Google Cloud TTS

La voce premium usa l'endpoint REST:

```text
https://texttospeech.googleapis.com/v1/text:synthesize
```

L'app non contiene API key reali. L'utente inserisce la propria chiave dentro l'app, salvata solo localmente sul dispositivo e mostrata mascherata dopo il salvataggio. Ogni richiesta invia solo la frase o il piccolo paragrafo corrente, mai l'intero EPUB.

La rotazione automatica di piu chiavi non e implementata: l'app usa una sola API key attiva alla volta. Il contatore mensile locale non viene azzerato quando cambi manualmente chiave.

## Generare APK con GitHub Actions

1. Carica il progetto nella root del repository GitHub.
2. Vai nella sezione `Actions`.
3. Avvia il workflow di build APK.
4. Scarica l'artifact generato.

Il path APK previsto e:

```text
app/build/outputs/apk/debug/app-debug.apk
```
