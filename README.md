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
- Google Chirp 3 HD premium opzionale per voce audiolibro, con fallback automatico a WaveNet e poi Android TTS.
- Preset Audiolibro PRO, Naturale, Studio, Lento e chiaro, Veloce e Focus.
- Controlli persistenti per velocita, pitch, pause, font reader e interlinea.
- Gestione API key con salvataggio locale, mascheramento, test, cancellazione e cambio manuale con conferma.
- Lista voci Google aggiornata via endpoint `voices`, filtrata sulle voci italiane Chirp 3 HD e WaveNet.
- Contatori mensili separati per Chirp 3 HD, WaveNet e totale Google Cloud.
- Blocco sicurezza Chirp a 900.000 caratteri/mese e WaveNet a 3.800.000 caratteri/mese.
- Cache audio locale interna all'app per riusare segmenti gia generati con Chirp 3 HD o WaveNet.
- Deduplicazione tramite chiave SHA-256 basata su provider, voce, lingua, testo normalizzato e impostazioni audio.
- Statistiche cache con spazio usato, file salvati, cache hit e caratteri stimati risparmiati.
- Navigazione principale a 5 sezioni: Libreria, Reader, Voce, Crediti e Impostazioni.
- Reader pulito con focus mode, provider attivo, preset e indicatore cache.
- Schermata Crediti dedicata per API key, quote Google e reset debug.
- Modalita risparmio crediti e modalita solo cache per evitare nuove chiamate Google.
- Eliminazione documenti dalla Libreria con conferma.

## Google Cloud TTS

Le voci premium usano gli endpoint REST:

```text
https://texttospeech.googleapis.com/v1/text:synthesize
https://texttospeech.googleapis.com/v1/voices
```

L'app non contiene API key reali. L'utente inserisce la propria chiave dentro l'app, salvata solo localmente sul dispositivo e mostrata mascherata dopo il salvataggio. Ogni richiesta invia solo la frase o il piccolo paragrafo corrente, mai l'intero EPUB.

La rotazione automatica di piu chiavi non e implementata: l'app usa una sola API key attiva alla volta. Il contatore mensile locale non viene azzerato quando cambi manualmente chiave.

Chirp 3 HD e piu naturale ma potenzialmente piu costoso oltre la quota gratuita: l'app lo blocca prudenzialmente a 900.000 caratteri mensili e prova WaveNet. Se WaveNet non e disponibile o raggiunge il limite sicurezza, la lettura continua con Android TextToSpeech locale.

## Cache audio

La cache salva MP3 generati da Google nella cache interna dell'app (`cacheDir`), con nomi file basati su hash e senza testo completo o API key nei nomi. Se lo stesso segmento viene letto di nuovo con lo stesso provider, voce e impostazioni audio, l'app riproduce l'audio locale e non consuma nuovi caratteri Google.

La sezione `Cache audio` permette di attivare/disattivare la cache, vedere spazio usato e hit, scegliere un limite massimo tra 100 MB, 250 MB, 500 MB e 1 GB, ricontrollare lo spazio e svuotare solo i file audio cache. Lo svuotamento non cancella documenti, API key, impostazioni o contatori mensili Google.

## Generare APK con GitHub Actions

1. Carica il progetto nella root del repository GitHub.
2. Vai nella sezione `Actions`.
3. Avvia il workflow di build APK.
4. Scarica l'artifact generato.

Il path APK previsto e:

```text
app/build/outputs/apk/debug/app-debug.apk
```
