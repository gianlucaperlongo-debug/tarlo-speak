# Tarlo Speaker V2

Tarlo Speaker e un'app Android in Kotlin e Jetpack Compose che carica file EPUB, estrae il testo dai file HTML/XHTML interni e li legge come audiolibro con Android Text-to-Speech in italiano.

## Funzioni

- Caricamento EPUB con selettore documenti Android.
- Lettura vocale a blocchi/frasi per evitare testi troppo lunghi.
- Play, pausa, avanti e indietro di 3 blocchi.
- Libreria locale dei libri caricati nella sessione.
- Player fisso in basso in stile Spotify.
- Controlli velocita e pitch della voce.
- Timer notte da 10, 20 o 30 minuti.
- Export WAV dei primi 50 blocchi in file separati.

## Caricare il progetto su GitHub

1. Crea un nuovo repository su GitHub.
2. Carica nella root del repository tutti i file contenuti in `tarlo-speaker-v2`.
3. Verifica che `.github/workflows/build-apk.yml` sia presente.

## Generare APK con GitHub Actions

1. Apri il repository su GitHub.
2. Vai nella sezione `Actions`.
3. Seleziona `Build Tarlo Speaker APK`.
4. Premi `Run workflow`.
5. Al termine della build apri il job completato.
6. Scarica l'artifact `tarlo-speaker-debug-apk`.

Dentro l'artifact troverai:

```text
app-debug.apk
```

Il path generato dalla build e:

```text
app/build/outputs/apk/debug/app-debug.apk
```
